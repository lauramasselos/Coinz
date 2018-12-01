package com.example.s1603459.myapplication

//import android.content.Context
//import android.content.Intent
//import android.location.Location
import android.os.Bundle
//import android.os.PersistableBundle
//import android.provider.ContactsContract
//import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
//import android.util.Log
//import com.mapbox.android.core.location.LocationEngine
//import com.mapbox.android.core.location.LocationEngineListener
//import com.mapbox.android.core.location.LocationEnginePriority
//import com.mapbox.android.core.location.LocationEngineProvider
//import com.mapbox.android.core.permissions.PermissionsListener
//import com.mapbox.android.core.permissions.PermissionsManager
//import android.view.Menu
//import android.view.MenuItem
//import android.widget.*
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
import com.mapbox.mapboxsdk.Mapbox
//import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.geometry.LatLng
//import com.mapbox.mapboxsdk.location.modes.CameraMode
//import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
//import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
//import com.mapbox.geojson.*
//import com.mapbox.mapboxsdk.annotations.Marker
//import com.mapbox.mapboxsdk.annotations.MarkerOptions
//import com.mapbox.mapboxsdk.camera.CameraUpdate
//import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
//import com.mapbox.mapboxsdk.style.light.Position
//import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
//import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity()  { // DownloadCompleteListener, OnMapReadyCallback, PermissionsListener, LocationEngineListener

//    private val tag = "MainActivity"
//    private var downloadDate = "" // Format YYYY/MM/DD
//    private val preferencesFile = "MyPrefsFile"


//    private var mapView: MapView? = null
//    private var map: MapboxMap? = null
    private lateinit var mapView: MapView
//    private lateinit var map: MapboxMap
//    private lateinit var originLocation : Location
//    private lateinit var permissionsManager : PermissionsManager
//    private lateinit var locationEngine : LocationEngine
//    private lateinit var locationLayerPlugin : LocationLayerPlugin




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        setContentView(R.layout.activity_main)

        //setSupportActionBar(toolbar)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // DUMMY MARKER
//        map.addMarker(MarkerOptions().position(LatLng(48.85819, 2.29458)).title("Eiffel Tower"))

//        mapView?.getMapAsync(this)
//        mapView?.getMapAsync{mapBoxMap ->
//            map = mapBoxMap
//            enableLocation()
//        }

    }

//    override fun onMapReady(mapboxMap: MapboxMap?) {
//        if (mapboxMap == null) {
//            Log.d(tag, "[onMapReady] mapboxMap is null")
//        } else {
//            map = mapboxMap
//            map?.uiSettings?.isCompassEnabled = true
//            map?.uiSettings?.isZoomControlsEnabled = true
//            enableLocation()
//        }


//        GeoJsonSource source = new GeoJsonSource("geojson", geoJsonString)
//        mapboxMap.addSource(source)
//        mapboxMap.addLayer(new LineLayer("geojson", "geojson"))
//        FeatureCollection featureCollection = FeatureCollection.fromJson(geoJsonString)
//
//        List<Feature> features = featureCollection.getFeatures();
//
//    for (Feature f : features) {
//        if (f.getGeometry() instanceof Point) {
//        Position coordinates = f.getGeometry().getCoordinates()
//        map.addMarker(new MarkerViewOptions().position(new LatLng(coordinates.getLatitude(), coordinates.getLongitude())))
//    }
//}

//    }


//    override fun downloadComplete(result: String) {
//       toodo("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

//    private fun enableLocation() {
//        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//            Log.d(tag, "Permissions are granted")
//            initialiseLocationEngine()
//            initialiseLocationLayer()
//        } else {
//            Log.d(tag, "Permissions are not granted")
//            permissionsManager = PermissionsManager(this)
//            permissionsManager.requestLocationPermissions(this)
//        }
//    }
//
//    @SuppressWarnings("MissingPermission")
//    private fun initialiseLocationEngine() {
//        locationEngine = LocationEngineProvider(this)
//                .obtainBestLocationEngineAvailable()
//        locationEngine.apply {
//            interval = 5000
//            fastestInterval = 1000
//            priority = LocationEnginePriority.HIGH_ACCURACY
//                    activate()
//        }
//        val lastLocation = locationEngine.lastLocation
//        if (lastLocation != null) {
//            originLocation = lastLocation
//            setCameraPosition(lastLocation)
//        } else { locationEngine.addLocationEngineListener(this) }
//    }
//
//    @SuppressWarnings("MissingPermission")
//    private fun initialiseLocationLayer() {
//        if (mapView == null) { Log.d(tag, "mapView is null")
//        } else {
//            if (map == null) { Log.d(tag, "map is null")
//            } else {
//                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
//                locationLayerPlugin.apply {
//                    setLocationLayerEnabled(true)
//                    cameraMode = CameraMode.TRACKING
//                    renderMode = RenderMode.NORMAL
//                }
//            }
//        }
//    }
//
//    private fun setCameraPosition(location: Location) {
//        val latlng = LatLng(location.latitude, location.longitude)
//        map?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
//    }
//
//    override fun onLocationChanged(location: Location?) {
//        location?.let {
//            originLocation = location
//            setCameraPosition(location)
//        }
//    }
//
//    @SuppressWarnings("MissingPermission")
//    override fun onConnected() {
//        Log.d(tag, "[onConnected] requesting location updates")
//        locationEngine.requestLocationUpdates()
//    }
//
//
//    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) { // override
//        Log.d(tag, "Permissions: $permissionsToExplain")
//        // Present popup message or dialog
//        Toast.makeText(this, "Permission needed to access location for gameplay.", Toast.LENGTH_LONG).show()
//    }
//
//    override fun onPermissionResult(granted: Boolean) { // override
//        Log.d(tag, "[onPermissionResult] granted == $granted")
//        if (granted) {
//            enableLocation()
//        } else {
//        // Open a dialogue with the user
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    override fun onStart() {
        super.onStart()

//        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
//        downloadDate = settings.getString("lastDownloadDate", "")
//        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")
//
//        if (PermissionsManager.areLocationPermissionsGranted(this)) {
//            locationEngine.requestLocationUpdates()
//            locationLayerPlugin.onStart()
//        }
        mapView.onStart()

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
//        locationEngine.removeLocationUpdates()
//        locationLayerPlugin.onStop()
        mapView.onStop()

//        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
//        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
//        val editor = settings.edit()
//        editor.putString("lastDownloadDate", downloadDate)
//        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
//        locationEngine.deactivate()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

//    private fun switchToMap() {
//        val intent = Intent(this, MapsActivity::class.java)
//        startActivity(intent)
//    }

}

