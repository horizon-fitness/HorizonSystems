package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwitchGymActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_gym)

        val tenantCodeEdit = findViewById<EditText>(R.id.tenantCodeEdit)
        val gymLinkEdit = findViewById<EditText>(R.id.gymLinkEdit)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Automatic Formatting (LLL-NNNN)
        tenantCodeEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            private var isInternalTag = false
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isInternalTag) return
                isInternalTag = true
                val original = s.toString()
                val upperCase = original.uppercase()
                val cleaned = upperCase.replace(Regex("[^A-Z0-9]"), "")
                val formatted = if (cleaned.length > 3) {
                    cleaned.substring(0, 3) + "-" + cleaned.take(7).substring(3)
                } else {
                    cleaned
                }
                if (original != formatted) {
                    s?.replace(0, s.length, formatted)
                }
                isInternalTag = false
            }
        })

        findViewById<MaterialButton>(R.id.btnConnectCode).setOnClickListener {
            val code = tenantCodeEdit.text.toString().trim()
            if (code.isNotEmpty()) {
                // For simplicity, we'll try to use the code as a slug if it doesn't contain a dash, 
                // or handle the LLL-NNNN format if supported by the backend slug logic.
                // In most systems, the tenant code maps to a specific branding.
                switchGym(code.lowercase())
            } else {
                Toast.makeText(this, "Please enter a tenant code", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.btnConnectLink).setOnClickListener {
            val link = gymLinkEdit.text.toString().trim()
            if (link.isNotEmpty()) {
                val slug = extractSlugFromLink(link)
                switchGym(slug)
            } else {
                Toast.makeText(this, "Please enter a gym link", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.view.View>(R.id.qrCameraPlaceholder).setOnClickListener {
            Toast.makeText(this, "QR Scanner starting...", Toast.LENGTH_SHORT).show()
            // Placeholder for QR Scanning logic
        }
    }

    private fun extractSlugFromLink(link: String): String {
        // e.g., https://horizonfitness.com/gym-slug -> gym-slug
        return try {
            val uri = android.net.Uri.parse(link)
            uri.lastPathSegment ?: link
        } catch (e: Exception) {
            link
        }
    }

    private fun switchGym(slug: String) {
        val cookie = GymManager.getBypassCookie(this)
        val ua = GymManager.getBypassUA(this)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getTenantInfo(slug)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val tenant = response.body()
                        if (tenant != null) {
                            GymManager.saveGymData(
                                this@SwitchGymActivity,
                                tenant.pageSlug ?: slug,
                                tenant.gymId ?: 0,
                                tenant.tenantCode ?: "000",
                                tenant.gymName ?: "Unknown Gym"
                            )
                            Toast.makeText(this@SwitchGymActivity, "Connected to ${tenant.gymName}", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@SwitchGymActivity, "Gym not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SwitchGymActivity, "Invalid Gym Code or Link", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SwitchGymActivity, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("SwitchGymActivity", "Error switching gym", e)
                }
            }
        }
    }
}
