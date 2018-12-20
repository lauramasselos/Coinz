package com.example.s1603459.myapplication

// Coin object (all fields added to Firebase under each coin added to wallet or bank; each coin is organized by their ID)
class Coin(var id: String, var banked: String, var collectedByUser: String, var currency: String, var dateCollected: String, var value: String, var transferred: String) {

    // The toString() method is overridden here to allow just each coin's currency and value to be displayed on the wallet spinner in BankActivity.
    override fun toString(): String {
        return "$currency: $value"
    }
}

