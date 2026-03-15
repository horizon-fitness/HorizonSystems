package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    private fun performLogin(username: String, password: String) {
        // Need cookie for InfinityFree bypass
        // For simplicity in the demo, we'll use the same bypass logic as MainActivity 
        // OR we can assume bypass is working if they came from LandingActivity
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // In a real app, you'd get the cookie first. 
                // Let's reuse the bypass if needed, but for the demo 
                // we'll try to use the stored cookie if available or fetch one.
                
                // For Monday Activity, we will assume the RetrofitClient manages the bypass
                // Fetch persisted security credentials for InfinityFree
                 val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(this@LoginActivity)
                 val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(this@LoginActivity)
                 
                 val api = RetrofitClient.getApi(cookie, ua) 
                 val loginRequest = com.example.horizonsystems.models.LoginRequest(username, password)
                 val response = api.login(loginRequest)


                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.success == true) {
                            val user = loginResponse.user
                            Toast.makeText(this@LoginActivity, "Welcome ${user?.firstName}", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("user_name", user?.username ?: "Guest")
                            intent.putExtra("user_email", user?.email ?: "")
                            intent.putExtra("gym_name", user?.gymName ?: "No Tenant")
                            intent.putExtra("tenant_id", user?.tenantId ?: "000")
                            startActivity(intent)
                            finish()
                        } else if (loginResponse?.unverified == true) {
                            Toast.makeText(this@LoginActivity, "Please verify your account", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LoginActivity, VerifyActivity::class.java)
                            intent.putExtra("user_id", loginResponse.userId ?: -1)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LoginActivity, loginResponse?.message ?: "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("AuthError", "Login Error", e)
                    Toast.makeText(this@LoginActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
