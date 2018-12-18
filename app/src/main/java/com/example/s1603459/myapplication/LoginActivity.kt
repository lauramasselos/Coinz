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

class LoginActivity : AppCompatActivity() {

    private val tag = "LoginActivity"
    //global variables
    private var email: String? = null
    private var password: String? = null
    //UI elements
    private var tvForgotPassword: TextView? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnLogin: Button? = null
    private var btnCreateAccount: Button? = null
    private var mProgressBar: ProgressDialog? = null
    //Firebase references
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initialise()
        val user = mAuth!!.currentUser
        Log.d(tag, "[onCreate] ${user.toString()}")
        if (user != null) {
            mProgressBar!!.setMessage("Logging you in...")
            mProgressBar!!.show()
            finish()
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        }
    }
    private fun initialise() {
        tvForgotPassword = findViewById<View>(R.id.tv_forgot_password) as TextView
        etEmail = findViewById<View>(R.id.et_email) as EditText
        etPassword = findViewById<View>(R.id.et_password) as EditText
        btnLogin = findViewById<View>(R.id.btn_login) as Button
        btnCreateAccount = findViewById<View>(R.id.btn_register_account) as Button
        mProgressBar = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        tvForgotPassword!!
                .setOnClickListener { startActivity(Intent(this@LoginActivity,
                        ForgottenPasswordActivity::class.java)) }
        btnCreateAccount!!
                .setOnClickListener { startActivity(Intent(this@LoginActivity,
                        CreateAccountActivity::class.java)) }
        btnLogin!!.setOnClickListener { loginUser() }
    }

    private fun loginUser() {
        email = etEmail?.text.toString()
        password = etPassword?.text.toString()
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mProgressBar!!.setMessage("Logging in...")
            mProgressBar!!.show()
            Log.d(tag, "Logging in user.")
            mAuth!!.signInWithEmailAndPassword(email!!, password!!)
                    .addOnCompleteListener(this) { task ->
                        mProgressBar!!.hide()
                        if (task.isSuccessful) {
                            // Sign in success, update UI with signed-in user's information
                            Log.d(tag, "signInWithEmail:success")
                            updateUI()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.e(tag, "signInWithEmail:failure", task.exception)
                            Snackbar.make(main_layout, "Authentication failed.", Snackbar.LENGTH_SHORT).show()
                        }
                    }
        } else {
            Snackbar.make(main_layout, "Enter all details", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        finish()
        startActivity(intent)
    }



}