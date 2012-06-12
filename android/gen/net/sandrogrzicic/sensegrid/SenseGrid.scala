package net.sandrogrzicic.sensegrid

class SenseGrid extends MapActivity {

	var locationManager: LocationManager = _
	var locationProvider: String = _
	var locationUpdateHandler: LocationUpdateHandler = _

	var lat: Int = _
	var long: Int = _

	protected var map: MapView = _

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.grid)

		map = findViewById(R.id.grid).asInstanceOf[MapView]
		map.setBuiltInZoomControls(true)
		map.setSatellite(false)
		map.setTraffic(true)

		fillGrid()

		initGeoLocation()

		centerOnLocation(getLastKnownLocation)

		SensorSenser(this).start()

	}

	override def onResume() {
		super.onResume()
		locationManager.requestLocationUpdates(locationProvider, 0, 0, locationUpdateHandler)
	}

	override def onPause() {
		super.onPause()
		locationManager.removeUpdates(locationUpdateHandler);
	}


	protected def fillGrid() {
		val locationList = map.getOverlays

		val locations = new DeviceLocations(this, getResources.getDrawable(android.R.drawable.star_on))

		locations.init()

		//    while () {
		//      final GeoPoint point = new GeoPoint(c.getInt(1), c.getInt(2))
		//      final OverlayItem item = new OverlayItem(point, c.getString(0), null)
		//      // TO DO: mijenjati marker, ovisno o trenutnom zauzeću menze (crveni/žuti/zeleni)
		//      //item.setMarker();
		//      locations.addOverlay(item);
		//    }


		locationList.clear()
		locationList.add(locations)

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

	def getBestLocationProvider = {
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			LocationManager.GPS_PROVIDER
		} else {
			LocationManager.NETWORK_PROVIDER
		}
	}


	def centerOnLocation(point: Option[GeoPoint]) {
		point match {
			case Some(p) =>
				val controller = map.getController
				controller.animateTo(p)
			case None => // do nothing
		}
	}

	// methods from the layout xml

	def toggleMapType(view: View) {
		map.isSatellite match {
			case false => map.setSatellite(true)
			case true => map.setSatellite(false)
		}
	}

	def toggleLocationProvider(view: View) {
		locationProvider = locationProvider match {
			case LocationManager.GPS_PROVIDER => LocationManager.NETWORK_PROVIDER
			case LocationManager.NETWORK_PROVIDER => LocationManager.GPS_PROVIDER
		}

		locationManager.removeUpdates(locationUpdateHandler);
		locationManager.requestLocationUpdates(locationProvider, 0, 0, locationUpdateHandler)
	}

	def centerOnCurrentLocation(view: View) {
		centerOnLocation(getLastKnownLocation)
	}

	def isRouteDisplayed = false

	/**Handles location updates. */
	class LocationUpdateHandler extends LocationListener {
		@Override
		def onLocationChanged(l: Location) {
			val (lat, long) = ((l.getLatitude * 1E6).toInt, (l.getLongitude * 1E6).toInt)
		}

		def onProviderDisabled(provider: String) {
			locationProvider = getBestLocationProvider
		}

		def onProviderEnabled(provider: String) {
			locationProvider = getBestLocationProvider
		}

		def onStatusChanged(provider: String, status: Int, extras: Bundle) {}
	}

}
