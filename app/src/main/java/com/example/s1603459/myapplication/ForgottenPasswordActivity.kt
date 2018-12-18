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

class ForgottenPasswordActivity : AppCompatActivity() {

    private val tag = "ForgotPasswordActivity"
    //UI elements
    private var etEmail: EditText? = null
    private var btnSubmit: Button? = null
    //Firebase references
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgotten_password)
        initialise()
    }
    private fun initialise() {
        etEmail = findViewById<View>(R.id.et_email) as EditText
        btnSubmit = findViewById<View>(R.id.btn_submit) as Button
        mAuth = FirebaseAuth.getInstance()
        btnSubmit!!.setOnClickListener { sendPasswordResetEmail() }
    }

    private fun sendPasswordResetEmail() {
        val email = etEmail?.text.toString()
        if (!TextUtils.isEmpty(email)) {
            mAuth!!.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(coordinatorLayout_forgot_password, "Password reset email sent.", Snackbar.LENGTH_SHORT).show()
                            updateToLoginUI()
                        } else {
                            Log.w(tag, task.exception!!.message)
                            Snackbar.make(coordinatorLayout_forgot_password, "No user found with this email.", Snackbar.LENGTH_SHORT).show()
                        }
                    }
        } else {
           Snackbar.make(coordinatorLayout_forgot_password, "Enter Email.", Snackbar.LENGTH_SHORT).show()
        }
    }
    private fun updateToLoginUI() {
        val intent = Intent(this@ForgottenPasswordActivity, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        finish()
        startActivity(intent)
    }


}
