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


class BankActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var todaysDate = "" // YYYY/MM/DD
    private val tag = "BankActivity"
    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null
    private var tvBackToMap: TextView? = null
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

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        todaysDate = SimpleDateFormat("YYYY/MM/dd").format(Date())
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
        spinner = findViewById<View>(R.id.walletSpinner) as Spinner
        spinner.onItemSelectedListener = this
        getCoinIds()

        btnTransfer = findViewById<View>(R.id.transferBtn) as Button
        btnTransfer!!.setOnClickListener{
            Log.d(tag, "[btnTransfer] onClick")
            if (!connected()) {
                Log.d(tag, "[btnTransfer] !connected()")
                Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {
                            finish()
                            startActivity(Intent(this, BankActivity::class.java))
                        }.show()
            } else if (spinner.selectedItem.toString() == "Wallet is empty!") {
                Snackbar.make(coordinatorLayout_bank, "Walk around to collect more coins; alternatively, get a friend to transfer you their spare change!", Snackbar.LENGTH_LONG).show()
            } else {
                firestoreWallet!!.get().addOnSuccessListener { firebaseWallet ->
                    coinsBankedTodayUserCollected = 0
                    val coinSelected = spinner.selectedItem as Coin
                    for (coin in firebaseWallet) {
                        if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                            coinsBankedTodayUserCollected++
                        }
                    }
                    when {
                        coinSelected.collectedByUser == "false" -> Snackbar.make(coordinatorLayout_bank, "You cannot transfer this coin, it's been sent to you!", Snackbar.LENGTH_SHORT).show()
                        coinsBankedTodayUserCollected < 25 -> Snackbar.make(coordinatorLayout_bank, "You can't send spare change until you bank 25 coins today!", Snackbar.LENGTH_SHORT).show()
                        else -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("Transfer Coin")
                            builder.setMessage("Enter email of user you wish to transfer your coin to.")
                            val input = EditText(this)
                            builder.setView(input)

                            builder.setPositiveButton("OK") { _, _ ->
                                doesUserExist(input.text.toString())
                                Log.d(tag, "[btnTransfer] setPositiveButton email ${input.text}")
                            }
                            builder.setNegativeButton("Cancel") { _, _ ->
                            }

                            builder.show()
                        }
                    }

                }

            }
        }


        btnBank = findViewById<View>(R.id.bankBtn) as Button
        btnBank!!.setOnClickListener{
            if (!connected()) {
                Log.d(tag, "[btnBank] !connected()")
                Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry") {
                            finish()
                            startActivity(Intent(this, BankActivity::class.java))
                        }.show()
            } else if (spinner.selectedItem.toString() == "Wallet is empty!") {
                Snackbar.make(coordinatorLayout_bank, "Walk around to collect more coins; alternatively, get a friend to transfer you their spare change!", Snackbar.LENGTH_LONG).show()
            } else {
                val builder = AlertDialog.Builder(this@BankActivity)
                builder.setTitle("Convert to Gold")
                builder.setMessage("Are you sure you want to bank this coin?")
                builder.setPositiveButton("Yes"){_, _ ->
                    fetchCoin()
                    Log.d(tag, "[initialise] btnBank")
                }
                builder.setNegativeButton("No"){ _, _ ->
                }
                builder.setNeutralButton("Transfer"){_,_ ->
                    btnTransfer!!.callOnClick()
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }

        tvBackToMap = findViewById<View>(R.id.tv_back_to_map) as TextView
        tvBackToMap!!.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
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
        if (!connected()) {
            Log.d(tag, "[getCoinIds] !connected()")
            Snackbar.make(coordinatorLayout_bank, "No internet connection!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        finish()
                        startActivity(Intent(this, BankActivity::class.java))
                    }.show()
        } else {
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
                        walletOfCoins.add(newCoin)
                    }
                }
                Log.d(tag, "[getCoinIds] Wallet is $walletOfCoins")

                addCoinsToSpinner(walletOfCoins)
            }
        }
    }

        private fun addCoinsToSpinner(wallet: ArrayList<Coin>) {
            if (spinner.adapter is ArrayAdapter<*>) {
                (spinner.adapter as ArrayAdapter<*>).clear()
            }
            if (wallet.isEmpty()) {
                Log.d(tag, "[addCoinsToSpinner] Wallet is empty")
                val emptyWalletList = ArrayList<String>()
                emptyWalletList.add("Wallet is empty!")
                val adapter = ArrayAdapter(this, R.layout.spinner_layout, emptyWalletList)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout)
                spinner.adapter = adapter
            } else {
                Log.d(tag, "[addCoinsToSpinner] Wallet is $wallet")
                val adapter = ArrayAdapter(this, R.layout.spinner_layout, wallet.clone() as ArrayList<*>)
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout)

                spinner.adapter = adapter
            }

    }

    private fun fetchCoin() {
        Log.d(tag, "Selected ${spinner.selectedItem}")
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            coinsBankedTodayUserCollected = 0
            var coinsInWallet = 0
            val coinSelected = spinner.selectedItem as Coin
            for (coin in firebaseWallet) {
                if (coin.data[DATE_BANKED_FIELD] == todaysDate && coin.data[COLLECTED_BY_USER_FIELD] == "true" && coin.data[IS_BANKED_FIELD] == "true") {
                    coinsBankedTodayUserCollected++
                }
                if (coin.data[IS_BANKED_FIELD] == "false" && coin.data[TRANSFER_FIELD] == "false") {
                    coinsInWallet++
                }
            }
                Log.d(tag, "Coins in wallet = $coinsInWallet; coins banked today $coinsBankedTodayUserCollected")
                for (coin in firebaseWallet) {
                    if (coinSelected.id == coin.id) {
                        if (coinsBankedTodayUserCollected >= 25 && coinSelected.collectedByUser == "true") {
                            Snackbar.make(coordinatorLayout_bank, "You have already banked 25 of your own coins today!", Snackbar.LENGTH_SHORT).show()
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
                    Snackbar.make(coordinatorLayout_bank, "You can't send yourself coinz!", Snackbar.LENGTH_SHORT).show()
                } else {
                    transferTo(input)
                }
            } else {
                Snackbar.make(coordinatorLayout_bank, "User $input doesn't exist!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun transferTo(friendEmail: String) {
        firestoreWallet?.get()?.addOnSuccessListener {
            val coinSelected = spinner.selectedItem as Coin
                coinSelected.collectedByUser = "false"
                coinSelected.transferred = "true"
                Log.d(tag, "Coin is $coinSelected")
            val id = coinSelected.id

            for (Coin in it) {
                if (coinSelected.id == Coin.id) {
                    coinSelected.id = coinSelected.id + 0
                }
            }


                firestoreUsers!!.document(friendEmail).collection(SUB_COLLECTION_KEY).document(coinSelected.id).set(coinSelected).addOnSuccessListener {
                    Log.d(tag, "[transferTo] Coin ${coinSelected.id} added to $friendEmail wallet")
                }.addOnFailureListener{
                    Log.d(tag, "[transferTo] Error adding coin to $friendEmail wallet")
                }

                coinSelected.id = id
                coinSelected.collectedByUser = "true"

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
            firestoreBanked?.document(coin.id)?.set(bankedCoin)?.addOnSuccessListener {
                Snackbar.make(coordinatorLayout_bank, "Coin banked!", Snackbar.LENGTH_SHORT).show()

            }?.addOnFailureListener{
                Log.d(tag, "Failed to bank coin")
            }

            val modifiedCoin = mapOf(
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to "true"
            )

            firestoreWallet?.document(coin.id)?.update(modifiedCoin)
            finish()
            startActivity(Intent(this, BankActivity::class.java))

        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Snackbar.make(coordinatorLayout_bank, "Please select a coin to bank or transfer.", Snackbar.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }

    private fun connected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }



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

