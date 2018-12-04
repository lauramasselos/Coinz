package com.example.s1603459.myapplication

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import android.widget.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.geojson.*
/**/
//import android.content.Context
//import android.content.Intent
//import android.os.PersistableBundle
//import android.provider.ContactsContract
//import android.support.design.widget.Snackbar
//import android.view.Menu
//import android.view.MenuItem
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.style.light.Position
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

//import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, LocationEngineListener, DownloadCompleteListener  {


    private val tag = "MainActivity"

    private var downloadDate = "" // YYYY/MM/DD
    private var todaysDate = ""
    private var calendar = Calendar.getInstance()
    private var calMonth = calendar.get(Calendar.MONTH) + 1
    private var calDay = calendar.get(Calendar.DAY_OF_MONTH)
//    private var dateString = "" + calendar.get(Calendar.YEAR) + "/" + calMonth + "/" + calendar.get(Calendar.DAY_OF_MONTH)
//
//    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
//    private var localDate = LocalDate.now()
//
//
//
//    private var todaysDate = dateFormatter.format(localDate)




    private val preferencesFile = "MyPrefsFile"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager

    private lateinit var JSONstring: DownloadFileTask

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "[onCreate] method")
        setContentView(R.layout.activity_main)

        if (calDay < 10 && calMonth < 10) todaysDate = "" + calendar.get(Calendar.YEAR) + "/0" + calMonth + "/0" + calendar.get(Calendar.DAY_OF_MONTH)
        else if (calDay > 10 && calMonth < 10) todaysDate = "" + calendar.get(Calendar.YEAR) + "/0" + calMonth + "/" + calendar.get(Calendar.DAY_OF_MONTH)
        else if (calDay < 10 && calMonth > 10) todaysDate = "" + calendar.get(Calendar.YEAR) + "/" + calMonth + "/0" + calendar.get(Calendar.DAY_OF_MONTH)
        else todaysDate = "" + calendar.get(Calendar.YEAR) + "/" + calMonth + "/" + calendar.get(Calendar.DAY_OF_MONTH)

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            Log.d(tag, "[onMapReady] mapboxMap is null")
        } else {
            Log.d(tag, "[onMapReady] mapboxMap is not null")
            map = mapboxMap
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isZoomControlsEnabled = true
            enableLocation()
            JSONstring = DownloadFileTask(this)
            Log.d(tag, "todays date is $todaysDate")
            JSONstring.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$todaysDate/coinzmap.geojson")
        }
    }


    override fun downloadComplete(result: String) {
        Log.d(tag, "[downloadComplete]")
        val featureCollection = FeatureCollection.fromJson(result)
        val features = featureCollection.features()
        for (Feature in features!!) {
                val coordinates = (Feature.geometry() as Point).coordinates()
                val jsonObject = Feature.properties()
                val currency = jsonObject?.get("currency").toString()
                val value = jsonObject?.get("value").toString()
//                val icon = IconFactory.getInstance(this)
//                val coin = icon.fromResource(R.drawable.new_coin)
                map.addMarker(MarkerOptions().position(LatLng(coordinates[1], coordinates[0])).title(currency).snippet(value))
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        Log.d(tag, "[initialiseLocationEngine] method")
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else { locationEngine?.addLocationEngineListener(this) }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        Log.d(tag, "[initialiseLocationLayer] method")
        locationLayerPlugin = LocationLayerPlugin(mapView, map, locationEngine)
        locationLayerPlugin?.apply {
            setLocationLayerEnabled(true)
            cameraMode = CameraMode.TRACKING
            renderMode = RenderMode.NORMAL
        }
    }

    private fun setCameraPosition(location: Location) {
        Log.d(tag, "[setCameraPosition] method")
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 13.0))
    }

    override fun onLocationChanged(location: Location?) {
        Log.d(tag, "[onLocationChanged] method")
        location?.let {
            originLocation = location
            setCameraPosition(location)
        }
    }
    //
    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }
    //
//
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) { // override
        Log.d(tag, "[onExplanationNeeded] Permissions: $permissionsToExplain")
        // Present popup message or dialog
        Toast.makeText(this, "Permission needed to access location for gameplay.", Toast.LENGTH_LONG).show()
    }
    //
    override fun onPermissionResult(granted: Boolean) { // override
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open a dialogue with the user
        }
    }
    //
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(tag, "[onRequestPermissionsResult] method")
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onStart() {
        super.onStart()
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        downloadDate = settings.getString("lastDownloadDate", "")
        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()

    }

    override fun onResume() {
        Log.d(tag, "[onResume] method")
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        Log.d(tag, "[onPause] method")
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationLayerPlugin?.onStop()
        mapView.onStop()
        Log.d(tag, "[onStop] Storing lastDownloadDate of $downloadDate")
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.apply()
    }

    override fun onDestroy() {
        Log.d(tag, "[onDestroy] method")
        super.onDestroy()
        locationEngine?.deactivate()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        Log.d(tag, "[onLowMemory] method")
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        Log.d(tag, "[onSaveInstanceState] method")
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

}