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
        NetworkBypass.getSecurityCookie(this) { cookie, userAgent ->
            cachedCookie = cookie
            cachedUserAgent = userAgent
            isBypassed = true
            runOnUiThread {
                handleIntent(intent)
            }
        }

        // Navigation to Login
        findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Mock download button
        findViewById<MaterialButton>(R.id.btnDownload).setOnClickListener {
            Toast.makeText(this, "Downloading Horizon Official App...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh branding if we're already bypassed to show changes from Owner dashboard
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
                            // Persist all data
                            GymManager.saveGymData(this@LandingActivity, it.pageSlug, it.gymId, it.gymName)
                            updateUIWithBranding(it) 
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BrandingError", "Failed to fetch branding", e)
            }
        }
    }

    private fun updateUIWithBranding(tenant: TenantPage) {
        findViewById<TextView>(R.id.headerTitle).text = tenant.gymName.uppercase()
        findViewById<TextView>(R.id.heroTitle).text = "Elevate Your\nFitness at ${tenant.gymName}"
        
        // Load Logo using Glide
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        tenant.logoPath?.let {
            val fullLogoUrl = if (it.startsWith("http")) it else "https://horizonfitnesscorp.gt.tc/$it"
            Glide.with(this).load(fullLogoUrl).into(imgLogo)
        }

        // Launch Portal Button - Opens specific web portal for this tenant
        findViewById<MaterialButton>(R.id.btnLaunchPortal).setOnClickListener {
            val portalUrl = "https://horizonfitnesscorp.gt.tc/portal.php?gym=${tenant.pageSlug}&preview=1"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
            startActivity(intent)
        }
        
        try {
            val color = android.graphics.Color.parseColor(tenant.themeColor)
            findViewById<com.google.android.material.card.MaterialCardView>(R.id.headerLogoCard)
                .setCardBackgroundColor(color)
            findViewById<MaterialButton>(R.id.btnGetStarted)
                .backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            findViewById<MaterialButton>(R.id.btnRegisterMember)
                .backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        } catch (e: Exception) {
            Log.e("BrandingError", "Invalid color format", e)
        }
            
        tenant.aboutText?.let {
            findViewById<TextView>(R.id.heroDescription).text = it
        }
    }
}
