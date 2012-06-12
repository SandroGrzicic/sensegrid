package net.sandrogrzicic.sensegrid

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import java.net.{HttpURLConnection, URL}
import proto.{Nodes, Node}

/**
 * Handles server communication asynchronously.
 * @param incomingMessageFunc optional function called after task execution finishes. Runs on the UI thread.
 */

class ServerTask(val context: Context, val serverURL: String, incomingMessageFunc: Option[Nodes => Unit])
  extends AsyncTask[Object, Object, Either[Exception, Nodes]] {

	protected def doInBackground(messageArray: Object*) = {
		var outgoingMessage: Option[Node] = None

		if (messageArray.length > 0) {
			outgoingMessage = Some(messageArray.asInstanceOf[Seq[Node]](0))
		}

		val url = new URL(serverURL + ServerTask.SUBMIT_ROUTE)

		val connection = url.openConnection().asInstanceOf[HttpURLConnection]
		connection.setDoInput(true)

		if (outgoingMessage.isDefined) {
			connection.setDoOutput(true)
		}

		connection.setRequestMethod("POST")

		try {
			connection.setConnectTimeout(ServerTask.CONNECT_TIMEOUT)

			outgoingMessage.foreach { message =>
				connection.setFixedLengthStreamingMode(message.getSerializedSize)
				message.writeTo(connection.getOutputStream)
			}

			val incomingMessage = Nodes.defaultInstance.mergeFrom(connection.getInputStream)

			Log.i(SenseGrid.SENSEGRID,
				"NETWORK - sent: " + (if (outgoingMessage.isDefined) outgoingMessage.get.getSerializedSize else 0) +
				  " - recv: " + incomingMessage.getSerializedSize)

			Right(incomingMessage)
		} catch {
			case e: Exception => Left(e)
		} finally {
			connection.disconnect()
		}
	}

	override def onPostExecute(result: Either[Exception, Nodes]) {
		result match {
			case Right(nodes)    => incomingMessageFunc.foreach(_(nodes))
			case Left(exception) =>
				Log.e(SenseGrid.SENSEGRID, "Error while connecting to server", exception)
				val toast = Toast.makeText(context, R.string.server_error, Toast.LENGTH_LONG)
				toast.setGravity(Gravity.BOTTOM, 0, 0)
				toast.show()
		}
	}
}

object ServerTask {
	val CONNECT_TIMEOUT = 4000

	val SUBMIT_ROUTE = "s"
}
