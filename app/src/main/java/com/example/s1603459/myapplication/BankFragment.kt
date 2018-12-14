package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.SimpleDateFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BankFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [BankFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class BankFragment : Fragment(), AdapterView.OnItemSelectedListener {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val COLLECTION_KEY = "Users"
    private val SUB_COLLECTION_KEY = "Wallet"
    private val ID_FIELD = "id"
    private val VALUE_FIELD = "value"
    private val CURRENCY_FIELD = "currency"
    private val DATE_FIELD = "dateCollected"
    private val IS_BANKED_FIELD = "banked"
    private val COLLECTED_BY_USER_FIELD = "collectedByUser"
    private val DATE_BANKED_FIELD = "dateBanked"
    private val TRANSFER_FIELD = "transferred"

    private var todaysDate = "" // YYYY/MM/DD
//    private val tag = "BankActivity"

    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null

    private var btnBank: Button? = null
    private var btnTransfer: Button? = null
    private var walletOfCoins: ArrayList<Coin> = ArrayList()

    private var coinsBankedTodayUserCollected: Int = 0

    private lateinit var email: String
    private lateinit var spinner: Spinner

    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreWallet: CollectionReference? = null
    private var firestoreExchangeRates: DocumentReference? = null
    private var firestoreBanked: CollectionReference? = null
    private var firestoreUsers: CollectionReference? = null

//    @SuppressLint("SimpleDateFormat")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        todaysDate = SimpleDateFormat("YYYY/MM/dd").format(Date())
        initialise()
    }

    //    @SuppressLint("SetTextI18n")
    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
//        name = user!!.displayName!!
        email = user!!.email!!
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreWallet = firestore?.collection(COLLECTION_KEY)?.document(email)?.collection(SUB_COLLECTION_KEY)
        firestoreBanked = firestore?.collection(COLLECTION_KEY)?.document(email)?.collection("Banked")
        firestoreUsers = firestore?.collection(COLLECTION_KEY)

        firestoreExchangeRates = firestore?.collection("Exchange Rates")?.document("Today's Exchange Rate")
        Log.d("BANKFRAGMENT", "bankFragment $view?")
        shilText = view?.findViewById<View>(R.id.SHILText) as TextView
        dolrText = view?.findViewById<View>(R.id.DOLRText) as TextView
        quidText = view?.findViewById<View>(R.id.QUIDText) as TextView
        penyText = view?.findViewById<View>(R.id.PENYText) as TextView
        getExchangeRate("SHIL")
        getExchangeRate("DOLR")
        getExchangeRate("QUID")
        getExchangeRate("PENY")
        spinner = view!!.findViewById(R.id.walletSpinner)
        spinner.onItemSelectedListener = this
        getCoinIds()


        btnTransfer = view?.findViewById<View>(R.id.transferBtn) as Button
        btnTransfer!!.setOnClickListener{

            firestoreWallet!!.get().addOnSuccessListener { firebaseWallet ->
                coinsBankedTodayUserCollected = 0
                val coinSelected = spinner.selectedItem as Coin
                for (coin in firebaseWallet) {
                    if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                        coinsBankedTodayUserCollected++
                    }
                }
                when {
                    coinSelected.collectedByUser == "false" -> Toast.makeText(this.context, "You cannot transfer this coin, it's been sent to you!", Toast.LENGTH_SHORT).show()
                    coinsBankedTodayUserCollected < 25 -> Toast.makeText(this.context, "You can't send spare change until you bank 25 coins today!", Toast.LENGTH_SHORT).show()
                    else -> {
                        val builder = AlertDialog.Builder(this.context)

                        builder.setTitle("Transfer Coin")
                        builder.setMessage("Enter email of user you wish to transfer your coin to.")

                        // Set an EditText view to get user input
                        val input = EditText(this.context)
                        builder.setView(input)

                        builder.setPositiveButton("OK") { _, _ ->
                            doesUserExist(input.text.toString())
                            Log.d(tag, "[btnTransfer] setPositiveButton email ${input.text}")
                        }

                        builder.setNegativeButton("Cancel") { _, _ ->
                            // Canceled.
                        }

                        builder.show()
                    }
                }

            }

        }



        btnBank = view?.findViewById<View>(R.id.bankBtn) as Button
        btnBank!!.setOnClickListener{
            // Initialize a new instance of
            val builder = AlertDialog.Builder(this.context)
            // Set the alert dialog title
            builder.setTitle("Convert to Gold")
            // Display a message on alert dialog
            builder.setMessage("Are you want to bank this coin?")
            // Set a positive button and its click listener on alert dialog
            builder.setPositiveButton("Yes"){_, _ ->
                // Do something when user press the positive button
                fetchCoin()
                Log.d(tag, "[initialise] btnBank")

            }
            // Display a negative button on alert dialog
            builder.setNegativeButton("No"){ _, _ ->
            }

            // Display a neutral button on alert dialog
            builder.setNeutralButton("Transfer"){_,_ ->
                btnTransfer!!.callOnClick()
            }

            // Finally, make the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Display the alert dialog on app interface
            dialog.show()
        }



    }


    private fun getExchangeRate(currency: String){
        var exchangeRate: Double
        firestoreExchangeRates!!.get().addOnSuccessListener { rates ->
            Log.d(tag, "Exchange rate for $currency is ${rates.get(currency)}")
            exchangeRate = rates.get(currency) as Double
            setRate(currency, exchangeRate)
        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }

    private fun setRate(currency: String, exchangeRate: Double){
        Log.d(tag, "[setRate] $exchangeRate")
        when(currency) {
            "SHIL" -> shilText!!.text = "1 SHIL = $exchangeRate GOLD"
            "DOLR" -> dolrText!!.text = "1 DOLR = $exchangeRate GOLD"
            "QUID" -> quidText!!.text = "1 QUID = $exchangeRate GOLD"
            "PENY" -> penyText!!.text = "1 PENY = $exchangeRate GOLD"
        }
    }

    private fun getCoinIds() {
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            for (coin in firebaseWallet) {
                val id = coin.id
                val banked = coin.data[IS_BANKED_FIELD].toString().replace("\"", "")
                val collectedByUser = coin.data[COLLECTED_BY_USER_FIELD].toString().replace("\"", "")
                val currency = coin.data[CURRENCY_FIELD].toString().replace("\"", "")
                val date = coin.data[DATE_FIELD].toString().replace("\"", "")
                val value = coin.data[VALUE_FIELD].toString().replace("\"", "")
                val transferred = coin.data[TRANSFER_FIELD].toString().replace("\"", "")
                val newCoin = Coin(id, banked, collectedByUser, currency, date, value, transferred)
                if (banked == "false" && ((transferred == "true" && collectedByUser == "false") || (transferred == "false" && collectedByUser == "true"))){
                    Log.d(tag, "[getCoinIds] inside if statement")
                    walletOfCoins.add(newCoin)
                }
            }
            Log.d(tag, "[getCoinIds] Wallet is $walletOfCoins")

            addCoinsToSpinner(walletOfCoins)
        }
    }

    private fun addCoinsToSpinner(wallet: ArrayList<Coin>) {
        if (wallet.isEmpty()) {
            Log.d(tag, "[addCoinsToSpinner] Wallet is empty")
            val emptyWalletList = ArrayList<String>()
            emptyWalletList.add("Wallet is empty!")
            val adapter = ArrayAdapter(this.context, android.R.layout.simple_spinner_item, emptyWalletList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        } else {
            Log.d(tag, "[addCoinsToSpinner] Wallet is $wallet")
            val adapter = ArrayAdapter(this.context, android.R.layout.simple_spinner_item, wallet)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

    }

    private fun fetchCoin() {
        Log.d(tag, "Selected ${spinner.selectedItem}")
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            coinsBankedTodayUserCollected = 0
            val coinSelected = spinner.selectedItem as Coin
            for (coin in firebaseWallet) {
                if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                    coinsBankedTodayUserCollected++
                }
            }

            Log.d(tag, "coins banked today $coinsBankedTodayUserCollected")

            for (coin in firebaseWallet) {
                if (coinSelected.id == coin.id) {
                    if (coinsBankedTodayUserCollected >= 25 && coinSelected.collectedByUser == "true") {
                        Toast.makeText(this.context, "You have already banked 25 of your own coins today!", Toast.LENGTH_SHORT).show()
                        break
                    } else {
                        bankCoin(coinSelected)
                        break
                    }
                }
            }
        }
    }

    private fun doesUserExist(input: String) {
        Log.d(tag, "[doesUserExist] $input")
        firestoreUsers?.get()?.addOnSuccessListener { firestoreUsers ->
            var userExists = false
            for (user in firestoreUsers) {
                if (user.data["email"] == input) {
                    Log.d(tag, "[doesUserExist] ${user.data["email"] == input}")
                    userExists = true
                }
            }
            if (userExists) {
                if (input == email) {
                    Toast.makeText(this.context, "You can't send yourself coinz!", Toast.LENGTH_SHORT).show()
                } else {
                    transferTo(input)
                }
            } else {
                Toast.makeText(this.context, "User $input doesn't exist!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun transferTo(friendEmail: String) {
        Log.d("TRANSFERTO", "entering method")
        firestoreWallet?.get()?.addOnSuccessListener {
            Log.d("TRANSFERTO", "onsuccess listener")
            val coinSelected = spinner.selectedItem as Coin
            coinSelected.collectedByUser = "false"
            coinSelected.transferred = "true"
            Log.d(tag, "Coin is $coinSelected")

            updateFriendWallet(friendEmail)

            coinSelected.collectedByUser = "true"

            firestoreWallet?.document(coinSelected.id)?.set(coinSelected)?.addOnSuccessListener {
                Log.d(tag, "[transferTo] Coin removed from user's display wallet")
                Toast.makeText(this.context, "Coin sent to $friendEmail!", Toast.LENGTH_SHORT).show()
//                startActivity(Intent(this, BankActivity::class.java)) TODO remove coin from spinner right away!
            }?.addOnFailureListener{
                Log.d("TRANSFERTO", "[transferTo] Error removing coin")
            }

        }
    }

    private fun updateFriendWallet(email: String) {
        Log.d("UPDATE FRIEND WALLET", "update $email's wallet with coin")
        firestoreUsers!!.document(email).collection(SUB_COLLECTION_KEY).get().addOnSuccessListener { firebaseWallet ->
            val coinSelected = spinner.selectedItem as Coin
            for (coin in firebaseWallet) {
                if (coin.id == coinSelected.id) {
                    coinSelected.id = "${coinSelected.id}0"
                }
            }
            firestoreUsers!!.document(email).collection(SUB_COLLECTION_KEY).document(coinSelected.id).set(coinSelected)
        }.addOnFailureListener{
            Log.d(tag, "[transferTo] Error adding coin to $email wallet")
        }

    }


    private fun bankCoin(coin: Coin) {
        firestoreExchangeRates!!.get().addOnSuccessListener { rates ->
            //            Log.d(tag, "Exchange rate for $currency is ${rates.get(currency)}")
            val exchangeRate = rates.get(coin.currency) as Double
            val gold = coin.value.toDouble() * exchangeRate
            val bankedCoin = mapOf(
                    ID_FIELD to coin.id,
                    "GOLD" to gold,
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true",
                    COLLECTED_BY_USER_FIELD to coin.collectedByUser

            )
            Log.d(tag, "[bankCoin] path ${firestoreBanked?.document(coin.id)?.path}")
            firestoreBanked?.document(coin.id)?.update(bankedCoin)?.addOnSuccessListener {
                Toast.makeText(this.context, "Coin banked", Toast.LENGTH_SHORT).show()

            }?.addOnFailureListener{
                Log.d(tag, "Failed to bank coin")
                Toast.makeText(this.context, "ERROR BANKING", Toast.LENGTH_SHORT).show()
            }

            val modifiedCoin = mapOf(
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true"
            )

            firestoreWallet?.document(coin.id)?.update(modifiedCoin)
//            startActivity(Intent(this, BankActivity::class.java))             TODO remove coin from wallet right away!

        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }


//    private fun displayCoinInformation(coin: Coin) {
//        val currency = coin.currency
//        val value = coin.value
//        val id = coin.id
//        val coinInfo = "Currency: $currency\nValue: $value\nID: $id"
//        Toast.makeText(this, coinInfo, Toast.LENGTH_LONG).show()
//    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(this.context, "Please select a coin to bank or transfer.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //displayCoinInformation(coin)
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
        return inflater.inflate(R.layout.fragment_bank, container, false)
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
         * @return A new instance of fragment BankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                BankFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
