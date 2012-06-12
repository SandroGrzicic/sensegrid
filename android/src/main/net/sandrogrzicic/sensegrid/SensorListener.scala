package net.sandrogrzicic.sensegrid

import android.content.Context
import android.hardware.{SensorManager, Sensor, SensorEvent, SensorEventListener}
import android.provider.Settings
import android.widget.{Toast, TextView}
import collection.mutable.ArrayBuffer
import proto.{Nodes, Node, Data}

/**
 * Receives sensor updates and sends them to the server periodically.
 */
class SensorListener(val activity: SenseGrid) extends SensorEventListener {

	/** Buffer holding last update times of all sensors. */
	val lastUpdates = ArrayBuffer.fill[Long](SensorListener.SENSOR_COUNT)(0L)

	/** Buffer holding the most recently updated sensor data. */
	val sensors = ArrayBuffer.fill[Data](SensorListener.SENSOR_COUNT)(null)

	/** When was our last message sent to the server. */
	var lastMessageSent: Long = _
	/** When was the last message received from the server. */
	var lastMessageReceived: Long = _

	/** Current sensor data update interval. */
	var updateInterval = SensorListener.DATA_UPDATE_INTERVAL_DEFAULT
	/** Current server synchronization interval. */
	var syncInterval = SensorListener.SYNC_INTERVAL_DEFAULT
	/** Minimum allowed server synchronization interval. */
	var syncIntervalMin = SensorListener.SYNC_INTERVAL_MIN

	var manager: SensorManager = _

	var magneticField: Sensor = _
	var pressure: Sensor = _
	var temperature: Sensor = _
	var relativeHumidity: Sensor = _

	var deviceID: String = _
	var serverURL: String = _

	/** Initialization: discover useful device sensors. */
	def create() {
		deviceID = Settings.Secure.getString(activity.getContentResolver, Settings.Secure.ANDROID_ID)
		serverURL = activity.getString(R.string.server_url)

		manager = activity.getSystemService(Context.SENSOR_SERVICE).asInstanceOf[SensorManager]

		magneticField = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
		pressure = manager.getDefaultSensor(Sensor.TYPE_PRESSURE)
		temperature = manager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
		relativeHumidity = manager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY)

		resume()
	}

	def resume() {
		manager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL)
		manager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_NORMAL)
		manager.registerListener(this, temperature, SensorManager.SENSOR_DELAY_NORMAL)
		manager.registerListener(this, relativeHumidity, SensorManager.SENSOR_DELAY_NORMAL)
	}

	def pause() {
		manager.unregisterListener(this)
	}

	def onSensorChanged(e: SensorEvent) {
		val sensorType = e.sensor.getType
		if (hasSufficientTimePassed(sensorType)) {
			updateCurrentTime(sensorType)

			val sensorData = Data(
				Data.Type.valueOf(sensorType),
				e.accuracy,
				Vector(e.values: _*)
			)
			sensors(sensorType) = sensorData

			val outgoingMessage = constructMessage()

			// update map representation of our node
			activity.updateNode(deviceID, outgoingMessage)

			if (timeToSendDataToServer) {
				if (syncDataWithServer(Some(outgoingMessage))) {
					updateLastMessageSentTime()
				}
			}
		}
	}

	def onAccuracyChanged(sensor: Sensor, accuracy: Int) { /* not useful */ }

	def hasSufficientTimePassed(sensorType: Int) = {
		System.currentTimeMillis - lastUpdates(sensorType) > updateInterval
	}

	def timeToSendDataToServer = {
		System.currentTimeMillis - lastMessageSent > syncInterval
	}

	def updateCurrentTime(sensorType: Int) {
		lastUpdates(sensorType) = System.currentTimeMillis
	}

	def updateLastMessageSentTime() {
		lastMessageSent = System.currentTimeMillis
	}

	def allowSyncWithServer() = {
		if (
			activity.lat == 0 ||
			activity.long == 0 ||
			System.currentTimeMillis() - lastMessageSent < syncIntervalMin
		) {
			false
		} else {
			true
		}
	}

	/** Returns a message with current node and sensor data. */
	def constructMessage() = Node(
		deviceID,
		activity.lat,
		activity.long,
		activity.accuracy,
		System.currentTimeMillis,
		Vector(sensors.filter(_ != null): _*)
	)

	/**
	 * Server synchronization.
	 * Optionally sends the currently stored messages to the server, assuming current position is known.
	 * Optionally receives fresh data from server and updates the nodes on the grid.
	 * Optionally shows notifications about the current synchronization status.
	 * @return false if message has surely not been sent; true otherwise.
	 */
	def syncDataWithServer(outgoingMessage: Option[Node], receive: Boolean = true, showNotification: Boolean = false): Boolean = {
		if (!allowSyncWithServer) {
			return false
		}

		var updateNotification: Option[Toast] = None
		if (showNotification) {
			val toast = Toast.makeText(activity, R.string.synchronization_started, Toast.LENGTH_SHORT)
			toast.show()
			updateNotification = Some(toast)
		}

		var incomingMessageFunc: Option[Nodes => Unit] = None
		if (receive) {
			incomingMessageFunc = Some(nodes => {
				if (nodes.timestamp > lastMessageReceived) {
					// update nodes on map
					activity.updateGrid(nodes.nodes)
					lastMessageReceived = nodes.timestamp
					// update node count
					val nodeCount = activity.findViewById(R.id.nodeCount).asInstanceOf[TextView]
					nodeCount.setText(nodes.nodes.size.toString)

					updateNotification.foreach { toast =>
						toast.cancel()
						Toast.makeText(activity, R.string.synchronization_finished, Toast.LENGTH_SHORT).show()
					}
				}
			})
		}

		val asyncTask = new ServerTask(activity, serverURL, incomingMessageFunc)

		outgoingMessage match {
			case Some(message) => asyncTask.execute(message)
			case None          => asyncTask.execute()
		}

		true
	}

}

object SensorListener {
	/** Sensor data default update interval. */
	val DATA_UPDATE_INTERVAL_DEFAULT: Long = 1000 // ms

	val SYNC_INTERVAL_MIN: Long = 1000 // ms
	val SYNC_INTERVAL_DEFAULT: Long = 10000 // ms

	val SENSOR_COUNT = 14
}
