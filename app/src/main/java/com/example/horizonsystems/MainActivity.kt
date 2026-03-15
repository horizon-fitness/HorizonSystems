package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        findViewById<TextView>(R.id.dashUserName).text = userName
        findViewById<TextView>(R.id.dashUserEmail).text = userEmail
        findViewById<TextView>(R.id.dashGymName).text = "Tenant ID: $tenantId"
        findViewById<TextView>(R.id.profileInitial).text = userName.firstOrNull()?.toString()?.uppercase() ?: "U"

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}