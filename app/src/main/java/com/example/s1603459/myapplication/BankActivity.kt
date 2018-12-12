package com.example.s1603459.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.widget.EditText



class BankActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var todaysDate = "" // YYYY/MM/DD

    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null

    private var btnBank: Button? = null
    private var btnTransfer: Button? = null

    private var wallet: HashMap<String, String> = HashMap<String, String>()
    private var walletOfCoins: ArrayList<Coin> = ArrayList()
    private var walletCopy: HashMap<String, String> = HashMap<String, String>()

    private lateinit var name: String
    private lateinit var email: String
    private lateinit var spinner: Spinner
    private lateinit var coin: String
    private lateinit var uniqueCoinId: String

    private var coinsBankedTodayUserCollected: Int = 0

    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreWallet: CollectionReference? = null
    private var firestoreExchangeRates: DocumentReference? = null
    private var firestoreBanked: CollectionReference? = null
    private var firestoreUsers: CollectionReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        todaysDate = SimpleDateFormat("YYYY/MM/dd").format(Date())
        initialise()
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

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
        shilText = findViewById<View>(R.id.SHILText) as TextView
        dolrText = findViewById<View>(R.id.DOLRText) as TextView
        quidText = findViewById<View>(R.id.QUIDText) as TextView
        penyText = findViewById<View>(R.id.PENYText) as TextView
        getExchangeRate("SHIL")
        getExchangeRate("DOLR")
        getExchangeRate("QUID")
        getExchangeRate("PENY")
        spinner = findViewById(R.id.walletSpinner)
        spinner.onItemSelectedListener = this
        getCoinIds()


        btnTransfer = findViewById<View>(R.id.transferBtn) as Button
        btnTransfer!!.setOnClickListener{

            firestoreWallet!!.get().addOnSuccessListener { firebaseWallet ->
                coinsBankedTodayUserCollected = 0
                val coinSelected = spinner.selectedItem as Coin
                for (coin in firebaseWallet) {
                     if (coin.data.get(DATE_BANKED_FIELD) == todaysDate && coin.data.get(COLLECTED_BY_USER_FIELD) == "true" && coin.data.get(IS_BANKED_FIELD) == "true") {
                      coinsBankedTodayUserCollected++
                    }
                }
                if (coinSelected.collectedByUser == "false") {
                    Toast.makeText(this, "You cannot transfer this coin, it's been sent to you!", Toast.LENGTH_SHORT).show()
                } else if (coinsBankedTodayUserCollected < 25) {
                    Toast.makeText(this, "You can't send spare change until you bank 25 coins today!", Toast.LENGTH_SHORT).show()
                } else {
                    val builder = AlertDialog.Builder(this)

                    builder.setTitle("Transfer Coin")
                    builder.setMessage("Enter email of user you wish to transfer your coin to.")

                    // Set an EditText view to get user input
                    val input = EditText(this)
                    builder.setView(input)

                    builder.setPositiveButton("OK") { dialog, which ->
                        doesUserExist(input.text.toString())
                        Log.d(tag, "[btnTransfer] setPositiveButton email ${input.text}")
                    }

                    builder.setNegativeButton("Cancel") { dialog, which ->
                        // Canceled.
                    }

                    builder.show()
                }

            }

        }



        btnBank = findViewById<View>(R.id.bankBtn) as Button
        btnBank!!.setOnClickListener{
            // Initialize a new instance of
            val builder = AlertDialog.Builder(this@BankActivity)
            // Set the alert dialog title
            builder.setTitle("Convert to Gold")
            // Display a message on alert dialog
            builder.setMessage("Are you want to bank this coin?")
            // Set a positive button and its click listener on alert dialog
            builder.setPositiveButton("Yes"){dialog, which ->
                // Do something when user press the positive button
                fetchCoin()
                Log.d(tag, "[initialise] btnBank")

            }
            // Display a negative button on alert dialog
            builder.setNegativeButton("No"){dialog,which ->
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
        var exchangeRate = 0.0
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
                    val banked = coin.data.get(IS_BANKED_FIELD).toString().replace("\"", "")
                    val collectedByUser = coin.data.get(COLLECTED_BY_USER_FIELD).toString().replace("\"", "")
                    val currency = coin.data.get(CURRENCY_FIELD).toString().replace("\"", "")
                    val date = coin.data.get(DATE_FIELD).toString().replace("\"", "")
                    val value = coin.data.get(VALUE_FIELD).toString().replace("\"", "")
                    val transferred = coin.data.get(TRANSFER_FIELD).toString().replace("\"", "")
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
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, emptyWalletList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            } else {
                Log.d(tag, "[addCoinsToSpinner] Wallet is $wallet")
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, wallet)
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
                if (coin.data.get(DATE_BANKED_FIELD) == todaysDate && coin.data.get(COLLECTED_BY_USER_FIELD) == "true" && coin.data.get(IS_BANKED_FIELD) == "true") {
                    coinsBankedTodayUserCollected++
                }
            }

            Log.d(tag, "coins banked today $coinsBankedTodayUserCollected")

            for (coin in firebaseWallet) {
                if (coinSelected.id == coin.id) {
                    if (coinsBankedTodayUserCollected >= 25 && coinSelected.collectedByUser == "true") {
                        Toast.makeText(this, "You have already banked 25 of your own coins today!", Toast.LENGTH_SHORT).show()
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
                if (user.data.get("Email") == input) {
                    Log.d(tag, "[doesUserExist] ${user.data.get("Email") == input}")
                    userExists = true
                }
            }
            if (userExists) {
                if (input == email) {
                    Toast.makeText(this, "You can't send yourself coinz!", Toast.LENGTH_SHORT).show()
                } else {
                    transferTo(input)
                }
            } else {
                Toast.makeText(this, "User $input doesn't exist!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun transferTo(friendEmail: String) {
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->

                val coinSelected = spinner.selectedItem as Coin
                coinSelected.collectedByUser = "false"
                coinSelected.transferred = "true"
                Log.d(tag, "Coin is $coinSelected")


                firestoreUsers!!.document(friendEmail).collection(SUB_COLLECTION_KEY).document(coinSelected.id).set(coinSelected).addOnSuccessListener {
                    Log.d(tag, "[transferTo] Coin ${coinSelected.id} added to $friendEmail wallet")
                }.addOnFailureListener{
                    Log.d(tag, "[transferTo] Error adding coin to $friendEmail wallet")
                }

                coinSelected.collectedByUser = "true"

                firestoreWallet?.document(coinSelected.id)?.set(coinSelected)?.addOnSuccessListener {
                    Log.d(tag, "[transferTo] Coin removed from user's display wallet")
                    Toast.makeText(this, "Coin sent to $friendEmail!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, BankActivity::class.java))
                }?.addOnFailureListener{
                    Log.d(tag, "[transferTo] Error removing coin")
                }

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

            firestoreBanked?.document(coin.id)?.set(bankedCoin)?.addOnSuccessListener {
                Toast.makeText(this, "Coin banked", Toast.LENGTH_SHORT).show()

            }?.addOnFailureListener{
                Log.d(tag, "Failed to bank coin")
            }

            val modifiedCoin = mapOf(
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true"
            )

            firestoreWallet?.document(coin.id)?.update(modifiedCoin)
            startActivity(Intent(this, BankActivity::class.java))

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
        Toast.makeText(this, "Please select a coin to bank or transfer.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val coin = parent?.selectedItem as Coin
            //displayCoinInformation(coin)
    }



    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                startActivity(Intent(this@BankActivity, ProfileActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_map-> {
                startActivity(Intent(this@BankActivity, MainActivity::class.java))
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_bank -> {
                return@OnNavigationItemSelectedListener true
            }

        }
        false
    }

companion object {
    private const val tag = "BankActivity"
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

