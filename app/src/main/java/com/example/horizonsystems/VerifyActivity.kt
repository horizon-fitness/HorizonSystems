package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.LoginResponse
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify)

        val userId = intent.getIntExtra("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Invalid User Session", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val pinEdit = findViewById<TextInputEditText>(R.id.pinEdit)
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)

        btnVerify.setOnClickListener {
            val pin = pinEdit.text.toString().trim()
            if (pin.length != 6) {
                pinEdit.error = "Enter 6-digit PIN"
                return@setOnClickListener
            }

            verifyPin(userId, pin)
        }
    }

    private fun verifyPin(userId: Int, pin: String) {
        val payload = mapOf(
            "action" to "verify_pin",
            "user_id" to userId,
            "pin" to pin
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = GymManager.getBypassCookie(this@VerifyActivity)
                val ua = GymManager.getBypassUA(this@VerifyActivity)
                val api = RetrofitClient.getApi(cookie, ua)
                
                val response = api.verifyUser(payload)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@VerifyActivity, "Verified Successfully!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@VerifyActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@VerifyActivity, response.message ?: "Verification Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifyActivity, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
