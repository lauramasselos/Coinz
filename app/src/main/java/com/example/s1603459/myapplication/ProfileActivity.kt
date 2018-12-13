package com.example.s1603459.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_profile.*
import org.w3c.dom.Text

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

    private var coinzUsers: ArrayList<String> = ArrayList()
    private lateinit var mUserEmail: String
    private lateinit var mUserName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        initialise()
    }

    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        mUserEmail = user!!.email!!
//        mUserName = user?.displayName!!
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreUsers = firestore?.collection("Users")
        firestoreBanked = firestore?.collection("Users")?.document(mUserEmail)?.collection("Banked")
//        userName = findViewById<View>(R.id.userText) as TextView
        usersEmail = findViewById<View>(R.id.userEmailText) as TextView
        usersGold = findViewById<View>(R.id.goldText) as TextView

//        userName!!.text = mUserName
        usersEmail!!.text = mUserEmail
        getGold()

    }

    private fun getGold() {
        firestoreBanked!!.get().addOnSuccessListener { firebaseGold ->
            var goldTotal = 0.0
            for (coin in firebaseGold) {
                goldTotal += coin.data["GOLD"] as Double
            }
            Log.d(tag, "[getGold] Gold total $goldTotal")
            usersGold!!.text = "GOLD: $goldTotal"
            setLevel(goldTotal.toInt())
        }
    }

    private fun setLevel(gold: Int) {
        val level = gold % 50000
        val newLevel = mapOf("Level" to level)
        firestoreUsers!!.document(mUserEmail).update(newLevel).addOnCompleteListener {
            Log.d(tag, "User $mUserEmail's level successfully updated to level $level")
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


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map-> {
                startActivity(Intent(this@ProfileActivity, MainActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_bank -> {
                startActivity(Intent(this@ProfileActivity, BankActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }

        }
        false
    }

}
