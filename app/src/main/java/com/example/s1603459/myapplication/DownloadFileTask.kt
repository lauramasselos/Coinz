package com.example.s1603459.myapplication

import android.os.AsyncTask
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

// This class is used to download the map and coin information from the internet (GeoJSON files)

class DownloadFileTask(private val caller: DownloadCompleteListener) : AsyncTask<String, Void, String>() {

    // Attempts to load the GeoJSON file from network in the background as the user interacts with the app
    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (e: IOException) {
        "Unable to load content. Check your network connection. "
    }

    // Returns the GeoJSON file as a String
    private fun loadFileFromNetwork(urlString: String): String {
        val stream: InputStream = downloadUrl(urlString)
        return stream.bufferedReader().use { it.readText() }
    }

    // Returns an input stream from the URL 'http://homepages.inf.ed.ac.uk/.../coinzmap.geojson'
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

    // Upon finishing the methods above, the function downloadComplete(result) is called
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        caller.downloadComplete(result)
    }

}
