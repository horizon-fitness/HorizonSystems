package com.example.horizonsystems.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

object NetworkBypass {
    private const val BYPASS_URL = "https://horizonfitnesscorp.gt.tc/get_data.php"

    fun getSecurityCookie(context: Context, onCaptured: (String, String) -> Unit) {
        val webView = WebView(context)
        val userAgent = webView.settings.userAgentString
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null && cookies.contains("__test")) {
                    onCaptured(cookies, userAgent)
                    webView.destroy()
                }
            }
        }
        webView.loadUrl(BYPASS_URL)
    }
}
