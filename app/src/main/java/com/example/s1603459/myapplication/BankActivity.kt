package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_bank.*


// This class is used to handle coin banking and the transferring of spare change to/from users of the app.

class BankActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val tag = "BankActivity"

    // UI elements
    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null
    private var tvBackToMap: TextView? = null
    private var btnBank: Button? = null
    private var btnTransfer: Button? = null
    private var walletOfCoins: ArrayList<Coin> = ArrayList()
    private lateinit var spinner: Spinner

    // Used to store coins banked or transferred in Firebase
    private var todaysDate = ""

    // Used to check if user has reached 25 coin banking limit
    private var coinsBankedTodayUserCollected: Int = 0

    // Current user's email
    private lateinit var email: String

    // Firebase references
    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null // Current user
    private var firestore: FirebaseFirestore? = null // Firestore used to read to/write from database
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreWallet: CollectionReference? = null // Reference to where in database coins in wallet are to be stored
    private var firestoreExchangeRates: DocumentReference? = null // Reference to where in database today's exchange rates are to be stored
    private var firestoreBanked: CollectionReference? = null // Reference to where in database bankec coins are to be stored
    private var firestoreUsers: CollectionReference? = null // Reference to list of all accounts on Coinz app

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        todaysDate = SimpleDateFormat("YYYY/MM/dd").format(Date()) // Get today's date in the given format
        initialise()
    }

    // Initialises variables, UI elements, buttons, Firebase references and the wallet spinner
    private fun initialise() {
        mDatabase = FirebaseDatabase.getInstance()
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        email = user!!.email!!
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        firestore?.firestoreSettings = settings
        firestoreWallet = firestore?.collection(COLLECTION_KEY)?.document(email)?.collection(SUB_COLLECTION_KEY)
        firestoreBanked = firestore?.collection(COLLECTION_KEY)?.document(email)?.collection("Banked")
        firestoreUsers = firestore?.collection(COLLECTION_KEY)

        firestoreExchangeRates = firestore?.collection("Exchange Rates")?.document("Today's Exchange Rate")
        shilText = findViewById<View>(R.id.SHILText) as TextView
        dolrText = findViewById<View>(R.id.DOLRText) as TextView
        quidText = findViewById<View>(R.id.QUIDText) as TextView
        penyText = findViewById<View>(R.id.PENYText) as TextView

        // Getting & storing exchange rates in app
        getExchangeRate("SHIL")
        getExchangeRate("DOLR")
        getExchangeRate("QUID")
        getExchangeRate("PENY")

        spinner = findViewById<View>(R.id.walletSpinner) as Spinner
        spinner.onItemSelectedListener = this
        getCoinIds() // Get coins in wallet on Firebase and add them to spinner

        btnTransfer = findViewById<View>(R.id.transferBtn) as Button
        btnTransfer!!.setOnClickListener{
            if (!connected()) { // If there's no internet connection, restart activity on click of Retry button
                Log.d(tag, "[btnTransfer] !connected()")
                Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {
                            finish()
                            startActivity(Intent(this, BankActivity::class.java))
                        }.show()
            } else if (spinner.selectedItem.toString() == "Wallet is empty!") { // Doesn't let user transfer with empty wallet
                Snackbar.make(coordinatorLayout_bank, "Walk around to collect more coins; alternatively, get a friend to transfer you their spare change!", Snackbar.LENGTH_LONG).show()
            } else {
                firestoreWallet!!.get().addOnSuccessListener { firebaseWallet ->
                    coinsBankedTodayUserCollected = 0
                    val coinSelected = spinner.selectedItem as Coin
                    // for-loop counts how many coins have been banked by the user today
                    for (coin in firebaseWallet) {
                        if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                            coinsBankedTodayUserCollected++
                        }
                    }
                    when {
                        // Received coins are non-transferable
                        coinSelected.collectedByUser == "false" -> Snackbar.make(coordinatorLayout_bank, "You cannot transfer this coin, it's been sent to you!", Snackbar.LENGTH_SHORT).show()
                        // No spare change until user banks 25 coins
                        coinsBankedTodayUserCollected < 25 -> Snackbar.make(coordinatorLayout_bank, "You can't send spare change until you bank 25 coins today!", Snackbar.LENGTH_SHORT).show()
                        else -> {
                            // Transfer coin to wallet of user with email input in AlertDialog EditText
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Transfer Coin")
                            builder.setMessage("Enter email of user you wish to transfer your coin to.")
                            val input = EditText(this)
                            builder.setView(input)
                            builder.setPositiveButton("OK") { _, _ ->
                                doesUserExist(input.text.toString()) // Checks if user with email input exists
                                Log.d(tag, "[btnTransfer] setPositiveButton email ${input.text}")
                            }
                            builder.setNegativeButton("Cancel") { _, _ -> }
                            builder.show()
                        }
                    }

                }

            }
        }


        btnBank = findViewById<View>(R.id.bankBtn) as Button
        btnBank!!.setOnClickListener{
            if (!connected()) { // If there's no internet connection, restart activity on click of Retry button
                Log.d(tag, "[btnBank] !connected()")
                Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {
                            finish()
                            startActivity(Intent(this, BankActivity::class.java))
                        }.show()
            } else if (spinner.selectedItem.toString() == "Wallet is empty!") { // Doesn't let user bank with empty wallet
                Snackbar.make(coordinatorLayout_bank, "Walk around to collect more coins; alternatively, get a friend to transfer you their spare change!", Snackbar.LENGTH_LONG).show()
            } else {
                // AlertDialog allows user to confirm whether or not they would like to bank coin
                val builder = AlertDialog.Builder(this@BankActivity)
                builder.setTitle("Convert to Gold")
                builder.setMessage("Are you sure you want to bank this coin?")
                builder.setPositiveButton("Yes"){_, _ ->
                    fetchCoin() // Fetch coin to be banked from Firebase wallet
                }
                builder.setNegativeButton("No"){ _, _ -> }
                builder.setNeutralButton("Transfer"){_,_ ->
                    btnTransfer!!.callOnClick()
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }

        // Returns to map on click
        tvBackToMap = findViewById<View>(R.id.tv_back_to_map) as TextView
        tvBackToMap!!.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }

    }

    // Gets exchange rates for use in BankActivity (which have been stored in Firebase in MainActivity)
    private fun getExchangeRate(currency: String){
        var exchangeRate: Double
        firestoreExchangeRates!!.get().addOnSuccessListener { rates ->
            Log.d(tag, "Exchange rate for $currency is ${rates.get(currency)}")
            exchangeRate = rates.get(currency) as Double
            setRate(currency, exchangeRate) // Exchange rate for each currency set in-app
        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }

    // After getting the exchange rates from Firebase, this sets them in the app for the user to see
    private fun setRate(currency: String, exchangeRate: Double){
        Log.d(tag, "[setRate] $exchangeRate")
        when(currency) {
            "SHIL" -> shilText!!.text = "1 SHIL = $exchangeRate GOLD"
            "DOLR" -> dolrText!!.text = "1 DOLR = $exchangeRate GOLD"
            "QUID" -> quidText!!.text = "1 QUID = $exchangeRate GOLD"
            "PENY" -> penyText!!.text = "1 PENY = $exchangeRate GOLD"
        }
    }

    // Retrieves the IDs of all the coins in the current user's Firebase wallet
    private fun getCoinIds() {
        if (!connected()) { // If there's no internet connection, restart activity on click of Retry button
            Log.d(tag, "[getCoinIds] !connected()")
            Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        finish()
                        startActivity(Intent(this, BankActivity::class.java))
                    }.show()
        } else {
            // Otherwise get all the IDs
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
                    if (banked == "false" && ((transferred == "true" && collectedByUser == "false") || (transferred == "false" && collectedByUser == "true"))) {
                        Log.d(tag, "[getCoinIds] inside if statement")
                        walletOfCoins.add(newCoin) // Add coins that haven't been banked to the list of coins in the UI wallet
                    }
                }
                Log.d(tag, "[getCoinIds] Wallet is $walletOfCoins")

                addCoinsToSpinner(walletOfCoins) // Adds coins to spinner for user to interact with
            }
        }
    }

    // Adds all "appropriate" coins (i.e. in current user's wallet that haven't been banked) to the spinner in the UI
    private fun addCoinsToSpinner(wallet: ArrayList<Coin>) {
        if (spinner.adapter is ArrayAdapter<*>) {
            (spinner.adapter as ArrayAdapter<*>).clear()
        }
        if (wallet.isEmpty()) { // If wallet is empty, display message in the spinner so the user knows.
            Log.d(tag, "[addCoinsToSpinner] Wallet is empty")
            val emptyWalletList = ArrayList<String>()
            emptyWalletList.add("Wallet is empty!")
            val adapter = ArrayAdapter(this, R.layout.spinner_layout, emptyWalletList)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout)
            spinner.adapter = adapter
        } else {
            // If wallet is not empty, add all "appropriate" coins
            Log.d(tag, "[addCoinsToSpinner] Wallet is $wallet")
            // adapter takes ArrayList<> and converts it to spinner entries
            val adapter = ArrayAdapter(this, R.layout.spinner_layout, wallet.clone() as ArrayList<*>)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout)
                spinner.adapter = adapter
        }
    }

    // Called on click of BANK button, this fetches the coin selected on the spinner from Firebase
    private fun fetchCoin() {
        Log.d(tag, "Selected ${spinner.selectedItem}")
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            coinsBankedTodayUserCollected = 0
            val coinSelected = spinner.selectedItem as Coin
            // Used to count how many of the user's own coins they have themselves collected have been banked today
            for (coin in firebaseWallet) {
                if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                    coinsBankedTodayUserCollected++
                }
            }
            Log.d(tag, "Coins banked today $coinsBankedTodayUserCollected")
            for (coin in firebaseWallet) {
                if (coinSelected.id == coin.id) {
                    if (coinsBankedTodayUserCollected >= 25 && coinSelected.collectedByUser == "true") { // User cannot bank more than 25 of their own coins per day
                        Snackbar.make(coordinatorLayout_bank, "You have already banked 25 of your own coins today!", Snackbar.LENGTH_SHORT).show()
                        break
                    } else {
                        bankCoin(coinSelected) // Conditions from specification met: this coin is bankable, therefore we proceed with banking it in Firebase
                        break
                    }
                }
            }
        }
    }

    // Checks if a particular user of Coinz with email 'input' exists (i.e. an account with this email exists in Firebase)
    private fun doesUserExist(input: String) {
        firestoreUsers?.get()?.addOnSuccessListener { firestoreUsers ->
            var userExists = false
            // Iterate through all the users to find an email that matches the user's input
            for (user in firestoreUsers) {
                if (user.data["email"] == input) {
                    Log.d(tag, "[doesUserExist] ${user.data["email"] == input}")
                    userExists = true
                }
            }
            if (userExists) {
                if (input == email) { // User cannot send themselves spare change
                    Snackbar.make(coordinatorLayout_bank, "You can't send yourself spare change!", Snackbar.LENGTH_SHORT).show()
                } else {
                    transferTo(input) // Other user exists, proceed with transferring coin
                }
            } else {
                Snackbar.make(coordinatorLayout_bank, "User $input doesn't exist!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Adds coin to friend's Firebase wallet from current user; removes it from current user's wallet and adds it to friend's
    private fun transferTo(friendEmail: String) {
        firestoreWallet?.get()?.addOnSuccessListener {firebaseWallet ->
            val coinSelected = spinner.selectedItem as Coin
                coinSelected.collectedByUser = "false"
                coinSelected.transferred = "true"
                Log.d(tag, "Coin is $coinSelected")
            val id = coinSelected.id
            coinSelected.id = coinSelected.id + '0' // ID of coin is changed so as not to overwrite the coin (yet to be) collected by the friend

            // This loop changes the ID of the coin by adding more zeros at the end of it
            // The purpose of this is that it allows multiple users to send the same coin to one user, and therefore this one user can bank the coin multiple times
            for (Coin in firebaseWallet) {
                if (coinSelected.id == Coin.id) {
                    coinSelected.id = coinSelected.id + '0'
                }
            }
            // Adds coin to friend's wallet
            firestoreUsers!!.document(friendEmail).collection(SUB_COLLECTION_KEY).document(coinSelected.id).set(coinSelected).addOnSuccessListener {
                Log.d(tag, "[transferTo] Coin ${coinSelected.id} added to $friendEmail wallet")
            }.addOnFailureListener{
                Log.d(tag, "[transferTo] Error adding coin to $friendEmail wallet")
            }

            // Reset selected coin's ID to original ID to be able to modify its fields in Firebase
            coinSelected.id = id
            coinSelected.collectedByUser = "true"

            // Modify current user's wallet
            firestoreWallet?.document(coinSelected.id)?.set(coinSelected)?.addOnSuccessListener {
                Log.d(tag, "[transferTo] Coin removed from user's display wallet")
                Snackbar.make(coordinatorLayout_bank, "Coin sent to $friendEmail!", Snackbar.LENGTH_SHORT).show()
                finish()
                startActivity(Intent(this, BankActivity::class.java))
            }?.addOnFailureListener{
                Log.d(tag, "[transferTo] Error removing coin")
            }
        }
    }

    // Converts selected coin to gold as per today's exchange rate, and stores it into the user's bank account
    private fun bankCoin(coin: Coin) {
        firestoreExchangeRates!!.get().addOnSuccessListener { rates ->
            val exchangeRate = rates.get(coin.currency) as Double // Get exchange rate of currency of selected coin
            val gold = coin.value.toDouble() * exchangeRate // Convert coin into its value in gold
            val bankedCoin = mapOf(
                    ID_FIELD to coin.id,
                    "GOLD" to gold,
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true",
                    COLLECTED_BY_USER_FIELD to coin.collectedByUser
            )

            // Add coin to bank, its value in gold, in Firebase
            firestoreBanked?.document(coin.id)?.set(bankedCoin)?.addOnSuccessListener {
                Snackbar.make(coordinatorLayout_bank, "Coin banked!", Snackbar.LENGTH_SHORT).show()
            }?.addOnFailureListener{
                Log.d(tag, "Failed to bank coin")
            }

            val modifiedCoin = mapOf(
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true"
            )

            // Modify coin in user's Firebase wallet saying that it's been banked, so they aren't able to bank this one again
            firestoreWallet?.document(coin.id)?.update(modifiedCoin)
            finish()
            startActivity(Intent(this, BankActivity::class.java))
        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }

    // ArrayAdapter method implemented
    override fun onNothingSelected(parent: AdapterView<*>?) {
        Snackbar.make(coordinatorLayout_bank, "Please select a coin to bank or transfer.", Snackbar.LENGTH_SHORT).show()
    }

    // ArrayAdapter method implemented
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(tag, "Selected ${spinner.selectedItem}")
    }

    // Method returning Boolean that checks if device is connected to the internet
    private fun connected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }

    // Companion object storing keys/fields for ease of editing source code and therefore ease of renaming/editing how Firebase database is maintained
    companion object {
        private const val COLLECTION_KEY = "Users"
        private const val SUB_COLLECTION_KEY = "Wallet"
        private const val ID_FIELD = "id"
        private const val VALUE_FIELD = "value"
        private const val CURRENCY_FIELD = "currency"
        private const val DATE_FIELD = "dateCollected"
        private const val IS_BANKED_FIELD = "banked"
        private const val COLLECTED_BY_USER_FIELD = "collectedByUser"
        private const val DATE_BANKED_FIELD = "dateBanked"
        private const val TRANSFER_FIELD = "transferred"
    }
}

