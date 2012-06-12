package net.sandrogrzicic.sensegrid

import android.content.res.Resources
import com.google.android.maps.{GeoPoint, OverlayItem}
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay
import java.text.SimpleDateFormat
import java.util.Date
import proto.{Data, Node}

/**
 * A graphical representation of a Node.
 */
class OverlayNode(val node: Node, snippet: String)
  extends OverlayItem(new GeoPoint(node.geoLat, node.geoLong), node.id.take(8), snippet)

object OverlayNode {
	lazy val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm")

	def apply(node: Node, resources: Resources) = {
		val overlayNode = new OverlayNode(node, generateSnippet(node, resources))

		if (System.currentTimeMillis() - node.timestamp < SenseGrid.NODE_ACTIVITY_THRESHOLD) {
			overlayNode.setMarker(BalloonItemizedOverlay.boundCenter(resources.getDrawable(R.drawable.marker2)))
		}

		overlayNode
	}

	def generateSnippet(node: Node, resources: Resources) = {
		val sb = StringBuilder.newBuilder
		sb append(resources.getString(R.string.last_update)) append(": ")
		sb append(OverlayNode.dateFormat.format(new Date(node.timestamp))) append("\n")

		node.sensors.foreach { s =>
			import Data.Type._
			s.sensorType match {
				case MAGNETIC_FIELD =>
					sb append(resources.getString(R.string.magnetic_field)) append(": ") append(
					  math.sqrt(s.data map(v => v * v) sum).formatted("%.2f")
					  ) append(" μT")
				case PRESSURE =>
					sb append(resources.getString(R.string.pressure)) append(": ") append(s.data(0)) append(" hPa")
				case RELATIVE_HUMIDITY =>
					sb append(resources.getString(R.string.relative_humidity)) append(": ") append(s.data(0)) append("%")
				case TEMPERATURE =>
					sb append(resources.getString(R.string.temperature)) append(": ") append(s.data(0)) append(" °C")
			}
			sb append("\n")
		}
		sb.stripLineEnd
	}

}
