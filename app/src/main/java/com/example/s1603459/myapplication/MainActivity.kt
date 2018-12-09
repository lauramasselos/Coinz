package com.example.s1603459.myapplication

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.style.light.Position
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

//import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, LocationEngineListener, DownloadCompleteListener  {


    private val tag = "MainActivity"

    private var downloadDate = "" // YYYY/MM/DD

    private val preferencesFile = "MyPrefsFile"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager

    private lateinit var JSONstring: DownloadFileTask
    private lateinit var GeoJSONFeatureCollection: FeatureCollection

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    //Firebase references
    private var mDatabaseReference: DatabaseReference? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreWallet: CollectionReference? = null



    //UI elements
    private var name: String? = null
    private var email: String? = null
    private var isEmailVerified: Boolean? = null
    private var btnLogout: Button? = null
    private var markers: HashMap<String, Marker> = HashMap<String, Marker>()
    private var markersRemoved: HashMap<String, Marker> = HashMap<String, Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "[onCreate] method")
        setContentView(R.layout.activity_main)
        initialise()
//        setSupportActionBar(toolbar)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        downloadDate = SimpleDateFormat("YYYY/MM/dd").format(Date())

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }


    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mDatabaseReference = mDatabase!!.reference!!.child("Users/Wallet")
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        name = user?.displayName
        email = user?.email
        isEmailVerified = user?.isEmailVerified
        btnLogout = findViewById<View>(R.id.signOutBtn) as Button
        btnLogout!!.setOnClickListener { signOut() }
        firestore = FirebaseFirestore.getInstance()
// Use com.google.firebase.Timestamp objects instead of java.util.Date objects
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreWallet = firestore?.collection(COLLECTION_KEY)?.document(email!!)?.collection(SUB_COLLECTION_KEY) // path Users/<user>/Wallet
//        realtimeUpdateListener()
    }

    private fun signOut() {
        mAuth!!.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
//        downloadDate = settings.getString("lastDownloadDate", "")
        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")

        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine?.requestLocationUpdates()
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()

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
            Log.d(tag, "todays date is $downloadDate")
            JSONstring.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson")
        }
    }


    override fun downloadComplete(result: String) {
        Log.d(tag, "[downloadComplete]")
        GeoJSONFeatureCollection = FeatureCollection.fromJson(result)
        val features = GeoJSONFeatureCollection.features()
        for (Feature in features!!) {
            val coordinates = (Feature.geometry() as Point).coordinates()
            val jsonObject = Feature.properties()
            val currency = jsonObject?.get("currency").toString()
            val value = jsonObject?.get("value").toString()
            val id = jsonObject?.get("id").toString()
//                val icon = IconFactory.getInstance(this)
//                val coin = icon.fromResource(R.drawable.new_coin)
            val marker = map.addMarker(MarkerOptions().position(LatLng(coordinates[1], coordinates[0])).title(currency).snippet(value))
            markers.put(id, marker)
            Log.d(tag, "[downloadComplete] added marker")
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
        val latlng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    override fun onLocationChanged(location: Location?) {
        Log.d(tag, "[onLocationChanged] method")
        location?.let {
            originLocation = location
            setCameraPosition(location)
        }
        for (marker in markers) {
            val markerLocation = Location("")
            markerLocation.latitude = marker.value.position.latitude
            markerLocation.longitude = marker.value.position.longitude
            val distanceToMarker = location!!.distanceTo(markerLocation)
            if (distanceToMarker <= 25) {
                marker.value.remove()
                markersRemoved.put(marker.key, marker.value)
            }
        }
        for (marker in markersRemoved) {
            if (markers.containsKey(marker.key)) {
                markers.remove(marker.key)
                addCoinToWallet(marker.key)
            }
        }
    }

    private fun addCoinToWallet(coinId: String) {
        val features = GeoJSONFeatureCollection.features()
        var currency = ""
        var value = ""

        for (Feature in features!!) {
            val jsonObject = Feature.properties()
            if (jsonObject?.get("id").toString() == coinId) {
               currency = jsonObject?.get("currency").toString()
               value = jsonObject?.get("value").toString()

            }
        }

        val newCoin = mapOf(
                ID_FIELD to coinId,
                VALUE_FIELD to value,
                CURRENCY_FIELD to currency,
                DATE_FIELD to downloadDate
        )



        firestoreWallet?.document(coinId)?.set(newCoin)?.addOnSuccessListener {
            Toast.makeText(this, "Coin added to wallet", Toast.LENGTH_SHORT).show()}
                ?.addOnFailureListener{
                   Log.d(tag, "Failed to add coin to wallet")
                }


    }


    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }


    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) { // override
        Log.d(tag, "[onExplanationNeeded] Permissions: $permissionsToExplain")
        // Present popup message or dialog
        Toast.makeText(this, "Permission needed to access location for gameplay.", Toast.LENGTH_LONG).show()
    }

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


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                message.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map-> {
                message.setText(R.string.title_map)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_bank -> {
                message.setText(R.string.title_bank)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    companion object {
        private const val tag = "MainActivity"
        private const val COLLECTION_KEY = "Users"
        private const val SUB_COLLECTION_KEY = "Wallet"
        private const val DOCUMENT_KEY = "Coin"
        private const val ID_FIELD = "ID"
        private const val VALUE_FIELD = "Value"
        private const val CURRENCY_FIELD = "Currency"
        private const val DATE_FIELD = "Date collected"
    }


}