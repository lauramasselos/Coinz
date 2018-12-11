package com.example.s1603459.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BankActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var todaysDate = "" // YYYY/MM/DD

    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null

    private var btnBank: Button? = null

    private var wallet: HashMap<String, String> = HashMap<String, String>()
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
//                Toast.makeText(applicationContext,"Ok, we change the app background.",Toast.LENGTH_SHORT).show()
                fetchCoin()
                Log.d(tag, "[initialise] btnBank")

            }
            // Display a negative button on alert dialog
            builder.setNegativeButton("No"){dialog,which ->
//                Toast.makeText(applicationContext,"You are not agree.",Toast.LENGTH_SHORT).show()
            }
//
//            // Display a neutral button on alert dialog
//            builder.setNeutralButton("Transfer"){_,_ ->
//                Toast.makeText(applicationContext,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
//            }

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
        if (currency.equals("SHIL")) {
            shilText!!.text = "1 SHIL = $exchangeRate GOLD"
        }
        if (currency.equals("DOLR")) {
            dolrText!!.text = "1 DOLR = $exchangeRate GOLD"
        }
        if (currency.equals("QUID")) {
            quidText!!.text = "1 QUID = $exchangeRate GOLD"
        }
        if (currency.equals("PENY")) {
            penyText!!.text = "1 PENY = $exchangeRate GOLD"
        }
    }

    private fun getCoinIds() {
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            for (coin in firebaseWallet) {
                val coinEntry = ("" + coin.data.get("Currency") + ": " + coin.data.get("Value")).replace("\"", "")
                val id = coin.id
                if (coin.data.get("Banked?") == false) {
                    wallet.put(id, coinEntry)
                }
                Log.d(tag, "Coin currency and value: ${coin.data.get("Currency")}, ${coin.data.get("Value")}")
            }
            addCoinsToSpinner(wallet)
        }
    }


    private fun addCoinsToSpinner(wallet: HashMap<String, String>) {
        var walletArray = ArrayList<String>()
        for (coin in wallet) {
            walletArray.add(coin.value)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, walletArray)
        spinner.adapter = adapter
    }

    private fun fetchCoin() {
        Log.d(tag, "Selected ${spinner.selectedItem}")
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            val currency = spinner.selectedItem.toString().substring(0, 4)
            val value = spinner.selectedItem.toString().substring(6)

            Log.d(tag, "Currency $currency with value $value")

            for (coin in firebaseWallet) {
                val isCollectedByUser = coin.data.get(COLLECTED_BY_USER_FIELD) as Boolean


                Log.d(tag, "[fetchCoin] first FOR loop ${coin.data.get(DATE_FIELD)} == $todaysDate ${coin.data.get(DATE_FIELD) == todaysDate} && ($isCollectedByUser) ${isCollectedByUser==true} && ${coin.data.get(IS_BANKED_FIELD) ==false}")



                if (coin.data.get(DATE_FIELD) == todaysDate && isCollectedByUser == true && coin.data.get(IS_BANKED_FIELD) == false) {
//                    Log.d(tag, "[fetchCoin] first FOR loop ${coin.data.get(DATE_FIELD)} == $todaysDate && ($isCollectedByUser) ${isCollectedByUser==true.toString()}")
                    coinsBankedTodayUserCollected++
                }
            }

            Log.d(tag, "[fetchCoin] coinsBankedTodayUserCollected = $coinsBankedTodayUserCollected")

            for (coin in firebaseWallet) {
                Log.d(tag, "[fetchCoin] Currency on firebase is ${coin.data.get(CURRENCY_FIELD)}; Currency on spinner is $currency")
                val isCollectedByUser = coin.data.get(COLLECTED_BY_USER_FIELD).toString()
                if (currency == coin.data.get(CURRENCY_FIELD).toString().replace("\"", "") && value == coin.data.get(VALUE_FIELD).toString().replace("\"", "")) {
//                    uniqueCoinId = coin.id
                    if (coin.data.get(IS_BANKED_FIELD) == true) {
                        Toast.makeText(this, "You just banked this coin!", Toast.LENGTH_SHORT).show()
                        /////////////////////////////////////////////////////////////////////////////////////////// ADD IF CONDITION HERE TO LIMIT MAX 25 BANKED PER DAY
                    } else if (coinsBankedTodayUserCollected >= 25) {
                        Toast.makeText(this, "You have already banked 25 of your own coins today!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(tag, "[fetchCoin] id is ${coin.id} currency is $currency value is $value")
                        bankCoin(coin.id, currency, value, isCollectedByUser)
                        break
                    }
                }
            }

//            bankCoin(uniqueCoinId, currency, value)

        }

    }

    private fun bankCoin(id: String, currency: String, value: String, isCollectedByUser: String) {
        firestoreExchangeRates!!.get().addOnSuccessListener { rates ->
//            Log.d(tag, "Exchange rate for $currency is ${rates.get(currency)}")
            val exchangeRate = rates.get(currency) as Double
            val gold = value.toDouble() * exchangeRate
            val bankedCoin = mapOf(
                    ID_FIELD to id,
                    "GOLD" to gold,
                    DATE_BANKED_FIELD to todaysDate,
                    IS_BANKED_FIELD to true,
                    COLLECTED_BY_USER_FIELD to isCollectedByUser.toBoolean()

            )

            banked(id, bankedCoin)

            val modifiedCoin = mapOf(
                    ID_FIELD to id,
                    VALUE_FIELD to value,
                    CURRENCY_FIELD to currency,
                    DATE_FIELD to todaysDate,
                    IS_BANKED_FIELD to true,
                    COLLECTED_BY_USER_FIELD to isCollectedByUser.toBoolean()
            )

            modifyWallet(id, modifiedCoin)


        }.addOnFailureListener{
            Log.d(tag, "Error getting exchange rates")
        }
    }

    private fun banked(id: String, bankedCoin: Map<String, Any>) {
        Log.d(tag, "[banked] $id")
        firestoreBanked?.document(id)
                ?.set(bankedCoin)
                ?.addOnSuccessListener {
            Toast.makeText(this, "Coin banked", Toast.LENGTH_SHORT).show()

        }?.addOnFailureListener{
            Log.d(tag, "Failed to bank coin")
        }
    }

    private fun modifyWallet(id: String, modifiedCoin: Map<String, Any>) {
        firestoreWallet?.document(id)?.set(modifiedCoin)?.addOnSuccessListener {
            Log.d(tag, "Coin removed from wallet successfully")

        }?.addOnFailureListener{
            Log.d(tag, "Failed to remove coin from wallet")
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(this, "Please select a coin to bank or transfer.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

//        coin = parent?.getItemAtPosition(position).toString()
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            for (coin in firebaseWallet) {
                val coinEntry = ("" + coin.data.get("Currency") + ": " + coin.data.get("Value")).replace("\"", "")
                val id = coin.id
                wallet.put(coinEntry, id)
            }
            Log.d(tag, "Selected coin ${parent?.getItemAtPosition(position)} at position $position with spinner item ${wallet.get(spinner.getItemAtPosition(position))}")
            Toast.makeText(this, "Selected ${parent?.getItemAtPosition(position)} with ID ${wallet.get(parent?.getItemAtPosition(position))}", Toast.LENGTH_SHORT).show()
        }

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
    private const val ID_FIELD = "ID"
    private const val VALUE_FIELD = "Value"
    private const val CURRENCY_FIELD = "Currency"
    private const val DATE_FIELD = "Date collected"
    private const val DATE_BANKED_FIELD = "Date banked"
    private const val IS_BANKED_FIELD = "Banked?"
    private const val COLLECTED_BY_USER_FIELD = "Collected by user?"

    }
}



/*
package com.example.s1603459.myapplication

import android.annotation.SuppressLint
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class BankActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var downloadDate = "" // YYYY/MM/DD

    private var shilText: TextView? = null
    private var dolrText: TextView? = null
    private var quidText: TextView? = null
    private var penyText: TextView? = null

    private var btnBank: Button? = null

    private var wallet: ArrayList<Coin> = ArrayList<Coin>()
    private var walletIds: ArrayList<String> = ArrayList<String>()

    private lateinit var name: String
    private lateinit var email: String
    private lateinit var spinner: Spinner
    private lateinit var coin: String

    private var mAuth: FirebaseAuth? = null
    private var user: FirebaseUser? = null
    private var firestore: FirebaseFirestore? = null
    private var mDatabase: FirebaseDatabase? = null
    private var firestoreWallet: CollectionReference? = null
    private var firestoreExchangeRates: DocumentReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        downloadDate = SimpleDateFormat("YYYY/MM/dd").format(Date())
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

        btnBank = findViewById<View>(R.id.bankBtn) as Button
        btnBank!!.setOnClickListener{ bankCoin() }

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
        if (currency.equals("SHIL")) {
            shilText!!.text = "1 SHIL = $exchangeRate GOLD"
        }
        if (currency.equals("DOLR")) {
            dolrText!!.text = "1 DOLR = $exchangeRate GOLD"
        }
        if (currency.equals("QUID")) {
            quidText!!.text = "1 QUID = $exchangeRate GOLD"
        }
        if (currency.equals("PENY")) {
            penyText!!.text = "1 PENY = $exchangeRate GOLD"
        }
    }

    private fun getCoinIds() {
        firestoreWallet?.get()?.addOnSuccessListener { firebaseWallet ->
            for (coinz in firebaseWallet) {
//                val coinEntry = ("" + coinz.data.get("Currency") + ": " + coinz.data.get("Value")).replace("\"", "")
                val coin = Coin(coinz.id, coinz.data.get("Currency") as String, coinz.data.get("Value") as String, coinz.data.get("Date collected") as String)
//                val id = coinz.id
//                wallet.add(coinEntry)
//                walletIds.add(id)
                wallet.add(coin)
                Log.d(tag, "Coin currency and value: ${coinz.data.get("Currency")}, ${coinz.data.get("Value")}")
            }
//            addCoinsToSpinner(wallet, walletIds)
            addCoinsToSpinner(wallet)
        }
    }

    private fun addCoinsToSpinner(wallet: ArrayList<Coin>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, wallet)
        spinner.adapter = adapter
    }

    private fun bankCoin() {

    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        Toast.makeText(this, "Please select a coin to bank or transfer.", Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Toast.makeText(this, "Selected ${parent?.getItemAtPosition(position)}", Toast.LENGTH_SHORT).show()
        coin = parent?.getItemAtPosition(position) as String
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
//    private const val ID_FIELD = "ID"
//    private const val VALUE_FIELD = "Value"
//    private const val CURRENCY_FIELD = "Currency"
//    private const val DATE_FIELD = "Date collected"
}


}

 */