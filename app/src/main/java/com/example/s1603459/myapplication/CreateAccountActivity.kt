@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication

import android.app.ProgressDialog
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_create_account.*

// This class is used when creating a new user; information is stored both in Firebase authentication as well as in the Firebase Database.

class CreateAccountActivity : AppCompatActivity() {


    private val tag = "CreateAccountActivity"

    // UI elements
    private var etFirstName: EditText? = null
    private var etLastName: EditText? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnCreateAccount: Button? = null
    private var mProgressBar: ProgressDialog? = null

   // Firebase references
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null // Firestore used to read to/write from database
    private var firestoreUsers: DocumentReference? = null // Reference to list of all accounts on Coinz app
    private lateinit var mDatabaseReference: DatabaseReference

    // New user's information
    private var firstName: String? = null
    private var lastName: String? = null
    private var email: String? = null
    private var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        initialise()
    }

    // Initialises variables, UI elements, buttons, and Firebase references
    private fun initialise() {
        etFirstName = findViewById<View>(R.id.et_first_name) as EditText
        etLastName = findViewById<View>(R.id.et_last_name) as EditText
        etEmail = findViewById<View>(R.id.et_email) as EditText
        etPassword = findViewById<View>(R.id.et_password) as EditText
        btnCreateAccount = findViewById<View>(R.id.btn_register) as Button
        mProgressBar = ProgressDialog(this)
        mDatabase = FirebaseDatabase.getInstance()
        mDatabaseReference = mDatabase!!.reference.child("Users")
        mAuth = FirebaseAuth.getInstance()
        btnCreateAccount!!.setOnClickListener { createNewAccount() }
    }

    // Called on click of the 'Create Account' button, this registers a new user onto Firebase
    private fun createNewAccount() {
        // Sets user information as what was input in-app
        firstName = etFirstName?.text.toString()
        lastName = etLastName?.text.toString()
        email = etEmail?.text.toString()
        password = etPassword?.text.toString()

        if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mProgressBar!!.setMessage("Registering User...")
            mProgressBar!!.show()
            mAuth!!.createUserWithEmailAndPassword(email!!, password!!).addOnCompleteListener(this) { task -> // Creates user with Firebase authentication
                mProgressBar!!.hide()
                if (task.isSuccessful) { // Checks if user with email given already exists
                    Log.d(tag, "[createNewAccount] success!")
                    val user = mAuth!!.currentUser
                    val usersEmail = user!!.email
                    firestoreUsers = firestore?.collection("Users")?.document(usersEmail!!)
                    verifyEmail() // Sends verification email to new user
                    updateUserInfoAndUI()
                    val name = "$firstName $lastName"
                    writeNewUser(user.uid, name, user.email!!) // Writes new user to Firebase
                } else {
                    Log.w(tag, "[createNewAccount] failed.", task.exception)
                    Snackbar.make(coordinatorLayout_register, "Account already exists!", Snackbar.LENGTH_SHORT).show()
                }
            }

        } else { // One or more fields left empty; must be filled out
            Snackbar.make(coordinatorLayout_register, "Enter all details", Snackbar.LENGTH_SHORT).show()
        }

    }

    // Given all the information entered, this writes the new user to the Firebase database.
    private fun writeNewUser(userId: String, name: String, email: String) {
        val user = User(userId, name, email, 0) // New user starts at level 0 as default
        mDatabaseReference.child("users").child(userId).setValue(user)
        mDatabaseReference.child("users").child(userId).child("name").setValue(name)
        firestoreUsers?.set(user)?.addOnSuccessListener{
            Log.d(tag, "User added to database")
        }?.addOnFailureListener{
            Log.d(tag, "Failed to add user")
        }
    }

    // Sends verification email to new user
    private fun verifyEmail() {
        val mUser = mAuth!!.currentUser
        mUser!!.sendEmailVerification().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Snackbar.make(coordinatorLayout_register, "Verification email sent to ${mUser.email}", Snackbar.LENGTH_SHORT).show()
            } else {
                Log.e(tag, "sendEmailVerification", task.exception)
                Snackbar.make(coordinatorLayout_register, "Failed to send verification email.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Upon creating an account, user is redirected to the login screen
    private fun updateUserInfoAndUI() {
        finish()
        startActivity(Intent(this, LoginActivity::class.java))
    }

}