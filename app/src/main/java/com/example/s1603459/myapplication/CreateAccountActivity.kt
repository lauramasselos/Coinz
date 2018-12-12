@file:Suppress("DEPRECATION")

package com.example.s1603459.myapplication

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class CreateAccountActivity : AppCompatActivity() {


    private val tag = "CreateAccountActivity"

    //UI elements
    private var etFirstName: EditText? = null
    private var etLastName: EditText? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnCreateAccount: Button? = null
    private var mProgressBar: ProgressDialog? = null

    //Firebase references
    private lateinit var mDatabaseReference: DatabaseReference
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var firestoreUsers: DocumentReference? = null

    //global variables
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

        realtimeUpdateListener()
        initialise()
    }
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

    private fun createNewAccount() {
        firstName = etFirstName?.text.toString()
        lastName = etLastName?.text.toString()
        email = etEmail?.text.toString()
        password = etPassword?.text.toString()

        if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName) && !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mProgressBar!!.setMessage("Registering User...")
            mProgressBar!!.show()
            mAuth!!.createUserWithEmailAndPassword(email!!, password!!).addOnCompleteListener(this) { task ->
                mProgressBar!!.hide()
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(tag, "createUserWithEmail:success")
                    val user = mAuth!!.currentUser
                    val usersEmail = user!!.email
                    firestoreUsers = firestore?.collection(COLLECTION_KEY)?.document(usersEmail!!)
                    //Verify Email
                    verifyEmail()
                    updateUserInfoAndUI()

                    val name = "$firstName $lastName"
                    writeNewUser(user.uid, name, user.email)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(tag, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@CreateAccountActivity, "Account already exists!",
                            Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            Toast.makeText(this, "Enter all details", Toast.LENGTH_SHORT).show()
        }

    }

    private fun writeNewUser(userId: String, name: String?, email: String?) {
        val user = User(name, email)
        mDatabaseReference.child("users").child(userId).setValue(user)
        mDatabaseReference.child("users").child(userId).child("name").setValue(name)

        // create a message of the form { ”Name”: str1, ”Text”: str2 }
        val newUser = mapOf(
                NAME_FIELD to name.toString(),
                USER_ID_FIELD to userId,
                EMAIL_FIELD to email.toString())
// send the message and listen for success or failure
        firestoreUsers?.set(newUser)?.addOnSuccessListener{
            Log.d(tag, "User added to database")}
                ?.addOnFailureListener{
                    Log.d(tag, "Failed to add user")
                }
    }


    private fun realtimeUpdateListener() {
        firestoreUsers?.addSnapshotListener { documentSnapshot, e ->
            when {
                e != null -> Log.d(tag, "e != null")
                documentSnapshot != null && documentSnapshot.exists() -> {
                }
            }
        }
    }




//    private fun getUsernameFromEmail(email: String?) : String {
//        return if (email!!.contains("@")) {
//            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
//        } else {
//            email
//        }
//    }

    private fun verifyEmail() {
        val mUser = mAuth!!.currentUser
        mUser!!.sendEmailVerification().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this@CreateAccountActivity,
                        "Verification email sent to " + mUser.email,
                        Toast.LENGTH_SHORT).show()
            } else {
                Log.e(tag, "sendEmailVerification", task.exception)
                Toast.makeText(this@CreateAccountActivity,
                        "Failed to send verification email.",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUserInfoAndUI() {
        //start next activity
        val intent = Intent(this@CreateAccountActivity, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }





    companion object {
        private const val COLLECTION_KEY = "Users"
        private const val NAME_FIELD = "Name"
        private const val USER_ID_FIELD = "ID"
        private const val EMAIL_FIELD = "Email"
    }

}