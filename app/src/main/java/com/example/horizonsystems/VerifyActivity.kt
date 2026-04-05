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

        val otpBoxes = listOf(
            findViewById<android.widget.EditText>(R.id.otp1),
            findViewById<android.widget.EditText>(R.id.otp2),
            findViewById<android.widget.EditText>(R.id.otp3),
            findViewById<android.widget.EditText>(R.id.otp4),
            findViewById<android.widget.EditText>(R.id.otp5),
            findViewById<android.widget.EditText>(R.id.otp6)
        )
        val btnVerify = findViewById<MaterialButton>(R.id.btnVerify)

        setupOtpInputs(otpBoxes)

        btnVerify.setOnClickListener {
            val pin = otpBoxes.joinToString("") { it.text.toString() }
            if (pin.length != 6) {
                Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyPin(userId, pin)
        }
    }

    private fun setupOtpInputs(boxes: List<android.widget.EditText>) {
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < boxes.size - 1) {
                        boxes[index + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        boxes[index - 1].requestFocus()
                        boxes[index - 1].setText("")
                        true
                    } else false
                } else false
            }
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
                    if (response.success == true) {
                        Toast.makeText(this@VerifyActivity, "Verified Successfully!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@VerifyActivity, LandingActivity::class.java).apply {
                            putExtra("SKIP_AUTO_LOGIN", true)
                        })
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
