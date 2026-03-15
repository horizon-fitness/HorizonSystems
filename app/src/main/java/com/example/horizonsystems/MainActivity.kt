package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // InfinityFree has an anti-bot shield that requires JavaScript.
        // We use a hidden WebView to "solve" the challenge and get the security cookie.
        bypassSecurityShieldAndFetchData()
    }

    private fun bypassSecurityShieldAndFetchData() {
        Log.d("DatabaseResponse", "1. Initializing Security Bypass (WebView)...")
        
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        val userAgent = webView.settings.userAgentString
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("DatabaseResponse", "2. Page finished Loading: $url")
                
                // Wait 2 seconds for the anti-bot JavaScript to run and set the cookie
                view?.postDelayed({
                    val cookies = CookieManager.getInstance().getCookie(url)
                    Log.d("DatabaseResponse", "3. Cookies found: $cookies")

                    if (cookies != null && cookies.contains("__test")) {
                        Log.d("DatabaseResponse", "4. Security cookie obtained! Starting API request...")
                        fetchDataWithCookie(cookies, userAgent)
                    } else {
                        Log.e("DatabaseResponse", "ERROR: Could not obtain security cookie.")
                    }
                }, 2000)
            }
        }
        
        // Load the URL to trigger the security script
        webView.loadUrl("https://horizonfitnesscorp.gt.tc/get_data.php")
    }

    private fun fetchDataWithCookie(cookie: String, userAgent: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // THE SECRET: InfinityFree requires the ?i=1 suffix once the cookie is set
                val url = URL("https://horizonfitnesscorp.gt.tc/get_data.php?i=1")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                
                // Use the EXACT same User-Agent and Cookie as the WebView
                connection.setRequestProperty("Cookie", cookie)
                connection.setRequestProperty("User-Agent", userAgent)

                val responseCode = connection.responseCode
                Log.d("DatabaseResponse", "5. API Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("DatabaseResponse", "6. SUCCESS! Downloaded JSON: $response")

                    if (response.trim().startsWith("[")) {
                        val jsonArray = JSONArray(response)
                        for (i in 0 until jsonArray.length()) {
                            val row = jsonArray.getJSONObject(i)
                            Log.d("DatabaseResponse", "Row Data: $row")
                        }
                    } else {
                        Log.e("DatabaseResponse", "FAILED: Received HTML instead of JSON. Bypass blocked.")
                    }
                } else {
                    Log.e("DatabaseResponse", "API ERROR: Server returned $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("DatabaseResponse", "CONNECTION ERROR: ${e.message}")
            }
        }
    }
}