package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val userName = intent.getStringExtra("user_name") ?: "Unknown User"
        val userEmail = intent.getStringExtra("user_email") ?: "No Email"
        val gymName = intent.getStringExtra("gym_name") ?: "No Tenant"
        val tenantId = intent.getStringExtra("tenant_id") ?: "000"
        val logoUrl = intent.getStringExtra("logo_url") ?: ""
        val themeColorStr = intent.getStringExtra("theme_color") ?: ""

        val dashGymName = findViewById<TextView>(R.id.dashGymName)
        val gymLogo = findViewById<ImageView>(R.id.gymLogo)
        val profileInitial = findViewById<TextView>(R.id.profileInitial)
        val profileCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.profileInitial).parent.parent as? com.google.android.material.card.MaterialCardView

        findViewById<TextView>(R.id.dashUserName).text = userName
        findViewById<TextView>(R.id.dashUserEmail).text = userEmail
        dashGymName.text = gymName
        profileInitial.text = userName.firstOrNull()?.toString()?.uppercase() ?: "U"

        // Match the database branding like what a gym owner sees (Logo parity)
        if (logoUrl.isNotEmpty()) {
            val fullLogoUrl = if (logoUrl.startsWith("http")) logoUrl else "https://horizonfitnesscorp.gt.tc/$logoUrl"
            Glide.with(this)
                .load(fullLogoUrl)
                .into(gymLogo)
            gymLogo.visibility = android.view.View.VISIBLE
            profileInitial.visibility = android.view.View.GONE
        }

        // Apply Dynamic Branding
        if (themeColorStr.isNotEmpty()) {
            try {
                val color = android.graphics.Color.parseColor(themeColorStr)
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.profileCard)?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(color))
                findViewById<TextView>(R.id.statusLabel)?.setTextColor(color)
                findViewById<TextView>(R.id.tenantLabel)?.setTextColor(color)
            } catch (e: Exception) {
               Log.e("Branding", "Invalid color: $themeColorStr")
            }
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}