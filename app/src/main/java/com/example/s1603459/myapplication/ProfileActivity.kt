package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_profile.*

// This class is used to show the current user's profile, and how much gold they have at this point.

class ProfileActivity : AppCompatActivity() {

    private val tag = "ProfileActivity"

    // Firebase references
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null // Current user
    private var firestore: FirebaseFirestore? = null // Firestore used to read from/write to database
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreUsers: CollectionReference? = null // Reference to list of all accounts on Coinz app
    private var firestoreBanked: CollectionReference? = null // Reference to where in database bankec coins are to be stored

    // UI elements
    private var userName: TextView? = null
    private var usersEmail: TextView? = null
    private var usersGold: TextView? = null
    private var tvBackToMap: TextView? = null
    private var tvLogout: TextView? = null

    // Current user's email
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initialise()
    }

    // Initialises variables, UI elements, (TextView) buttons and Firebase references
    private fun initialise() {
        if (!connected()) { // If there's no internet connection, restart activity on click of Retry button
            Snackbar.make(coordinatorLayout_profile, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        finish()
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }.show()
        } else {
            mDatabase = FirebaseDatabase.getInstance()
            mAuth = FirebaseAuth.getInstance()
            user = mAuth!!.currentUser
            email = user!!.email!!
            firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
            firestore?.firestoreSettings = settings
            firestoreUsers = firestore?.collection("Users")
            firestoreBanked = firestore?.collection("Users")?.document(email)?.collection("Banked")
            userName = findViewById<View>(R.id.userText) as TextView
            usersEmail = findViewById<View>(R.id.userEmailText) as TextView
            usersGold = findViewById<View>(R.id.goldText) as TextView
            tvLogout = findViewById<View>(R.id.tv_sign_out) as TextView
            tvLogout!!.setOnClickListener { signOut() }
            usersEmail!!.text = email
            getName()
            getGold()
            tvBackToMap = findViewById<View>(R.id.tv_back_to_map) as TextView
            tvBackToMap!!.setOnClickListener {
                finish()
                startActivity(Intent(this, MainActivity::class.java)) }
        }
    }

    // Signs user out and redirects them to the login screen
    private fun signOut() {
        mAuth!!.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        finish()
        startActivity(intent)
    }

    // Retrieves user's name from Firebase to display in-app
    @SuppressLint("SetTextI18n")
    private fun getName() {
        firestoreUsers!!.document(email).get().addOnSuccessListener { userInfo ->
            val name = userInfo.get("name").toString()
            userName!!.text = "Welcome back, $name!"
        }
    }

    // Retrieves user's gold from Firebase to display in-app, and calls method to set user's level based on gold collected
    private fun getGold() {
        firestoreBanked!!.get().addOnSuccessListener { firebaseGold ->
            var goldTotal = 0.0
            for (coin in firebaseGold) {
                goldTotal += coin.data["GOLD"] as Double
            }
            Log.d(tag, "[getGold] Gold total $goldTotal")
            usersGold!!.text = "GOLD: $goldTotal"
            setLevel(goldTotal)
        }
    }

    // BONUS FEATURE: Sets user's level to be GOLD / 50000 rounded down to the nearest integer. The level a user is on is how many metres greater than 25 their pick-up radius is when collecting coins
    private fun setLevel(gold: Double) {
        val level = gold.toInt() / 50000
        val newLevel = mapOf("level" to level, "gold" to gold)
        firestoreUsers!!.document(email).update(newLevel).addOnCompleteListener {
            Log.d(tag, "User $email's level successfully updated to level $level with gold $gold")
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

}
