package com.example.s1603459.myapplication

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner : DownloadCompleteListener {
    var result : String? = null
    override fun downloadComplete(result: String) {
        this.result = result
    }
}