package com.example.horizonsystems.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

object NetworkBypass {
    private const val BYPASS_URL = "https://horizonfitnesscorp.gt.tc/"

    fun getSecurityCookie(context: Context, onCaptured: (String, String) -> Unit) {
        // 1. Skip if we already have valid bypass credentials
        val existingCookie = GymManager.getBypassCookie(context)
        val existingUA = GymManager.getBypassUA(context)
        if (existingCookie.isNotEmpty() && existingCookie.contains("__test")) {
            onCaptured(existingCookie, existingUA)
            return
        }

        val webView = WebView(context)
        val settings = webView.settings
        settings.userAgentString = settings.userAgentString // Keep current
        settings.loadsImagesAutomatically = false // SPEED BOOST: Don't load images
        settings.blockNetworkImage = true
        settings.javaScriptEnabled = true // Required for InfinityFree
        
        val userAgent = settings.userAgentString
        
        // Timeout handling (7 seconds)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (webView.url != null) {
                val cookies = CookieManager.getInstance().getCookie(webView.url)
                if (cookies != null && cookies.contains("__test")) {
                    onCaptured(cookies, userAgent)
                } else {
                    // Force complete even without cookie to allow manual landing
                    onCaptured("", userAgent)
                }
                webView.destroy()
            }
        }
        handler.postDelayed(timeoutRunnable, 7000)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null && cookies.contains("__test")) {
                    handler.removeCallbacks(timeoutRunnable)
                    onCaptured(cookies, userAgent)
                    webView.destroy()
                }
            }
        }
        webView.loadUrl(BYPASS_URL)
    }
}
