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
        val userRole = intent.getStringExtra("user_role") ?: "Member"
        
        // Load appropriate menu based on role
        bottomNavigationView.menu.clear() 
        when {
            userRole.equals("Superadmin", ignoreCase = true) || userRole.equals("Super Admin", ignoreCase = true) -> bottomNavigationView.inflateMenu(R.menu.menu_superadmin)
            userRole.equals("Tenant", ignoreCase = true) || userRole.equals("Admin", ignoreCase = true) -> bottomNavigationView.inflateMenu(R.menu.menu_tenant)
            userRole.equals("Coach", ignoreCase = true) -> bottomNavigationView.inflateMenu(R.menu.menu_coach)
            else -> bottomNavigationView.inflateMenu(R.menu.bottom_nav_menu)
        }
        
        // Load default fragment based on role
        if (savedInstanceState == null) {
            val defaultFragment = when {
                userRole.equals("Superadmin", ignoreCase = true) || userRole.equals("Super Admin", ignoreCase = true) -> SuperAdminDashboardFragment()
                userRole.equals("Tenant", ignoreCase = true) || userRole.equals("Admin", ignoreCase = true) -> TenantDashboardFragment()
                userRole.equals("Coach", ignoreCase = true) -> CoachDashboardFragment()
                else -> HomeFragment()
            }
            loadFragment(defaultFragment)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> {
                    when {
                        userRole.equals("Superadmin", ignoreCase = true) || userRole.equals("Super Admin", ignoreCase = true) -> SuperAdminDashboardFragment()
                        userRole.equals("Tenant", ignoreCase = true) || userRole.equals("Admin", ignoreCase = true) -> TenantDashboardFragment()
                        userRole.equals("Coach", ignoreCase = true) -> CoachDashboardFragment()
                        else -> HomeFragment()
                    }
                }
                R.id.nav_payment, R.id.nav_revenue -> PaymentFragment()
                R.id.nav_booking -> BookingFragment()
                R.id.nav_membership, R.id.nav_tenants, R.id.nav_members -> MembershipFragment()
                R.id.nav_appointment -> AppointmentFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.nav_coaches -> TrainersFragment()
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