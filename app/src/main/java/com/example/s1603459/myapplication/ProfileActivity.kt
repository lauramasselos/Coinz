package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class ProfileActivity : AppCompatActivity() {

    private val tag = "ProfileActivity"
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreUsers: CollectionReference? = null
    private var firestoreBanked: CollectionReference? = null
    private var userName: TextView? = null
    private var usersEmail: TextView? = null
    private var usersGold: TextView? = null

    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        initialise()
    }

    private fun initialise() {
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

        usersEmail!!.text = email
        getName()
        getGold()

    }

    @SuppressLint("SetTextI18n")
    private fun getName() {
        firestoreUsers!!.document(email).get().addOnSuccessListener { userInfo ->
            val name = userInfo.get("name").toString()
            userName!!.text = "Welcome back, $name!"
        }
    }

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

    private fun setLevel(gold: Double) {
        val level = gold.toInt() / 50000
        val newLevel = mapOf("level" to level, "gold" to gold)
        firestoreUsers!!.document(email).update(newLevel).addOnCompleteListener {
            Log.d(tag, "User $email's level successfully updated to level $level with gold $gold")
        }
    }

//    private fun searchUsers() {
//        firestoreUsers?.get()?.addOnSuccessListener {users ->
//            for (user in users) {
//                coinzUsers.add(user.toString())
//                Log.d(tag, "[onCreate] $user")
//            }
//            // all users' emails in a list
//            // probably better to add friends in another activity; have a button that passes an intent to start AddFriendActivity
//
//
//
//
//
//        }
//    }

}
