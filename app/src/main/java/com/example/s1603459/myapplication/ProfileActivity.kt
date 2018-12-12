package com.example.s1603459.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.util.Log
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*

class ProfileActivity : AppCompatActivity() {

    private val tag = "ProfileActivity"
    private var addFriendBtn: Button? = null
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreUsers: CollectionReference? = null

    private var coinzUsers: ArrayList<String> = ArrayList()
    private lateinit var userEmail: String

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
        userEmail = user!!.email!!
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreUsers = firestore?.collection("Users")
        addFriendBtn = findViewById(R.id.addFriendBtn)
        addFriendBtn!!.setOnClickListener{ searchUsers() }


    }

    private fun searchUsers() {
        firestoreUsers?.get()?.addOnSuccessListener {users ->
            for (user in users) {
                coinzUsers.add(user.toString())
                Log.d(tag, "[onCreate] $user")
            }
            // all users' emails in a list
            // probably better to add friends in another activity; have a button that passes an intent to start AddFriendActivity





        }
    }


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
//    Menu menu = navigation.getMenu();
//    MenuItem menuItem = menu.getItem(INSERT_INDEX_HERE);
//    menuItem.setChecked(true);

}
