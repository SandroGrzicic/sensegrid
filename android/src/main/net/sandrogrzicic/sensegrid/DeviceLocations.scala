package net.sandrogrzicic.sensegrid

import android.graphics.drawable.Drawable
import collection.mutable
import com.readystatesoftware.maps.TapControlledMapView
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay

class DeviceLocations(icon: Drawable, mapView: TapControlledMapView)
  extends BalloonItemizedOverlay[OverlayNode](icon, mapView) {

	val nodes = mutable.LinkedHashMap.empty[String, OverlayNode]

	setSnapToCenter(false)
	populate()

	/** Must be called after any mutation to the nodes collection. */
	def updated() {
		populate()
	}

	protected def createItem(i: Int) = {
		nodes.values.toList(i)
	}

	def size = nodes.size

	protected override def onBalloonTap(index: Int, item: OverlayNode): Boolean = {
		false
	}

}
