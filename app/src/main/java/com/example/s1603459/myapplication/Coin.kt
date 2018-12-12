package com.example.s1603459.myapplication

class Coin(var id: String, var banked: String, var collectedByUser: String, var currency: String, var dateCollected: String, var value: String) {
    override fun toString(): String {
        return "$currency: $value"
    }
}

