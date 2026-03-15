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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing)

        // 1. First capture security cookie for InfinityFree
        NetworkBypass.getSecurityCookie(this) { cookie, userAgent ->
            // Save these for all subsequent API calls
            runOnUiThread {
                handleIntent(intent, cookie, userAgent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Refresh with bypass if needed, but for simplicity we'll just handle intent
        handleIntent(intent, "", "") 
    }

    private fun handleIntent(intent: Intent?, cookie: String, userAgent: String) {
        val appLinkData: Uri? = intent?.data
        var slug = GymManager.getGymSlug(this)
        
        if (appLinkData != null) {
            val deepSlug = appLinkData.getQueryParameter("gym")
            if (deepSlug != null) {
                slug = deepSlug
                GymManager.saveGymSlug(this, slug)
            }
        }
        
        fetchTenantBranding(slug, cookie, userAgent)
    }

    private fun fetchTenantBranding(slug: String, cookie: String, userAgent: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use captured security credentials
                val api = RetrofitClient.getApi(cookie, userAgent) 
                val response = api.getTenantInfo(slug)

                if (response.isSuccessful) {
                    val tenant = response.body()
                    withContext(Dispatchers.Main) {
                        tenant?.let { updateUIWithBranding(it) }
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

        // Launch Portal Button
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
            Log.e("BrandingError", "Invalid color formart", e)
        }
            
        tenant.aboutText?.let {
            findViewById<TextView>(R.id.heroDescription).text = it
        }
    }
}
