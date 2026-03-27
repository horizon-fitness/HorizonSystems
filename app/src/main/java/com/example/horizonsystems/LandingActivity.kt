package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.CheckBox
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
import androidx.core.widget.NestedScrollView
import android.widget.LinearLayout
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.horizonsystems.utils.NetworkBypass
import com.example.horizonsystems.utils.GymManager

class LandingActivity : AppCompatActivity() {

    private var cachedCookie: String = ""
    private var cachedUserAgent: String = ""
    private var isBypassed = false
    
    // UI References
    private var loginScrollView: NestedScrollView? = null
    private var dashContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Temporarily disabled to rule out API level issues
        // enableEdgeToEdge()
        setContentView(R.layout.activity_landing)
        
        // Initialize view references
        loginScrollView = findViewById(R.id.loginScrollView)
        dashContainer = findViewById(R.id.dashContainer)
        
        // 0. Pre-fill branding from cache to avoid jumping
        prefillUIFromCache()

        // 1. First capture security cookie for InfinityFree
        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay) ?: return
        val btnManualBypass = findViewById<MaterialButton>(R.id.btnManualBypass) ?: return
        
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
        
        // 1. Show overlay ONLY if we don't have a cached session to avoid "not opening" feel
        if (GymManager.getBypassCookie(this).isEmpty()) {
            loadingOverlay.visibility = android.view.View.VISIBLE
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
                
                // Auto-login if Remember Me is enabled
                if (GymManager.isRememberMeEnabled(this@LandingActivity)) {
                    val savedUser = GymManager.getSavedUsername(this@LandingActivity)
                    val savedPass = GymManager.getSavedPassword(this@LandingActivity)
                    if (savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
                        loadingOverlay.visibility = android.view.View.VISIBLE
                        performLogin(savedUser, savedPass)
                    }
                }
            }
        }

        // Navigation / Login
        val usernameEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.usernameEdit) ?: return
        val passwordEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordEdit) ?: return
        
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

        // Switch Gym Button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSwitchGym).setOnClickListener {
            val intent = Intent(this, SwitchGymActivity::class.java)
            startActivity(intent)
        }

        // Initialize Bottom Navigation for Single Activity Dashboard
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.menu.clear()
        bottomNav.inflateMenu(R.menu.bottom_nav_menu)
        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_payment -> PaymentFragment()
                R.id.nav_booking -> BookingFragment()
                R.id.nav_membership -> MembershipFragment()
                R.id.nav_appointment -> AppointmentFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun showDashboard(user: com.example.horizonsystems.models.User, branding: TenantPage?) {
        // Transfer data to "Activity Intent" equivalent (mocking Intent extras for fragments)
        intent.apply {
            putExtra("user_id", user.userId ?: -1)
            putExtra("gym_id", user.gymId ?: -1)
            putExtra("user_name", user.username ?: "Guest")
            putExtra("user_email", user.email ?: "")
            putExtra("gym_name", user.gymName ?: (branding?.gymName ?: "No Tenant"))
            putExtra("tenant_id", user.tenantId ?: (branding?.tenantCode ?: "000"))
            putExtra("logo_url", branding?.logoPath ?: "")
            putExtra("user_role", user.role ?: "Member")
        }

        loginScrollView?.visibility = android.view.View.GONE
        dashContainer?.visibility = android.view.View.VISIBLE

        // Initialize persistent top header
        findViewById<TextView>(R.id.gymNameHeader).text = user.gymName?.uppercase() ?: (branding?.gymName?.uppercase() ?: "HORIZON SYSTEMS")
        
        findViewById<View>(R.id.btnTopProfile).setOnClickListener {
            // Load Profile Fragment
            loadFragment(ProfileFragment())
        }
        
        findViewById<View>(R.id.btnTopLogout).setOnClickListener {
            performLogout()
        }
        
        // Load initial fragment
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView).selectedItemId = R.id.nav_home
        loadFragment(HomeFragment())
    }

    fun performLogout() {
        dashContainer?.visibility = android.view.View.GONE
        loginScrollView?.visibility = android.view.View.VISIBLE
        
        // Optional: clear inputs
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordEdit)?.setText("")
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

    private fun prefillUIFromCache() {
        val tenantTitle = findViewById<TextView>(R.id.tenantTitle)
        val savedName = GymManager.getGymName(this)
        // Default to HORIZON SYSTEMS to match standard branding
        tenantTitle.text = if (savedName == "HORIZON SYSTEMS" || savedName.isEmpty()) "HORIZON SYSTEMS" else savedName.uppercase()
    }

    private fun updateUIWithBranding(tenant: TenantPage) {
        val tenantTitle = findViewById<TextView>(R.id.tenantTitle)
        val gymName = tenant.gymName ?: "HORIZON SYSTEMS"
        
        tenantTitle.text = gymName.uppercase()
        
        // We preserve the user's XML description ("Enter your credentials...") 
        // unless the database provides a very specific branding title.
        // For now, we keep it static to avoid the "AUTHORIZED PERSONNEL ONLY" jump.
    }

    private fun applyDynamicColors(tenant: TenantPage) {
        tenant.themeColor?.let {
            try {
                val color = android.graphics.Color.parseColor(it)
                val btnSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)
                val gymLogo = findViewById<ImageView>(R.id.gymLogo)
                val btnSwitchGym = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSwitchGym)

                btnSignIn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
                gymLogo.imageTintList = android.content.res.ColorStateList.valueOf(color)
                btnSwitchGym.iconTint = android.content.res.ColorStateList.valueOf(color)
            } catch (e: Exception) {
                Log.e("LandingActivity", "Error parsing color: $it")
            }
        }
        tenant.bgColor?.let {
            try {
                val bg = android.graphics.Color.parseColor(it)
                findViewById<android.view.View>(R.id.rootLayout).setBackgroundColor(bg)
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
                    val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
                    loadingOverlay?.visibility = android.view.View.GONE

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse?.success == true) {
                            val user = loginResponse?.user
                            val role = user?.role ?: "Member"
                            
                            // Restrict to Members/Customers only as staff/admin are for Web
                            if (!role.equals("Member", ignoreCase = true) && !role.equals("Customer", ignoreCase = true)) {
                                Toast.makeText(this@LandingActivity, "Only members are allowed on Mobile", Toast.LENGTH_LONG).show()
                                return@withContext
                            }

                            val branding = loginResponse?.branding
                            Toast.makeText(this@LandingActivity, "Welcome ${user?.firstName ?: "User"}", Toast.LENGTH_SHORT).show()
                            
                            // Handle Remember Me
                            val rememberMeCheck = findViewById<CheckBox>(R.id.rememberMe)
                            if (rememberMeCheck?.isChecked == true) {
                                GymManager.saveLoginCredentials(this@LandingActivity, username, password)
                            } else {
                                GymManager.clearLoginCredentials(this@LandingActivity)
                            }

                            // Instead of starting MainActivity, transition UI state
                            if (user != null) {
                                showDashboard(user, branding)
                            }
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
                    }
                    
                    // Force refresh security cookie
                    NetworkBypass.getSecurityCookie(this@LandingActivity, forceRefresh = true) { newCookie, newUA ->
                        // Retry login with fresh credentials
                        performLogin(username, password, isRetry = true, forcedCookie = newCookie, forcedUA = newUA)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
                        loadingOverlay?.visibility = android.view.View.GONE
                        
                        val errorMsg = if (isParsingError) "Security check failed. Please restart the app." else "Connection Error: ${e.message}"
                        Toast.makeText(this@LandingActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
