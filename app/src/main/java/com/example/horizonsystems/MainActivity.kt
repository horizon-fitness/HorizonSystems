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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        val userId = intent.getIntExtra("user_id", -1)
        val gymId = intent.getIntExtra("gym_id", -1)
        val userName = intent.getStringExtra("user_name") ?: "Unknown User"
        val userEmail = intent.getStringExtra("user_email") ?: "No Email"
        val gymName = intent.getStringExtra("gym_name") ?: "No Tenant"
        val tenantId = intent.getStringExtra("tenant_id") ?: "000"
        val logoUrl = intent.getStringExtra("logo_url") ?: ""
        val themeColorStr = intent.getStringExtra("theme_color") ?: ""
        val bgColorStr = intent.getStringExtra("bg_color") ?: ""

        val dashGymName = findViewById<TextView>(R.id.dashGymName)
        val gymLogo = findViewById<ImageView>(R.id.gymLogo)
        val profileInitial = findViewById<TextView>(R.id.profileInitial)
        
        // Attendance UI
        val btnAttendance = findViewById<MaterialButton>(R.id.btnAttendanceAction)
        val attendanceStatusText = findViewById<TextView>(R.id.attendanceStatusText)
        val attendanceTimeText = findViewById<TextView>(R.id.attendanceTimeText)

        findViewById<TextView>(R.id.dashUserName).text = userName
        findViewById<TextView>(R.id.dashUserEmail).text = userEmail
        dashGymName.text = gymName
        profileInitial.text = userName.firstOrNull()?.toString()?.uppercase() ?: "U"

        // Apply Dynamic Branding
        if (themeColorStr.isNotEmpty()) {
            try {
                val color = android.graphics.Color.parseColor(themeColorStr)
                findViewById<com.google.android.material.card.MaterialCardView>(R.id.profileCard)?.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(color))
                findViewById<TextView>(R.id.statusLabel)?.setTextColor(color)
                findViewById<TextView>(R.id.tenantLabel)?.setTextColor(color)
                btnAttendance.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            } catch (e: Exception) {
               Log.e("Branding", "Invalid theme color: $themeColorStr")
            }
        }

        if (bgColorStr.isNotEmpty()) {
            try {
                val bgColor = android.graphics.Color.parseColor(bgColorStr)
                findViewById<CoordinatorLayout>(android.R.id.content).rootView.setBackgroundColor(bgColor)
            } catch (e: Exception) {
                Log.e("Branding", "Invalid bg color: $bgColorStr")
            }
        }

        // Match the database branding like what a gym owner sees (Logo parity)
        if (logoUrl.isNotEmpty()) {
            val fullLogoUrl = if (logoUrl.startsWith("http")) logoUrl else "https://horizonfitnesscorp.gt.tc/$logoUrl"
            Glide.with(this)
                .load(fullLogoUrl)
                .into(gymLogo)
            gymLogo.visibility = android.view.View.VISIBLE
            profileInitial.visibility = android.view.View.GONE
        }

        // Attendance Logic
        var isCheckedIn = false
        
        fun updateAttendanceUI(checkedIn: Boolean, status: String?, time: String?) {
            isCheckedIn = checkedIn
            if (checkedIn) {
                attendanceStatusText.text = "Session Active"
                attendanceStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                attendanceTimeText.text = "Started at: ${time ?: "Now"}"
                btnAttendance.text = "Check Out Now"
                btnAttendance.icon = getDrawable(android.R.drawable.ic_lock_power_off)
            } else {
                attendanceStatusText.text = "Not Checked In"
                attendanceStatusText.setTextColor(android.graphics.Color.WHITE)
                attendanceTimeText.text = "Ready for your session?"
                btnAttendance.text = "Check In Now"
                btnAttendance.icon = getDrawable(android.R.drawable.ic_input_add)
            }
        }

        fun fetchAttendanceStatus() {
            if (userId == -1 || gymId == -1) return
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cookie = GymManager.getBypassCookie(this@MainActivity)
                    val ua = GymManager.getBypassUA(this@MainActivity)
                    val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                    
                    val response = api.handleAttendance(com.example.horizonsystems.models.AttendanceRequest(userId, gymId, "status"))
                    if (response.isSuccessful) {
                        val res = response.body()
                        withContext(Dispatchers.Main) {
                            updateAttendanceUI(res?.isCheckedIn == true, null, res?.lastSession?.get("check_in_time")?.toString())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Attendance", "Error fetching status", e)
                }
            }
        }

        btnAttendance.setOnClickListener {
            val action = if (isCheckedIn) "check_out" else "check_in"
            btnAttendance.isEnabled = false
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val cookie = GymManager.getBypassCookie(this@MainActivity)
                    val ua = GymManager.getBypassUA(this@MainActivity)
                    val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                    
                    val response = api.handleAttendance(com.example.horizonsystems.models.AttendanceRequest(userId, gymId, action))
                    withContext(Dispatchers.Main) {
                        btnAttendance.isEnabled = true
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@MainActivity, response.body()?.message, Toast.LENGTH_SHORT).show()
                            fetchAttendanceStatus() // Refresh
                        } else {
                            Toast.makeText(this@MainActivity, "Failed: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnAttendance.isEnabled = true
                        Toast.makeText(this@MainActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fetchAttendanceStatus()

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}