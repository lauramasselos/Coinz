package com.example.s1603459.myapplication

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.*
import android.view.View
import kotlinx.android.synthetic.main.activity_login.*
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.text.TextUtils

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    override fun onClick(v: View?) {
        val i = v!!.id

        if (i == R.id.btn_email_create_account) {
            createAccount(edtEmail.text.toString(), edtPassword.text.toString())
        } else if (i == R.id.btn_email_sign_in) {
            signIn(edtEmail.text.toString(), edtPassword.text.toString())
        } else if (i == R.id.btn_sign_out) {
            signOut()
        } else if (i == R.id.btn_verify_email) {
            sendEmailVerification()
        }
    }


    private val tag = "LoginActivity"
    private var mAuth: FirebaseAuth? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        updateUI(currentUser)
    }

    private fun createAccount(email: String, password: String) {
        Log.d(tag, "[createAccount]: $email")
        if (!isValidatedForm(email, password)) {
            return
        }

        mAuth!!.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {task ->
            if (task.isSuccessful) {
                Log.d(tag, "[createAccount] successful")
                val user = mAuth!!.currentUser
                updateUI(user)
            } else {
                Log.d(tag, "[createAccount] failed")
                Toast.makeText(applicationContext, "Registration failed!", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }
        }
    }

    private fun signIn(email: String, password: String) {
        Log.d(tag, "[signIn]: $email")
        if (!isValidatedForm(email, password)) {
            return
        }

        mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d(tag, "[signIn] successful")
                val user = mAuth!!.currentUser
                updateUI(user)
            } else {
                Log.d(tag, "[signIn] failed")
                Toast.makeText(applicationContext, "Login failed!", Toast.LENGTH_SHORT).show()
                updateUI(null)
            }

            if (!task.isSuccessful) {
                tvStatus.text = "Authentication failed!"
            }
        }
    }

    private fun signOut() {
        mAuth!!.signOut()
        updateUI(null)
    }

    private fun sendEmailVerification() {
        findViewById<View>(R.id.btn_verify_email).isEnabled = false

        val user = mAuth!!.currentUser
        user!!.sendEmailVerification().addOnCompleteListener(this) { task ->
            findViewById<View>(R.id.btn_verify_email).isEnabled = true

            if (task.isSuccessful) {
                Toast.makeText(applicationContext, "Verification email sent to " + user.email, Toast.LENGTH_SHORT).show()
            } else {
                Log.d(tag, "[sendEmailVerification] failed")
                Toast.makeText(applicationContext, "Failed sending verification email to " + user.email, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidatedForm(email: String, password: String): Boolean {

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(applicationContext, "Enter email address!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(applicationContext, "Enter password!", Toast.LENGTH_SHORT).show()
        }

        return true
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            tvStatus.text = "User Email: " + user.email + "(verified: " + user.isEmailVerified + ")"
            tvDetail.text = "Firebase User ID: " + user.uid

            email_password_buttons.visibility = View.GONE
            email_password_fields.visibility = View.GONE
            layout_signed_in_buttons.visibility = View.VISIBLE

            btn_verify_email.isEnabled = !user.isEmailVerified
        } else {
            tvStatus.text = "Signed Out"
            tvDetail.text = null

            email_password_buttons.visibility = View.VISIBLE
            email_password_fields.visibility = View.VISIBLE
            layout_signed_in_buttons.visibility = View.GONE
        }
    }

}
