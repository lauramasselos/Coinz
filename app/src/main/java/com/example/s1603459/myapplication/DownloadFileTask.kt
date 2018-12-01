package com.example.s1603459.myapplication

import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.s1603459.myapplication.DownloadCompleteRunner.result
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask(private val caller: DownloadCompleteListener) : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (e: IOException) {
        "Unable to load content. Check your network connection. "
    }

    private fun loadFileFromNetwork(urlString: String): String {
        val stream: InputStream = downloadUrl(urlString)
        //Read input from stream; build result as string

        return stream.toString() // result
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true
        conn.connect()
        return conn.inputStream
    }

    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}
