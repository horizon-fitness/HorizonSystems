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
        val gymName = intent.getStringExtra("gym_name") ?: "No Tenant"

        findViewById<TextView>(R.id.dashUserName).text = userName
        findViewById<TextView>(R.id.dashGymName).text = "Tenant: $gymName"

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            val intent = Intent(this, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}