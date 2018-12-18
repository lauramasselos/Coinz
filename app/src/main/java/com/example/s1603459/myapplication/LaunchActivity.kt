@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.firebase.auth.FirebaseAuth

class LaunchActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var startBtn: Button? = null
    private var mProgressBar: ProgressDialog? = null

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
            if (user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }
}
