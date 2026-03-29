package com.example.horizonsystems

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PayMongoActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay_mongo)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        webView = findViewById(R.id.webViewPayMongo)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        val checkoutUrl = intent.getStringExtra("checkout_url")
        if (checkoutUrl == null) {
            Toast.makeText(this, "Invalid Checkout Session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupWebView()
        webView.loadUrl(checkoutUrl)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                loadingOverlay.visibility = View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                // Handle GCash & Maya Deep Links
                if (url.startsWith("gcash://") || url.startsWith("maya://") || url.startsWith("paymaya://")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Toast.makeText(this@PayMongoActivity, "App not installed", Toast.LENGTH_SHORT).show()
                        return false
                    }
                }

                // Handle Success Redirect (More flexible detection)
                if (url.contains("https://horizonsystems.com/success")) {
                    android.util.Log.d("PayMongo", "Payment Success URL Detected: $url")
                    setResult(RESULT_OK)
                    finish()
                    return true
                }

                // Handle Cancel Redirect
                if (url.contains("https://horizonsystems.com/cancel")) {
                    android.util.Log.d("PayMongo", "Payment Cancel URL Detected: $url")
                    setResult(RESULT_CANCELED)
                    finish()
                    return true
                }

                return false
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@PayMongoActivity, "Connection Timeout. Please check your internet.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
