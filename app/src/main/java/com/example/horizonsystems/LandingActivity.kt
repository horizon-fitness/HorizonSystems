package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.horizonsystems.models.LoginRequest
import com.example.horizonsystems.models.LoginResponse
import com.example.horizonsystems.models.TenantPage
import com.example.horizonsystems.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.horizonsystems.utils.NetworkBypass
import com.example.horizonsystems.utils.GymManager

class LandingActivity : AppCompatActivity() {

    private var cachedCookie: String = ""
    private var cachedUserAgent: String = ""
    private var isBypassed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing)

        // 1. First capture security cookie for InfinityFree
        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
        val btnManualBypass = findViewById<MaterialButton>(R.id.btnManualBypass)
        
        // Timer for manual bypass (5 seconds)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val showManualBypassRunnable = Runnable {
            btnManualBypass.visibility = android.view.View.VISIBLE
        }
        handler.postDelayed(showManualBypassRunnable, 5000)
        
        btnManualBypass.setOnClickListener {
            handler.removeCallbacks(showManualBypassRunnable)
            loadingOverlay.visibility = android.view.View.GONE
            handleIntent(intent) // Try to fetch branding anyway
        }
        
        NetworkBypass.getSecurityCookie(this) { cookie, userAgent ->
            cachedCookie = cookie
            cachedUserAgent = userAgent
            isBypassed = true
            
            // Persist for other activities (RegisterActivity)
            GymManager.saveBypassCredentials(this, cookie, userAgent)
            
            runOnUiThread {
                handler.removeCallbacks(showManualBypassRunnable)
                loadingOverlay.visibility = android.view.View.GONE
                handleIntent(intent)
            }
        }

        // Navigation / Login
        val usernameEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.usernameEdit)
        val passwordEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordEdit)
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn).setOnClickListener {
            val username = usernameEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Trigger unified login logic
            performLogin(username, password)
        }

        findViewById<android.widget.TextView>(R.id.btnCreateAccount).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.btnForgotPassword).setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        btnTogglePassword.setOnClickListener {
            if (passwordEdit.transformationMethod is android.text.method.PasswordTransformationMethod) {
                passwordEdit.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                btnTogglePassword.alpha = 1.0f
            } else {
                passwordEdit.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                btnTogglePassword.alpha = 0.5f
            }
            passwordEdit.setSelection(passwordEdit.text?.length ?: 0)
        }

        // Remember Me initialization
        val rememberMeCheck = findViewById<android.widget.CheckBox>(R.id.rememberMe)
        if (GymManager.isRememberMeEnabled(this)) {
            rememberMeCheck.isChecked = true
            usernameEdit.setText(GymManager.getSavedUsername(this))
            passwordEdit.setText(GymManager.getSavedPassword(this))
        }
    }




    override fun onResume() {
        super.onResume()
        // Refresh branding if we're already bypassed to show changes from Owner dashboard
        if (isBypassed) {
            val slug = GymManager.getGymSlug(this)
            fetchTenantBranding(slug)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isBypassed) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val appLinkData: Uri? = intent?.data
        var slug = GymManager.getGymSlug(this)
        
        if (appLinkData != null) {
            val deepSlug = appLinkData.getQueryParameter("gym")
            if (deepSlug != null) {
                slug = deepSlug
            }
        }
        
        fetchTenantBranding(slug)
    }

    private fun fetchTenantBranding(slug: String) {
        if (!isBypassed) return
        
        Log.d("LandingActivity", "Fetching branding for slug: $slug")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(cachedCookie, cachedUserAgent) 
                val response = api.getTenantInfo(slug)

                if (response.isSuccessful) {
                    val tenant = response.body()
                    Log.d("LandingActivity", "Branding Fetch Success: ${tenant?.pageTitle}")
                    withContext(Dispatchers.Main) {
                        tenant?.let {
                            // Persist all data
                            GymManager.saveGymData(this@LandingActivity, it.pageSlug ?: "default", it.gymId ?: 0, it.tenantCode ?: "000", it.gymName ?: "Unknown")
                            updateUIWithBranding(it)
                            applyDynamicColors(it)
                        }
                    }
                } else {
                    Log.e("LandingActivity", "Branding Fetch Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LandingActivity", "Error fetching branding: ${e.message}")
            }
        }
    }

    private fun updateUIWithBranding(tenant: TenantPage) {
        val heroTitle = findViewById<TextView>(R.id.heroTitle)
        val heroDescription = findViewById<TextView>(R.id.heroDescription)
        val contactFooter = findViewById<TextView>(R.id.contactTextFooter)

        heroTitle.text = tenant.pageTitle ?: "WELCOME BACK"
        heroDescription.text = tenant.aboutText ?: "AUTHORIZED PERSONNEL ONLY"
        contactFooter.text = tenant.contactText ?: ""

        tenant.contactText?.let {
            contactFooter.text = it
        }
    }

    private fun applyDynamicColors(tenant: TenantPage) {
        tenant.themeColor?.let {
            try {
                val color = android.graphics.Color.parseColor(it)
                val btnSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)
                btnSignIn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            } catch (e: Exception) {
                Log.e("LandingActivity", "Invalid theme color: $it")
            }
        }

        tenant.bgColor?.let {
            try {
                val bg = android.graphics.Color.parseColor(it)
                findViewById<android.view.View>(R.id.rootLayout).setBackgroundColor(bg)
                findViewById<android.view.View>(R.id.innerLayout).setBackgroundColor(bg)
                findViewById<android.view.View>(android.R.id.content).rootView.setBackgroundColor(bg)
            } catch (e: Exception) {
                Log.e("LandingActivity", "Invalid bg color: $it")
            }
        }
    }

    private fun performLogin(username: String, password: String, isRetry: Boolean = false, forcedCookie: String? = null, forcedUA: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                 val cookie = forcedCookie ?: GymManager.getBypassCookie(this@LandingActivity)
                 val ua = forcedUA ?: GymManager.getBypassUA(this@LandingActivity)
                 
                 val api = RetrofitClient.getApi(cookie.ifEmpty { null }, ua.ifEmpty { null }) 
                 val currentTenant = GymManager.getTenantCode(this@LandingActivity)
                 val loginRequest = LoginRequest(username, password, currentTenant)
                 val response = api.login(loginRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.success == true) {
                            val user = loginResponse?.user
                            val branding = loginResponse?.branding
                            Toast.makeText(this@LandingActivity, "Welcome ${user?.firstName ?: "User"}", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@LandingActivity, MainActivity::class.java).apply {
                                putExtra("user_id", user?.userId ?: -1)
                                putExtra("gym_id", user?.gymId ?: -1)
                                putExtra("user_name", user?.username ?: "Guest")
                                putExtra("user_email", user?.email ?: "")
                                putExtra("gym_name", user?.gymName ?: (branding?.gymName ?: "No Tenant"))
                                putExtra("tenant_id", user?.tenantId ?: (branding?.tenantCode ?: "000"))
                                putExtra("logo_url", branding?.logoPath ?: "")
                            }

                            // Handle Remember Me
                            val rememberMeCheck = findViewById<android.widget.CheckBox>(R.id.rememberMe)
                            if (rememberMeCheck.isChecked) {
                                GymManager.saveLoginCredentials(this@LandingActivity, username, password)
                            } else {
                                GymManager.clearLoginCredentials(this@LandingActivity)
                            }

                            startActivity(intent)
                            finish()
                        } else if (loginResponse?.unverified == true) {
                            Toast.makeText(this@LandingActivity, "Please verify your account", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@LandingActivity, VerifyActivity::class.java)
                            intent.putExtra("user_id", loginResponse?.userId ?: (loginResponse?.user?.userId ?: -1))
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@LandingActivity, loginResponse?.message ?: "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@LandingActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                // Specifically catch parsing errors which indicate InfinityFree "Checking your browser" page
                val isParsingError = e is IllegalStateException || e is com.google.gson.JsonSyntaxException || e.message?.contains("Expected BEGIN_OBJECT") == true
                
                Log.e("AuthError", "Exception in performLogin: ${e.message}", e)

                if (isParsingError && !isRetry) {
                    withContext(Dispatchers.Main) {
                        Log.w("AuthError", "Bypass might have expired, refreshing...")
                        Toast.makeText(this@LandingActivity, "Refreshing security... Please wait", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Force refresh security cookie
                    NetworkBypass.getSecurityCookie(this@LandingActivity, forceRefresh = true) { newCookie, newUA ->
                        // Retry login with fresh credentials
                        performLogin(username, password, isRetry = true, forcedCookie = newCookie, forcedUA = newUA)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (isParsingError) "Security check failed. Please restart the app." else "Connection Error: ${e.message}"
                        Toast.makeText(this@LandingActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
