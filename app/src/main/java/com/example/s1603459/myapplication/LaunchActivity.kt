@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

// This class is used upon launching the app: it's purpose is to allow users who have logged in previously to remain logged in upon exiting the app

class LaunchActivity : AppCompatActivity() {

    // Firebase authentication
    private lateinit var mAuth: FirebaseAuth

    // UI elements
    private var startBtn: Button? = null
    private var mProgressBar: ProgressDialog? = null

    // Initialises authentication: if current user is not null, jump to MainActivity; otherwise, user is redirected to login
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        mProgressBar = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        startBtn = findViewById<View>(R.id.startButton) as Button
        startBtn!!.setOnClickListener {
            mProgressBar!!.setMessage("Loading...")
            mProgressBar!!.show()
            if (user == null) { // no one logged in on this device
                startActivity(Intent(this, LoginActivity::class.java))
            } else { // someone is logged in on this device
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}
