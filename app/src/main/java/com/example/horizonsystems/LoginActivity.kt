package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.LoginRequest
import com.example.horizonsystems.models.LoginResponse
import com.example.horizonsystems.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val usernameEdit = findViewById<TextInputEditText>(R.id.usernameEdit)
        val passwordEdit = findViewById<TextInputEditText>(R.id.passwordEdit)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val textRegister = findViewById<TextView>(R.id.textRegister)
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        btnLogin.setOnClickListener {
            val username = usernameEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }

        textRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(username: String, password: String, isRetry: Boolean = false, forcedCookie: String? = null, forcedUA: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use forced credentials if provided, otherwise fetch from manager
                 val cookie = forcedCookie ?: com.example.horizonsystems.utils.GymManager.getBypassCookie(this@LoginActivity)
                 val ua = forcedUA ?: com.example.horizonsystems.utils.GymManager.getBypassUA(this@LoginActivity)
                 
                 Log.d("AuthAuth", "Performing login (retry=$isRetry). Cookie present: ${cookie.isNotEmpty()}")
                 
                 val api = RetrofitClient.getApi(cookie.ifEmpty { null }, ua.ifEmpty { null }) 
                 val currentTenant = com.example.horizonsystems.utils.GymManager.getTenantCode(this@LoginActivity)
                 val loginRequest = com.example.horizonsystems.models.LoginRequest(username, password, currentTenant)
                 val response = api.login(loginRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.success == true) {
                            val user = loginResponse?.user
                            val branding = loginResponse?.branding
                            Toast.makeText(this@LoginActivity, "Welcome ${user?.firstName ?: "User"}", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                                putExtra("user_id", user?.userId ?: -1)
                                putExtra("gym_id", user?.gymId ?: -1)
                                putExtra("user_name", user?.username ?: "Guest")
                                putExtra("user_email", user?.email ?: "")
                                putExtra("gym_name", user?.gymName ?: (branding?.gymName ?: "No Tenant"))
                                putExtra("tenant_id", user?.tenantId ?: (branding?.tenantCode ?: "000"))
                                putExtra("logo_url", branding?.logoPath ?: "")
                                putExtra("theme_color", branding?.themeColor ?: "")
                                putExtra("bg_color", branding?.bgColor ?: "")
                            }
                            startActivity(intent)
                            finish()
                        } else if (loginResponse?.unverified == true) {
                            Toast.makeText(this@LoginActivity, "Please verify your account", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LoginActivity, VerifyActivity::class.java)
                            intent.putExtra("user_id", loginResponse?.userId ?: (loginResponse?.user?.userId ?: -1))
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LoginActivity, loginResponse?.message ?: "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("AuthError", "Server Error Body: $errorBody")
                        Toast.makeText(this@LoginActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                // Specifically catch parsing errors which indicate InfinityFree "Checking your browser" page
                val isParsingError = e is IllegalStateException || e is com.google.gson.JsonSyntaxException || e.message?.contains("Expected BEGIN_OBJECT") == true
                
                Log.e("AuthError", "Exception in performLogin: ${e.message}", e)

                if (isParsingError && !isRetry) {
                    withContext(Dispatchers.Main) {
                        Log.w("AuthAuth", "Bypass might have expired, refreshing...")
                        Toast.makeText(this@LoginActivity, "Refreshing security... Please wait", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Force refresh security cookie
                    com.example.horizonsystems.utils.NetworkBypass.getSecurityCookie(this@LoginActivity, forceRefresh = true) { newCookie, newUA ->
                        // Retry login with fresh credentials
                        performLogin(username, password, isRetry = true, forcedCookie = newCookie, forcedUA = newUA)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (isParsingError) "Security check failed. Please restart the app." else "Connection Error: ${e.message}"
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
