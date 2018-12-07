package com.example.s1603459.myapplication

class User {
    var name: String? = null
    var email: String? = null
    constructor() {

    }
    constructor(username: String?, email: String?) {
        this.name = username
        this.email = email
    }
}