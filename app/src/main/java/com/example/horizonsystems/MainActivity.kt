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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.horizonsystems.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.userRecyclerView)
        userAdapter = UserAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        // InfinityFree Security Bypass
        bypassSecurityShieldAndFetchData()
    }

    private fun bypassSecurityShieldAndFetchData() {
        Log.d("DatabaseResponse", "1. Initializing Security Bypass (WebView)...")
        
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        val userAgent = webView.settings.userAgentString
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                Log.d("DatabaseResponse", "1.5. Loading security page: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d("DatabaseResponse", "2. Page finished Loading. Solving challenge...")
                checkCookieLoop(url ?: "", userAgent, 0)
            }
        }
        
        webView.loadUrl("https://horizonfitnesscorp.gt.tc/get_data.php")
    }

    private fun checkCookieLoop(url: String, userAgent: String, attempts: Int) {
        if (attempts > 15) {
            Log.e("DatabaseResponse", "ERROR: Timeout waiting for security cookie.")
            return
        }

        val cookies = CookieManager.getInstance().getCookie(url)
        Log.d("DatabaseResponse", "3. Attempt $attempts - Cookies: $cookies")

        if (cookies != null && cookies.contains("__test=")) {
            Log.d("DatabaseResponse", "4. Security cookie obtained! Switching to Retrofit...")
            fetchDataWithRetrofit(cookies, userAgent)
        } else {
            // Wait 1 second and try again
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                checkCookieLoop(url, userAgent, attempts + 1)
            }, 1000)
        }
    }

    private fun fetchDataWithRetrofit(cookie: String, userAgent: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Now we use Retrofit for professional JSON handling
                val api = RetrofitClient.getApi(cookie, userAgent)
                val response = api.getUsers()

                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    Log.d("DatabaseResponse", "5. SUCCESS! Retrieved ${users.size} users.")
                    
                    // Switch to MAIN thread to update UI
                    withContext(Dispatchers.Main) {
                        userAdapter.updateUsers(users)
                    }
                } else {
                    Log.e("DatabaseResponse", "API ERROR: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("DatabaseResponse", "RETROFIT ERROR: ${e.message}")
            }
        }
    }
}