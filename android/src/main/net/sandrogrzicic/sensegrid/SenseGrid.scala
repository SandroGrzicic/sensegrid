package net.sandrogrzicic.sensegrid

import _root_.android.os.Bundle
import android.content.{Intent, Context}
import android.graphics.drawable.Drawable
import android.location.{LocationListener, Location, LocationManager}
import android.provider.Settings
import android.view.{MotionEvent, View}
import android.widget.Button
import com.google.android.maps.{GeoPoint, MapActivity}
import com.readystatesoftware.maps.{OnSingleTapListener, TapControlledMapView}
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay
import proto.Node

class SenseGrid extends MapActivity {

	var locationManager: LocationManager = _
	var locationProvider: String = _
	var locationUpdateHandler: LocationUpdateHandler = _

	var lat: Int = _
	var long: Int = _
	var accuracy: Float = _

	protected var map: TapControlledMapView = _
	var locations: DeviceLocations = _

	val listener: SensorListener = new SensorListener(this)

	var markerNodeActive: Drawable = _
	var nodeMarker: Drawable = _

	/** Setup to be done on first launch. */
	def firstTimeSetup() {
		val prefs = getPreferences(Context.MODE_PRIVATE)
		if (!prefs.getBoolean(SenseGrid.SETTINGS_SETUP, false)) {

			centerOnLocation(getLastKnownLocation)

		} else {
			prefs.edit().putBoolean(SenseGrid.SETTINGS_SETUP, true).apply()
		}
	}

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.grid)

		map = findViewById(R.id.gridView).asInstanceOf[TapControlledMapView]
		map.setBuiltInZoomControls(true)
		map.setSatellite(false)
		map.setTraffic(true)

		map.setOnSingleTapListener(new OnSingleTapListener() {
			override def onSingleTap(event: MotionEvent) = {
				locations.hideAllBalloons()
				true
			}
		})


		nodeMarker = BalloonItemizedOverlay.boundCenter(getResources.getDrawable(R.drawable.marker))
		locations = new DeviceLocations(nodeMarker, map)
		map.getOverlays.add(locations)

		initGeoLocation()

		firstTimeSetup()

		listener.create()

	}

	override def onResume() {
		super.onResume()
		locationManager.requestLocationUpdates(locationProvider, 0, 0, locationUpdateHandler)
		listener.resume()
	}

	override def onPause() {
		super.onPause()
		locationManager.removeUpdates(locationUpdateHandler)
		listener.pause()
	}


	/** Updates the MapView with the specified nodes. */
	def updateGrid(nodes: Vector[Node]) {
		val lNodes = locations.nodes
		nodes.foreach { node =>
			val item = OverlayNode(node, getResources)
			lNodes(node.id) = item
		}
		locations.updated()

		map.invalidate()
	}

	/** Update a single Node. */
	def updateNode(nodeID: String, message: Node) {
		locations.nodes(nodeID) = OverlayNode(message, getResources)
		locations.updated()

		map.invalidate()
	}


	protected def initGeoLocation() {
		locationManager = getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]

		locationProvider = SenseGrid.DEFAULT_LOCATION_PROVIDER

		locationUpdateHandler = new LocationUpdateHandler()
	}

	def getLastKnownLocation: Option[GeoPoint] = {
		var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
		if (location == null) {
			location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
			if (location == null) {
				return None
			}
		}
		Some(new GeoPoint((location.getLatitude * 1E6).toInt, (location.getLongitude * 1E6).toInt))
	}

	/** Finds and sets the best available location provider. Updates the provider UI icon. */
	def updateBestLocationProvider() {
		val providerButton = findViewById(R.id.mapProviderSwitch).asInstanceOf[Button]

		locationProvider =
		  if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				providerButton.setBackgroundResource(android.R.drawable.ic_menu_directions)
				LocationManager.GPS_PROVIDER
			} else {
				providerButton.setBackgroundResource(android.R.drawable.ic_menu_mylocation)
				LocationManager.NETWORK_PROVIDER
			}
	}


	def centerOnLocation(pointOption: Option[GeoPoint]) {
		pointOption foreach { point =>
			map.getController animateTo(point)
		}
	}

	// methods from the layout xml (buttons)

	def toggleMapType(view: View) {
		map.isSatellite match {
			case false => map.setSatellite(true)
			case true  => map.setSatellite(false)
		}
	}

	def toggleLocationProvider(view: View) {
		locationProvider match {
			case LocationManager.NETWORK_PROVIDER =>
				if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					locationProvider = LocationManager.GPS_PROVIDER
					view.setBackgroundResource(android.R.drawable.ic_menu_directions)
				} else {
					val intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
					startActivity(intent)
				}
			case LocationManager.GPS_PROVIDER =>
				locationProvider = LocationManager.NETWORK_PROVIDER
				view.setBackgroundResource(android.R.drawable.ic_menu_mylocation)
		}

		locationManager.removeUpdates(locationUpdateHandler)
		locationManager.requestLocationUpdates(locationProvider, 0, 0, locationUpdateHandler)
	}

	def centerOnCurrentLocation(view: View) {
		centerOnLocation(getLastKnownLocation)
	}

	def syncData(view: View) {
		listener.syncDataWithServer(Some(listener.constructMessage()), showNotification = true)
	}

	def isRouteDisplayed = false

	/** Handles location updates. */
	class LocationUpdateHandler extends LocationListener {

		@Override
		def onLocationChanged(l: Location) {
			lat = (l.getLatitude * 1E6).toInt
			long = (l.getLongitude * 1E6).toInt
			accuracy = l.getAccuracy
		}

		def onProviderDisabled(provider: String) {
			updateBestLocationProvider()
		}

		def onProviderEnabled(provider: String) {
			updateBestLocationProvider()
		}

		def onStatusChanged(provider: String, status: Int, extras: Bundle) {}
	}

}

object SenseGrid {
	val SENSEGRID = "SenseGrid"

	val DEFAULT_LOCATION_PROVIDER = LocationManager.NETWORK_PROVIDER

	val SETTINGS_SETUP = "S"

	/** How long should node data be old to mark a node as stale */
	val NODE_ACTIVITY_THRESHOLD = 60 * 1000 // ms
}
