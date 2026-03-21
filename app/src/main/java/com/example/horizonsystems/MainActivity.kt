package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: com.google.android.material.bottomnavigation.BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_payment -> PaymentFragment()
                R.id.nav_booking -> BookingFragment()
                R.id.nav_membership -> MembershipFragment()
                R.id.nav_appointment -> AppointmentFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }

        // Apply Global Background Branding if provided
        val bgColorStr = intent.getStringExtra("bg_color") ?: ""
        if (bgColorStr.isNotEmpty()) {
            try {
                val bgColor = android.graphics.Color.parseColor(bgColorStr)
                findViewById<android.view.View>(android.R.id.content).setBackgroundColor(bgColor)
            } catch (e: Exception) {
                Log.e("Branding", "Invalid bg color: $bgColorStr")
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}