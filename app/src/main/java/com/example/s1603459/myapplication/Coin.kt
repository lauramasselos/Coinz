package com.example.s1603459.myapplication

class Coin {
    var coinId: String? = null
    var currency: String? = null
    var value : Double? = null
    var date: String? = null
    var latitude: Double? = null
    var longitude: Double? = null

    constructor(id: String?, currency: String?, value: Double?, date: String?, latitude: Double?, longitude: Double?) {
        this.coinId = id
        this.currency = currency
        this.value = value
        this.date = date
        this.latitude = latitude
        this.longitude = longitude
    }
}