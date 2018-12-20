package com.example.s1603459.myapplication

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
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
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// This class is used as the main UI for the game: it's where users can track their location and collect coins on a different map every day

class MainActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener, LocationEngineListener, DownloadCompleteListener  {

    private val tag = "MainActivity"

    // Used to download correct GeoJSON file everyday, and store coins collected with appropriate date in Firebase
    private var downloadDate = ""

    // Location variables / MapBox usage
    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager
    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    // Firebase references
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null // Current user
    private var firestore: FirebaseFirestore? = null // Firestore used to read from/write to database
    private var firestoreWallet: CollectionReference? = null // Reference to where in database coins in wallet are to be stored
    private var firestoreExchangeRates: DocumentReference? = null // Reference to where in database today's exchange rates are to be stored
    private var firestoreUsers: CollectionReference? = null // Reference to list of all accounts on Coinz app
    private var firestoreUser: DocumentReference? = null // Reference to current user's information
    private lateinit var mDatabaseReference: DatabaseReference

    // Buttons
    private var btnBank: FloatingActionButton? = null
    private var btnProfile: FloatingActionButton? = null

    // Variables for backend
    private var markers: HashMap<String, Marker> = HashMap()
    private var markersRemoved: HashMap<String, Marker> = HashMap()
    private var coinsCollected: ArrayList<String> = ArrayList()
    private var name: String? = null
    private var email: String? = null
    private var isEmailVerified: Boolean? = null
    private val preferencesFile = "MyPrefsFile"
    private lateinit var jsonString: DownloadFileTask
    private lateinit var geoJSONFeatureCollection: FeatureCollection


    // ANDROID LIFECYCLE //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "[onCreate] method")
        setContentView(R.layout.activity_main)
        initialise()
        downloadDate = SimpleDateFormat("YYYY/MM/dd").format(Date())
        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        // Get MapView map with MapBox
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }
    // If location permissions are granted, then this allows location tracking in-app
    override fun onStart() {
        super.onStart()
        getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")

        // Start tracking
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

    // Initialises variables, buttons, and Firebase references
    private fun initialise() {
        if (!connected()){ // If there's no internet connection, restart activity on click of Retry button
            Log.d(tag, "[initialise] !connected()")
            Snackbar.make(coordinatorLayout, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }.show()
        } else {
            mDatabase = FirebaseDatabase.getInstance()
            mDatabaseReference = mDatabase!!.reference.child("Users")
            Log.d(tag, "[initialise] mDatabase $mDatabase")
            mAuth = FirebaseAuth.getInstance()
            user = mAuth!!.currentUser
            name = user!!.displayName
            email = user!!.email
            isEmailVerified = user?.isEmailVerified
            btnBank = findViewById<View>(R.id.bankBtn) as FloatingActionButton
            btnProfile = findViewById<View>(R.id.profileBtn) as FloatingActionButton
            btnBank!!.setOnClickListener {
                finish()
                startActivity(Intent(this, BankActivity::class.java)) }
            btnProfile!!.setOnClickListener {
                finish()
                startActivity(Intent(this, ProfileActivity::class.java)) }
            firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
            firestore?.firestoreSettings = settings
            firestoreUser = firestore?.collection(COLLECTION_KEY)?.document(email!!)
            firestoreWallet = firestore?.collection(COLLECTION_KEY)?.document(email!!)?.collection(SUB_COLLECTION_KEY) // path Users/<user>/Wallet
            firestoreExchangeRates = firestore?.collection("Exchange Rates")?.document("Today's Exchange Rate")
            firestoreUsers = firestore?.collection("Users")
        }
    }

    // Called when the map is ready, this enables user location tracking, and downloads the GeoJSON file from the internet using DownloadFileTask()
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (!connected()) { // If there's no internet connection, restart activity on click of Retry button
            Log.d(tag, "[onMapReady] !connected()")
            Snackbar.make(coordinatorLayout, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        finish()
                        startActivity(Intent(this, MainActivity::class.java))
                    }.show()
        } else {
            if (mapboxMap == null) {
                Log.d(tag, "[onMapReady] mapboxMap is null")
            } else {
                Log.d(tag, "[onMapReady] mapboxMap is not null")
                map = mapboxMap
                map.uiSettings.isCompassEnabled = true
                enableLocation()
                jsonString = DownloadFileTask(this) // Create a DownloadFileTask instance
                Log.d(tag, "todays date is $downloadDate")

                // Upon completion of .execute(...), the function downloadComplete(result) is called
                jsonString.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson") // Download the file the URL links to
            }
        }
    }

    // This stores today's exchange rates on Firebase, and drops markers where each coin (that hasn't been collected) on the map
    override fun downloadComplete(result: String) {
        storeExchangeRates(result)
        geoJSONFeatureCollection = FeatureCollection.fromJson(result)
        firestoreWallet?.get()?.addOnSuccessListener { wallet ->
            for (coin in wallet) {
                if (coin.data[COLLECTED_BY_USER_FIELD] == "true") { // Keeps track of what coins the user has collected
                    coinsCollected.add(coin.id)
                }
            }
            dropMarkers(geoJSONFeatureCollection)
            Log.d(tag, "Collection $coinsCollected")
        }?.addOnFailureListener{

            // This is called even in onFailureListener{} because an error getting wallet is likely to be because a new user has yet to collect any coins, and so their wallet doesn't exist yet
            dropMarkers(geoJSONFeatureCollection)
            Log.d(tag, "Error getting wallet")
        }
    }

    // Upon granting location permissions, this function allows location tracking of the user and initialises the LocationEngine and LocationLayerPlugin
    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            // Initialise location tracking
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this) // Redirect to request location permissions
        }
    }

    // Initialises location tracking
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

    // Initialises visible location tracking in-app, i.e. so camera follows location on map
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

    // Sets MapView to be where user's current location is, i.e. to follow location on map
    private fun setCameraPosition(location: Location) {
        Log.d(tag, "[setCameraPosition] method")
        val latlng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLng(latlng))
    }

    // Updates location when user moves, and calls removeMarkers(markers, location) to check if any markers are within the pick-up radius to remove them
    override fun onLocationChanged(location: Location?) {
            Log.d(tag, "[onLocationChanged] method")
            location?.let {
                originLocation = location
                setCameraPosition(location)
            }

            removeMarkers(markers, location)
    }

    // If connected to the internet and location permissions are granted, requests location updates
    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }

    // Gives user explanation as to what location permissions are needed for
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Log.d(tag, "[onExplanationNeeded] Permissions: $permissionsToExplain")
        Snackbar.make(coordinatorLayout, "Permission needed to access location for game play.", Snackbar.LENGTH_LONG).show()
    }

    // Allows (or doesn't allow) enabling of location based on whether the user has allowed or denied location permissions
    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            Snackbar.make(coordinatorLayout, "This app requires location permissions. If you wish to play Coinz, please allow location access.", Snackbar.LENGTH_LONG).show()
            finish()
        }
    }

    // Adjusts permissionsManager
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(tag, "[onRequestPermissionsResult] method")
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Stores the exchange rates in today's GeoJSON file onto Firebase
    private fun storeExchangeRates(result: String) {
        val jsonObject = JSONObject(result) // Return GeoJSON file String as JSONObject
        val jsonRates = jsonObject.getJSONObject("rates")

        // Map of exchange rates
        val newER = mapOf(
                "SHIL" to jsonRates.get("SHIL"),
                "DOLR" to jsonRates.get("DOLR"),
                "QUID" to jsonRates.get("QUID"),
                "PENY" to jsonRates.get("PENY")
        )

        firestoreExchangeRates?.set(newER)?.addOnSuccessListener {
            Log.d(tag, "Exchange rates updated for $downloadDate on Firebase")
        }?.addOnFailureListener{
            Log.d(tag, "Failed to set exchange rates for $downloadDate")
        }
    }

    // Drops markers of coins not yet collected by the user by looking at what they have in their Firebase wallet (or if they have one)
    private fun dropMarkers(GeoJSONFeatureCollection: FeatureCollection) {
        val features = GeoJSONFeatureCollection.features()
        for (Feature in features!!) {
            // Location of each coin
            val coordinates = (Feature.geometry() as Point).coordinates()
            val jsonObject = Feature.properties()

            // Currency of each coin
            val currency = (jsonObject?.get(CURRENCY_FIELD).toString()).replace("\"", "")

            // Value of each coin
            val value = (jsonObject?.get(VALUE_FIELD).toString()).replace("\"", "")

            // ID of coin
            val id = jsonObject?.get(ID_FIELD).toString()

            // Icon used to represent each coin on map
            val icon = IconFactory.getInstance(this)
            val coin = icon.fromResource(R.drawable.pixelcoin)

            if (!coinsCollected.contains(id)) { // If a coin with this ID hasn't been collected, drop a marker on the map
                val marker = map.addMarker(MarkerOptions().position(LatLng(coordinates[1], coordinates[0])).title(currency).snippet(value).icon(coin))
                markers[id] = marker // Store this marker with the coin's ID into a HashMap for later use
            } else {
                Log.d(tag, "Coin $id already collected!")
            }
        }
    }

    // Checks if any markers are in the user's pick-up radius (dependant on their level), and if so, removes them and calls functions to add coin to wallet on Firebase
    private fun removeMarkers(markersMap: HashMap<String, Marker>, location: Location?) {
        firestoreUsers!!.document(email!!).get().addOnSuccessListener { userInfo ->
            val level = (userInfo.get("level") as Long).toInt() // Gets user's level

            // Loops through all markers currently on map and adds markers within pick-up distance of user to a HashMap of (Coin ID, Marker) pairs
            for (marker in markersMap) {
                val markerLocation = Location("")
                markerLocation.latitude = marker.value.position.latitude
                markerLocation.longitude = marker.value.position.longitude
                val distanceToMarker = location!!.distanceTo(markerLocation)
                if (distanceToMarker <= 25 + level) { // Checks if marker is within pick-up distance of user (min. 25 metres)
                    markersRemoved[marker.key] = marker.value // If so, add HashMap entry to different HashMap that keeps track of markers to be removed
                    Log.d(tag, "[removeMarkers] user's level is $level, and collect from a distance of ${25+level} metres")
                }
            }

            // Loops through HashMap of all markers (to be) removed, removes the marker from the map, removes the entry from the first HashMap of markers, and calls method to add coin to wallet
            for (marker in markersRemoved) {
                if (markersMap.containsKey(marker.key)) {
                    marker.value.remove() // Removes marker from map
                    markersMap.remove(marker.key) // Removes marker from initial HashMap
                    addCoinToWallet(marker.key) // Adds coin to Firebase wallet
                }
            }
        }
    }

    // Adds coin picked up to Firebase wallet of user
    private fun addCoinToWallet(coinId: String) {
        val features = geoJSONFeatureCollection.features()
        var currency = ""
        var value = ""

        // Loops through all features in GeoJSON file to find correct coin ID; then gets the coin's value and currency from here
        for (Feature in features!!) {
            val jsonObject = Feature.properties()
            if (jsonObject?.get(ID_FIELD).toString() == coinId) {
                currency = jsonObject?.get(CURRENCY_FIELD).toString()
                value = jsonObject?.get(VALUE_FIELD).toString()
            }
        }

        val newCoin = Coin(coinId, "false", "true", currency, downloadDate, value, "false")

        // Stores a new Coin object to user's Firebase wallet
        firestoreWallet?.document(coinId)?.set(newCoin)?.addOnSuccessListener {
            Snackbar.make(coordinatorLayout, "Coin added to wallet", Snackbar.LENGTH_SHORT).show()
        }?.addOnFailureListener{
            Log.d(tag, "Failed to add coin to wallet")
        }
    }

    // Method returning Boolean that checks if device is connected to the internet
    private fun connected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }

    // Companion object storing keys/fields for ease of editing source code and therefore ease of renaming/editing how Firebase database is maintained
    companion object {
        private const val COLLECTION_KEY = "Users"
        private const val SUB_COLLECTION_KEY = "Wallet"
        private const val ID_FIELD = "id"
        private const val VALUE_FIELD = "value"
        private const val CURRENCY_FIELD = "currency"
        private const val COLLECTED_BY_USER_FIELD = "collectedByUser"
    }

}