package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import com.bumptech.glide.Glide
import com.example.horizonsystems.models.TenantPage
import com.example.horizonsystems.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.horizonsystems.utils.NetworkBypass
import com.example.horizonsystems.utils.GymManager

class LandingActivity : AppCompatActivity() {

    private var cachedCookie: String = ""
    private var cachedUserAgent: String = ""
    private var isBypassed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing)

        // 1. First capture security cookie for InfinityFree
        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
        val btnManualBypass = findViewById<MaterialButton>(R.id.btnManualBypass)
        
        // Timer for manual bypass (5 seconds)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val showManualBypassRunnable = Runnable {
            btnManualBypass.visibility = android.view.View.VISIBLE
        }
        handler.postDelayed(showManualBypassRunnable, 5000)
        
        btnManualBypass.setOnClickListener {
            handler.removeCallbacks(showManualBypassRunnable)
            loadingOverlay.visibility = android.view.View.GONE
            handleIntent(intent) // Try to fetch branding anyway
        }
        
        NetworkBypass.getSecurityCookie(this) { cookie, userAgent ->
            cachedCookie = cookie
            cachedUserAgent = userAgent
            isBypassed = true
            
            // Persist for other activities (LoginActivity, RegisterActivity)
            GymManager.saveBypassCredentials(this, cookie, userAgent)
            
            runOnUiThread {
                handler.removeCallbacks(showManualBypassRunnable)
                loadingOverlay.visibility = android.view.View.GONE
                handleIntent(intent)
            }
        }

        // Navigation to Login
        findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Register Button - Opens RegisterActivity
        findViewById<MaterialButton>(R.id.btnLaunchPortal).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }




    override fun onResume() {
        super.onResume()
        // Refresh branding whenever app is resumed to sync with Web Dashboard
        if (isBypassed) {
            val slug = GymManager.getGymSlug(this)
            fetchTenantBranding(slug)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isBypassed) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val appLinkData: Uri? = intent?.data
        var slug = GymManager.getGymSlug(this)
        
        if (appLinkData != null) {
            val deepSlug = appLinkData.getQueryParameter("gym")
            if (deepSlug != null) {
                slug = deepSlug
            }
        }
        
        fetchTenantBranding(slug)
    }

    private fun fetchTenantBranding(slug: String) {
        if (!isBypassed) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cachedCookie, cachedUserAgent) 
                val response = api.getTenantInfo(slug)

                if (response.isSuccessful) {
                    val tenant = response.body()
                    withContext(Dispatchers.Main) {
                        tenant?.let { 
                            // Only save if it's NOT the hardcoded default "Horizon Systems"
                            if (it.pageTitle != "Horizon Systems") {
                                GymManager.saveGymData(
                                    this@LandingActivity, 
                                    it.pageSlug, 
                                    it.gymId, 
                                    it.tenantCode ?: "000", 
                                    it.gymName ?: "Unknown",
                                    it.themeColor,
                                    it.bgColor,
                                    it.logoPath,
                                    it.aboutText
                                )
                            }
                            updateUIWithBranding(it) 
                            applyDynamicColors(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BrandingError", "Failed to fetch branding", e)
                withContext(Dispatchers.Main) {
                    // Local Fallback if API or Network fails
                    val defaultTenant = TenantPage(
                        pageId = 1,
                        gymId = 1,
                        tenantCode = "000",
                        pageSlug = "horizon",
                        pageTitle = "Horizon Systems",
                        logoPath = "assets/default_logo.png",
                        bannerImage = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=2070&auto=format&fit=crop",
                        themeColor = "#1a73e8",
                        bgColor = "#0a090d",
                        fontFamily = "Inter",
                        aboutText = "Welcome to Horizon Systems. Your fitness journey starts here.",
                        contactText = null,
                        appDownloadLink = "https://horizonfitnesscorp.gt.tc/download.php",
                        gymName = "Horizon Systems",
                        gymEmail = null,
                        gymContact = null
                    )
                    updateUIWithBranding(defaultTenant)
                    applyDynamicColors(defaultTenant)
                }
            }
        }
    }

    private fun updateUIWithBranding(tenant: TenantPage) {
        findViewById<TextView>(R.id.heroSubtitle).text = "● OPEN FOR MEMBERSHIP"
        
        // Match the Web Dashboard's default mixed-case title if no custom title is set
        val displayTitle = if (tenant.pageTitle.isNullOrEmpty() || tenant.pageTitle == "Horizon Systems") {
            "Elevate Your Fitness at \n${tenant.gymName ?: "Horizon Systems"}"
        } else {
            tenant.pageTitle
        }
        findViewById<TextView>(R.id.heroTitle).text = displayTitle
        
        // Load Logo using Glide
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        tenant.logoPath?.let {
            val fullLogoUrl = when {
                it.startsWith("http") -> it
                it.startsWith("data:image") -> it
                else -> "https://horizonfitnesscorp.gt.tc/$it"
            }
            Glide.with(this).load(fullLogoUrl).into(imgLogo)
        }
        
        // Load Banner using Glide
        val imgBanner = findViewById<ImageView>(R.id.bannerImage)
        tenant.bannerImage?.let {
            val fullBannerUrl = when {
                it.startsWith("http") -> it
                it.startsWith("data:image") -> it
                else -> "https://horizonfitnesscorp.gt.tc/$it"
            }
            Glide.with(this).load(fullBannerUrl).into(imgBanner)
        }

        // Apply Contact Text
        val txtContact = findViewById<TextView>(R.id.contactText)
        if (!tenant.contactText.isNullOrEmpty()) {
            txtContact.text = tenant.contactText
            txtContact.visibility = android.view.View.VISIBLE
        } else {
            txtContact.visibility = android.view.View.GONE
        }


    }

    private fun applyDynamicColors(tenant: TenantPage) {
        try {
            val color = android.graphics.Color.parseColor(tenant.themeColor)
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.headerLogoCard)
                .setCardBackgroundColor(color)
            findViewById<MaterialButton>(R.id.btnGetStarted)
                .backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            findViewById<MaterialButton>(R.id.btnLaunchPortal)
                .backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            
            // Apply color to Hero Subtitle tint
            findViewById<TextView>(R.id.heroSubtitle).setTextColor(color)
            findViewById<TextView>(R.id.heroSubtitle).backgroundTintList = android.content.res.ColorStateList.valueOf(color).withAlpha(30)
            
            // Tint Ambient Glow
            findViewById<ImageView>(R.id.ambientGlow).imageTintList = android.content.res.ColorStateList.valueOf(color)
                
            // Apply Background Color if provided
            tenant.bgColor?.let {
                val bg = android.graphics.Color.parseColor(it)
                findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.rootLayout).setBackgroundColor(bg)
            }
        } catch (e: Exception) {
            Log.e("BrandingError", "Invalid color format", e)
        }
            
        tenant.aboutText?.let {
            findViewById<TextView>(R.id.heroDescription).text = it
        }
    }
}
