package com.example.s1603459.myapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.content.Intent
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MapFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MapFragment : Fragment(), OnMapReadyCallback, PermissionsListener, LocationEngineListener, DownloadCompleteListener  {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null


    // copy

//    private val tag = "MainActivity"

    private val COLLECTION_KEY = "Users"
    private val SUB_COLLECTION_KEY = "Wallet"
    private val ID_FIELD = "id"
    private val VALUE_FIELD = "value"
    private val CURRENCY_FIELD = "currency"
    private val COLLECTED_BY_USER_FIELD = "collectedByUser"

    private var downloadDate = "" // YYYY/MM/DD

    private val preferencesFile = "MyPrefsFile"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private lateinit var originLocation: Location
    private lateinit var permissionsManager: PermissionsManager

    private lateinit var jsonString: DownloadFileTask
    private lateinit var geoJSONFeatureCollection: FeatureCollection

    private lateinit var mDatabaseReference: DatabaseReference

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    private var coinsCollected: ArrayList<String> = ArrayList()

    //Firebase references
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreUsers: CollectionReference? = null
    private var firestoreUser: DocumentReference? = null
    private var firestoreWallet: CollectionReference? = null
    private var firestoreExchangeRates: DocumentReference? = null



    //UI elements
    private var name: String? = null
    private var email: String? = null
    private var isEmailVerified: Boolean? = null
    private var btnLogout: Button? = null
    private var markers: HashMap<String, Marker> = HashMap()
    private var markersRemoved: HashMap<String, Marker> = HashMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(tag, "[onCreate] method")
//        setContentView(R.layout.activity_main)
        initialise()
//        setSupportActionBar(toolbar)
        downloadDate = SimpleDateFormat("YYYY/MM/dd").format(Date())

        Mapbox.getInstance(this.requireContext(), getString(R.string.access_token))
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mDatabaseReference = mDatabase!!.reference.child("Users")
        Log.d(tag, "[initialise] mDatabase $mDatabase")
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        name = user!!.displayName
        email = user!!.email
        isEmailVerified = user?.isEmailVerified
        btnLogout = view?.findViewById<View>(R.id.signOutBtn) as Button
        btnLogout!!.setOnClickListener { signOut() }
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreUser = firestore?.collection(COLLECTION_KEY)?.document(email!!)
        firestoreWallet = firestore?.collection(COLLECTION_KEY)?.document(email!!)?.collection(SUB_COLLECTION_KEY) // path Users/<user>/Wallet
        firestoreExchangeRates = firestore?.collection("Exchange Rates")?.document("Today's Exchange Rate")
        firestoreUsers = firestore?.collection("Users")
    }

    private fun signOut() {
        mAuth!!.signOut()
        startActivity(Intent(this.activity, LoginActivity::class.java))                    //TODO Make sure to remove all remnants of signing out in fragment
    }

    override fun onStart() {
        super.onStart()
        Log.d(tag, "[onStart] Recalled lastDownloadDate is $downloadDate")

        if (PermissionsManager.areLocationPermissionsGranted(this.context)) {
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
            jsonString = DownloadFileTask(this)
            Log.d(tag, "todays date is $downloadDate")
            jsonString.execute("http://homepages.inf.ed.ac.uk/stg/coinz/$downloadDate/coinzmap.geojson")
        }
    }


    override fun downloadComplete(result: String) {
        Log.d(tag, "[downloadComplete]")
        storeExchangeRates(result)
        geoJSONFeatureCollection = FeatureCollection.fromJson(result)
        firestoreWallet?.get()?.addOnSuccessListener { wallet ->
            for (coin in wallet) {
                if (coin.data[COLLECTED_BY_USER_FIELD] == "true") {
                    coinsCollected.add(coin.id)
                }
            }
            dropMarkers(geoJSONFeatureCollection)
            Log.d(tag, "Collection $coinsCollected")
        }?.addOnFailureListener{
            dropMarkers(geoJSONFeatureCollection)
            Log.d(tag, "Error getting wallet")
        }

    }


    private fun storeExchangeRates(result: String) {
        Log.d(tag, "[storeExchangeRates] $result")
        val jsonObject = JSONObject(result)
        val jsonRates = jsonObject.getJSONObject("rates")

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


    private fun dropMarkers(GeoJSONFeatureCollection: FeatureCollection) {
        val features = GeoJSONFeatureCollection.features()
        for (Feature in features!!) {
            val coordinates = (Feature.geometry() as Point).coordinates()
            val jsonObject = Feature.properties()
            val currency = (jsonObject?.get(CURRENCY_FIELD).toString()).replace("\"", "")
            val value = (jsonObject?.get(VALUE_FIELD).toString()).replace("\"", "")
            val id = jsonObject?.get(ID_FIELD).toString()
            val icon = IconFactory.getInstance(this.requireContext())
            val coin = icon.fromResource(R.drawable.pixelcoin)
            if (!coinsCollected.contains(id)) {
                val marker = map.addMarker(MarkerOptions().position(LatLng(coordinates[1], coordinates[0])).title(currency).snippet(value).icon(coin))
                markers[id] = marker
            } else {
                Log.d(tag, "Coin $id already collected!")
            }
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this.context)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this.activity)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {
        Log.d(tag, "[initialiseLocationEngine] method")
        locationEngine = LocationEngineProvider(this.context).obtainBestLocationEngineAvailable()
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

        removeMarkers(markers, location)
    }

    private fun removeMarkers(markersMap: HashMap<String, Marker>, location: Location?) {
        firestoreUsers!!.document(email!!).get().addOnSuccessListener { userInfo ->
            val level = (userInfo.get("level") as Long).toInt()

            for (marker in markersMap) {
                val markerLocation = Location("")
                markerLocation.latitude = marker.value.position.latitude
                markerLocation.longitude = marker.value.position.longitude
                val distanceToMarker = location!!.distanceTo(markerLocation)
                if (distanceToMarker <= 25 + level) {
                    markersRemoved[marker.key] = marker.value
                    Log.d(tag, "[removeMarkers] user's level is $level, and collect from a distance of ${25+level} metres")
                }
            }

            for (marker in markersRemoved) {
                if (markersMap.containsKey(marker.key)) {
                    marker.value.remove()
                    markersMap.remove(marker.key)
                    addCoinToWallet(marker.key)
                }
            }
        }
    }

    private fun addCoinToWallet(coinId: String) {
        val features = geoJSONFeatureCollection.features()
        var currency = ""
        var value = ""

        for (Feature in features!!) {
            val jsonObject = Feature.properties()
            if (jsonObject?.get(ID_FIELD).toString() == coinId) {
                currency = jsonObject?.get(CURRENCY_FIELD).toString()
                value = jsonObject?.get(VALUE_FIELD).toString()

            }
        }

        val newCoin = Coin(coinId, "false", "true", currency, downloadDate, value, "false")



        firestoreWallet?.document(coinId)?.set(newCoin)?.addOnSuccessListener {
            Toast.makeText(this.context, "Coin added to wallet", Toast.LENGTH_SHORT).show()}
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
        Toast.makeText(this.context, "Permission needed to access location for gameplay.", Toast.LENGTH_LONG).show()
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
//        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)
//        val editor = settings.edit()
//        editor.putString("lastDownloadDate", downloadDate)
//        editor.apply()
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

//    override fun onSaveInstanceState(outState: Bundle?) {
//        Log.d(tag, "[onSaveInstanceState] method")
//        super.onSaveInstanceState(outState)
//        super.onSaveInstanceState(outState)
//        if (outState != null) {
//            mapView.onSaveInstanceState(outState)
//        }
//    }


    // end copy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MapFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                MapFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
