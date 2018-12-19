package com.example.s1603459.myapplication

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_forgotten_password.*

// This class is used when an existing user has forgotten their password, and sends them an email where they can set a new password

class ForgottenPasswordActivity : AppCompatActivity() {

    private val tag = "ForgotPasswordActivity"

    // UI elements
    private var etEmail: EditText? = null
    private var btnSubmit: Button? = null

    // Firebase authentication
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotten_password)
        initialise()
    }

    // Initialises EditText, button, and authentication
    private fun initialise() {
        etEmail = findViewById<View>(R.id.et_email) as EditText
        btnSubmit = findViewById<View>(R.id.btn_submit) as Button
        mAuth = FirebaseAuth.getInstance()
        btnSubmit!!.setOnClickListener { sendPasswordResetEmail() }
    }

    // Sends user with input email an email to reset their password
    private fun sendPasswordResetEmail() {
        val email = etEmail?.text.toString()
        if (!TextUtils.isEmpty(email)) {
            mAuth!!.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Snackbar.make(coordinatorLayout_forgot_password, "Password reset email sent.", Snackbar.LENGTH_SHORT).show()
                    updateToLoginUI()
                } else {
                    Log.w(tag, task.exception!!.message)
                    Snackbar.make(coordinatorLayout_forgot_password, "No user found with this email.", Snackbar.LENGTH_SHORT).show()
                }
            }
        } else { // Email field left empty
           Snackbar.make(coordinatorLayout_forgot_password, "Enter Email.", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Upon sending a reset email, the user is redirected to the login screen
    private fun updateToLoginUI() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }


}
