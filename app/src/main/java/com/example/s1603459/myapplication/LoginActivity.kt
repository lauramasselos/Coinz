@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.*
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_login.*

// This class is used to login a user to the Coinz app.

class LoginActivity : AppCompatActivity() {


    private val tag = "LoginActivity"

    // Input variables
    private var email: String? = null
    private var password: String? = null

    // UI elements
    private var tvForgotPassword: TextView? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnCreateAccount: Button? = null
    private var mProgressBar: ProgressDialog? = null

    // Firebase references
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initialise()
        val user = mAuth!!.currentUser
        Log.d(tag, "[onCreate] ${user.toString()}")
    }

    // Initialises variables, UI elements, buttons, and Firebase authentication
    private fun initialise() {
        tvForgotPassword = findViewById<View>(R.id.tv_forgot_password) as TextView
        etEmail = findViewById<View>(R.id.et_email) as EditText
        etPassword = findViewById<View>(R.id.et_password) as EditText
        btnLogin = findViewById<View>(R.id.btn_login) as Button
        btnCreateAccount = findViewById<View>(R.id.btn_register_account) as Button
        mProgressBar = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()

        // On click, user is redirected to ForgottenPasswordActivity
        tvForgotPassword!!.setOnClickListener { startActivity(Intent(this, ForgottenPasswordActivity::class.java)) }
        // On click, user is redirected to CreateAccountActivity
        btnCreateAccount!!.setOnClickListener { startActivity(Intent(this, CreateAccountActivity::class.java)) }
        // On click, user is attempting to login, calling loginUser()
        btnLogin!!.setOnClickListener { loginUser() }
    }

    // Called on click of the 'Login' button, this signs a user in to give them access to their wallet and gold banked details
    private fun loginUser() {
        // Sets information as what user input in-app
        email = etEmail?.text.toString()
        password = etPassword?.text.toString()

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mProgressBar!!.setMessage("Logging in...")
            mProgressBar!!.show()
            Log.d(tag, "Logging in user.")
            mAuth!!.signInWithEmailAndPassword(email!!, password!!).addOnCompleteListener(this) { task -> // Logs in user with Firebase authentication
                mProgressBar!!.hide()
                if (task.isSuccessful) {
                    Log.d(tag, "[loginUser] success")
                    updateUI()
                } else { // Sign in failed; most likely due to an incorrect password or a user not existing
                    Log.e(tag, "[loginUser] failure", task.exception)
                    Snackbar.make(main_layout, "Authentication failed.", Snackbar.LENGTH_SHORT).show()
                }
            }
        } else { // One or more fields left empty; must be filled out
            Snackbar.make(main_layout, "Enter all details.", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Upon logging in, user is redirected to game (specifically the MainActivity map)
    private fun updateUI() {
        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }



}