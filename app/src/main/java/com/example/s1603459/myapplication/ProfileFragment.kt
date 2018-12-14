package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ProfileFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

//    private val tag = "ProfileActivity"
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreUsers: CollectionReference? = null
    private var firestoreBanked: CollectionReference? = null
    private var userName: TextView? = null
    private var usersEmail: TextView? = null
    private var usersGold: TextView? = null

    private lateinit var email: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialise()
    }

    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        email = user!!.email!!
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreUsers = firestore?.collection("Users")
        firestoreBanked = firestore?.collection("Users")?.document(email)?.collection("Banked")
        getNameEmailGold()
    }

    @SuppressLint("SetTextI18n")
    private fun getName() {
        firestoreUsers!!.document(email).get().addOnSuccessListener { userInfo ->
            val name = userInfo.get("name").toString()
            userName!!.text = "Welcome back, $name!"
        }
    }

    private fun getNameEmailGold() {
        firestoreBanked!!.get().addOnSuccessListener { firebaseGold ->
            userName = view?.findViewById<View>(R.id.userText) as TextView
            usersEmail = view?.findViewById<View>(R.id.userEmailText) as TextView
            usersGold = view?.findViewById<View>(R.id.goldText) as TextView
            usersEmail!!.text = email
            var goldTotal = 0.0
            for (coin in firebaseGold) {
                goldTotal += coin.data["GOLD"] as Double
            }
            Log.d(tag, "[getGold] Gold total $goldTotal")
            usersGold!!.text = "GOLD: $goldTotal"
            getName()
            setLevel(goldTotal)
        }
    }

    private fun setLevel(gold: Double) {
        val level = gold.toInt() / 50000
        val newLevel = mapOf("level" to level, "gold" to gold)
        firestoreUsers!!.document(email).update(newLevel).addOnCompleteListener {
            Log.d(tag, "User $email's level successfully updated to level $level with gold $gold")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onResume() {
        super.onResume()
        initialise()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                ProfileFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
