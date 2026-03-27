package com.example.horizonsystems.utils

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NetworkBypass {
    private const val BYPASS_URL = "https://horizonfitnesscorp.gt.tc/"

    fun getSecurityCookie(context: Context, forceRefresh: Boolean = false, onCaptured: (String, String) -> Unit) {
        // 1. Skip if we already have valid bypass credentials AND we're not forcing refresh
        val existingCookie = GymManager.getBypassCookie(context)
        val existingUA = GymManager.getBypassUA(context)
        if (!forceRefresh && existingCookie.isNotEmpty() && existingCookie.contains("__test")) {
            onCaptured(existingCookie, existingUA)
            return
        }

        // WebView MUST be created and used on the Main thread
        (context as? LifecycleOwner)?.let { lifecycleOwner ->
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                val webView = WebView(context)
                val settings = webView.settings
                settings.javaScriptEnabled = true 
                settings.loadsImagesAutomatically = false 
                settings.blockNetworkImage = true
                
                val userAgent = settings.userAgentString
                
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val timeoutRunnable = Runnable {
                    if (webView.url != null) {
                        val cookies = CookieManager.getInstance().getCookie(webView.url)
                        if (cookies != null && cookies.contains("__test")) {
                            GymManager.saveBypassCredentials(context, cookies, userAgent)
                            onCaptured(cookies, userAgent)
                        } else {
                            onCaptured("", userAgent)
                        }
                    } else {
                        onCaptured("", userAgent)
                    }
                    webView.destroy()
                }
                handler.postDelayed(timeoutRunnable, 5000)

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val cookies = CookieManager.getInstance().getCookie(url)
                        if (cookies != null && cookies.contains("__test")) {
                            handler.removeCallbacks(timeoutRunnable)
                            GymManager.saveBypassCredentials(context, cookies, userAgent)
                            onCaptured(cookies, userAgent)
                            webView.destroy()
                        }
                    }
                }
                webView.loadUrl(BYPASS_URL)
            }
        } ?: run {
            // Fallback for non-lifecycle contexts
            onCaptured("", "")
        }
    }
}
