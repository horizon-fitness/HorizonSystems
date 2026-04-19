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
        
        // native splash screen wait removed for speed (0.5s max or just immediate)
        var isSplashReady = true 
        
        val rootLayout = findViewById<View>(R.id.rootLayout)
        // Set background color immediately to avoid white flash
        val cachedBg = GymManager.getBgColor(this)
        if (!cachedBg.isNullOrEmpty()) {
            try { rootLayout.setBackgroundColor(android.graphics.Color.parseColor(cachedBg)) } catch(e: Exception) {}
        }
        
        // Initialize view references
        loginScrollView = findViewById(R.id.loginScrollView)
        dashContainer = findViewById(R.id.dashContainer)
        
        // 0. Pre-fill branding from cache to avoid jumping
        prefillUIFromCache()

        val loadingOverlay = findViewById<android.view.View>(R.id.loadingOverlay)
        val btnManualBypass = findViewById<MaterialButton>(R.id.btnManualBypass)
        
        if (loadingOverlay == null || btnManualBypass == null) return
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val showManualBypassRunnable = Runnable {
            btnManualBypass.visibility = android.view.View.VISIBLE
        }

        btnManualBypass.setOnClickListener {
            handler.removeCallbacks(showManualBypassRunnable)
            loadingOverlay.visibility = android.view.View.GONE
            handleIntent(intent)
        }

        // 1. Initial UI Setup & Handle Intent immediately (Zero-Wait)
        handleIntent(intent)

        // 2. Check for cached credentials to hide overlay immediately
        val cachedCookieStr = GymManager.getBypassCookie(this)
        if (cachedCookieStr.isNotEmpty() && cachedCookieStr.contains("__test")) {
            loadingOverlay.visibility = android.view.View.GONE
            isBypassed = true
        } else {
            // Only show manual bypass if we really don't have a cookie
            handler.postDelayed(showManualBypassRunnable, 2000)
        }
        
        // 3. Refresh/Get Security Cookie in the background
        NetworkBypass.getSecurityCookie(this) { cookie, userAgent ->
            cachedCookie = cookie
            cachedUserAgent = userAgent
            isBypassed = true
            
            // Persist for other activities
            GymManager.saveBypassCredentials(this, cookie, userAgent)
            
            runOnUiThread {
                handler.removeCallbacks(showManualBypassRunnable)
                // Silent update - just refresh branding if needed
                if (GymManager.getGymSlug(this@LandingActivity) != "horizon") {
                    fetchTenantBranding(GymManager.getGymSlug(this@LandingActivity))
                }
                
                // Auto-login logic (if enabled)
                val skipAutoLogin = intent.getBooleanExtra("SKIP_AUTO_LOGIN", false)
                if (GymManager.isRememberMeEnabled(this@LandingActivity) && !skipAutoLogin) {
                    val savedUser = GymManager.getSavedUsername(this@LandingActivity)
                    val savedPass = GymManager.getSavedPassword(this@LandingActivity)
                    if (savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
                        performLogin(savedUser, savedPass)
                    }
                }
            }
        }

        // Navigation / Login
        val usernameEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.usernameEdit) ?: return
        val passwordEdit = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordEdit) ?: return
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)?.setOnClickListener {
            val username = usernameEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Trigger unified login logic
            performLogin(username, password)
        }

        findViewById<android.widget.TextView>(R.id.btnCreateAccount)?.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.btnForgotPassword)?.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        btnTogglePassword?.setOnClickListener {
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
            rememberMeCheck?.isChecked = true
            usernameEdit.setText(GymManager.getSavedUsername(this))
            passwordEdit.setText(GymManager.getSavedPassword(this))
        }

        // Switch Gym Button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSwitchGym)?.setOnClickListener {
            val intent = Intent(this, SwitchGymActivity::class.java)
            startActivity(intent)
        }

        // Initialize Bottom Navigation for Single Activity Dashboard
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.let {
            setupBottomNavigation()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView) ?: return
        bottomNav.menu.clear()
        bottomNav.inflateMenu(R.menu.bottom_nav_menu)
        bottomNav.setOnItemSelectedListener { item ->
            if (bottomNav.selectedItemId == item.itemId) return@setOnItemSelectedListener true
            
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_booking -> BookingFragment()
                R.id.nav_membership -> MembershipFragment()
                R.id.nav_attendance -> AttendanceFragment()
                R.id.nav_profile -> ProfileFragment()
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
        // Persist session data in GymManager for reliable fragment access
        user.userId?.let { GymManager.saveUserId(this, it) }
        user.memberId?.let { GymManager.saveMemberId(this, it) }

        // Transfer data to "Activity Intent" equivalent (mocking Intent extras for fragments)
        intent.apply {
            putExtra("user_id", user.userId ?: -1)
            putExtra("member_id", user.memberId ?: -1)
            putExtra("gym_id", user.gymId ?: -1)
            putExtra("user_name", user.username ?: "Guest")
            putExtra("email", user.email ?: "")
            putExtra("gym_name", user.gymName ?: (branding?.gymName ?: "No Tenant"))
            putExtra("tenant_id", user.tenantId ?: (branding?.tenantCode ?: "000"))
            putExtra("logo_url", branding?.logoPath ?: "")
            putExtra("user_role", user.role ?: "Member")
            
            // New Registration Fields (Schema Aligned)
            putExtra("first_name", user.firstName ?: "")
            putExtra("last_name", user.lastName ?: "")
            putExtra("middle_name", user.middleName ?: "")
            putExtra("contact_number", user.contactNumber ?: "")
            putExtra("address", user.address ?: "")
            putExtra("address_line", user.addressLine ?: "")
            putExtra("barangay", user.barangay ?: "")
            putExtra("city", user.city ?: "")
            putExtra("province", user.province ?: "")
            putExtra("region", user.region ?: "")
            putExtra("birth_date", user.birthDate ?: "")
            putExtra("sex", user.sex ?: "")
            putExtra("occupation", user.occupation ?: "")
            putExtra("medical_history", user.medicalHistory ?: "")
            putExtra("emergency_contact_name", user.emergencyContactName ?: "")
            putExtra("emergency_contact_number", user.emergencyContactNumber ?: "")
            putExtra("member_code", user.memberCode ?: "")
            putExtra("parent_name", user.parentName ?: "")
            putExtra("parent_contact_number", user.parentContactNumber ?: "")
            putExtra("profile_pic", user.profilePic ?: "")
        }

        loginScrollView?.visibility = android.view.View.GONE
        dashContainer?.visibility = android.view.View.VISIBLE

        // Initialize persistent top header
        val gymNameHeader = findViewById<TextView>(R.id.gymNameHeader)
        val gymLogoHeader = findViewById<ImageView>(R.id.gymLogoHeader)
        val gymLogoContainer = findViewById<View>(R.id.gymLogoContainer)
        
        val rawGymName = user.gymName?.uppercase() ?: (branding?.gymName?.uppercase() ?: "HORIZON SYSTEMS")
        
        // Apply Brand Name (Clean Style)
        gymNameHeader?.text = rawGymName

        // Apply Dynamic Background to Dashboard (Matching Login Screen)
        val bgColorStr = GymManager.getBgColor(this)
        try {
            val bgColor = android.graphics.Color.parseColor(bgColorStr)
            dashContainer?.setBackgroundColor(bgColor)
            // Also ensure the root layout is set
            findViewById<android.view.View>(R.id.rootLayout)?.setBackgroundColor(bgColor)
        } catch (e: Exception) {
            dashContainer?.setBackgroundColor(android.graphics.Color.parseColor("#050505"))
        }
        
        // Optimized Logo Loading with reliable fallback
        val logoUrl = if (!branding?.logoPath.isNullOrEmpty()) branding?.logoPath else GymManager.getGymLogo(this)
        
        if (!logoUrl.isNullOrEmpty()) {
            gymLogoContainer?.visibility = android.view.View.VISIBLE
            GymManager.loadLogo(this, logoUrl, gymLogoHeader)
        } else {
            // Last resort: Show default logo if no gym logo is set
            gymLogoContainer?.visibility = android.view.View.VISIBLE
            if (rawGymName.contains("HORIZON")) {
                gymLogoHeader?.setImageResource(R.drawable.horizon_logo)
                gymLogoHeader?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
            } else {
                gymLogoHeader?.setImageResource(R.drawable.ic_dumbbell)
                try {
                    val accent = android.graphics.Color.parseColor(GymManager.getThemeColor(this))
                    gymLogoHeader?.imageTintList = android.content.res.ColorStateList.valueOf(accent)
                } catch (e: Exception) {
                    gymLogoHeader?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY)
                }
            }
        }
        
        // Handle Member Profile Pic Placeholder in HomeFragment (via Intent/Mock)
        // Note: Actual fragment view might need to be reached if it's already inflated, 
        // but since we call loadFragment(HomeFragment()), it will handle its own UI.
        
        findViewById<View>(R.id.btnTopTransactions)?.setOnClickListener {
            loadFragment(PaymentFragment())
        }

        findViewById<View>(R.id.btnTopNotifications)?.setOnClickListener {
            // Launch Notifications Sheet
            try {
                NotificationSheet().show(supportFragmentManager, "notifications")
            } catch (e: Exception) {
                Log.e("LandingActivity", "Error showing NotificationSheet: ${e.message}")
            }
        }
        
        findViewById<View>(R.id.btnTopLogout)?.setOnClickListener {
            performLogout()
        }
        
        // Load initial fragment via navigation selection (which triggers loadFragment)
        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.nav_home
    }

    fun setTopNotificationsVisibility(visible: Boolean) {
        findViewById<View>(R.id.btnTopNotifications)?.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    fun performLogout() {
        dashContainer?.visibility = android.view.View.GONE
        loginScrollView?.visibility = android.view.View.VISIBLE
        
        // Optional: clear inputs
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordEdit)?.setText("")
    }

    /**
     * Helper to update user data in the intent extras dynamically.
     * This allows fragments to see changes immediately during the demo.
     */
    fun updateUserData(updates: Map<String, String>) {
        updates.forEach { (key, value) ->
            intent.putExtra(key, value)
        }
    }




    override fun onResume() {
        super.onResume()
        
        // Always refresh branding from cache to show changes after switching gyms
        prefillUIFromCache()
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
            if (appLinkData.scheme == "horizon" && appLinkData.host == "connect") {
                val code = appLinkData.getQueryParameter("tenant_code")
                if (!code.isNullOrEmpty()) {
                    Log.d("LandingActivity", "Deep link connection: $code")
                    fetchTenantBranding(code)
                    return
                }
            } else {
                val deepSlug = appLinkData.getQueryParameter("gym")
                if (deepSlug != null) {
                    slug = deepSlug
                }
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
                        if (tenant != null && (tenant.success == false || tenant.isSuspended == true)) {
                            // Enforce Restriction: Auto-Disconnect
                            handleRestrictedGym(tenant.message ?: "This gym is currently restricted.")
                        } else {
                            tenant?.let {
                                // Persist all data
                                GymManager.saveGymData(
                                    this@LandingActivity, 
                                    it.pageSlug ?: "default", 
                                    it.gymId ?: 0, 
                                    it.tenantCode ?: "000", 
                                    it.gymName ?: "Unknown",
                                    it.logoPath,
                                    it.themeColor,
                                    it.iconColor,
                                    it.textColor,
                                    it.bgColor,
                                    it.cardColor,
                                    it.autoCardTheme,
                                    it.openingTime,
                                    it.closingTime
                                )
                                updateUIWithBranding(it)
                                applyDynamicColors(it)
                            }
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

    private fun handleRestrictedGym(message: String) {
        // 1. Show Blocking Alert
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Connection Terminated")
            .setMessage(message + "\n\nYou have been automatically disconnected.")
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> 
                d.dismiss()
                disconnectGym()
            }
            .show()
    }

    private fun disconnectGym() {
        // 2. Clear Gym Data (Revert to Horizon Systems)
        GymManager.saveGymData(
            this,
            "horizon", 1, "000", "HORIZON SYSTEMS",
            null, "#8c2bee", "#a1a1aa", "#d1d5db", "#0a090d",
            "#141216", "1"
        )
        // 3. Refresh UI to default
        prefillUIFromCache()
        applyDynamicColors("#8c2bee")
        findViewById<android.view.View>(R.id.rootLayout)?.setBackgroundColor(android.graphics.Color.parseColor("#0a090d"))
        
        Toast.makeText(this, "Disconnected from gym due to account restrictions.", Toast.LENGTH_LONG).show()
    }

    private fun prefillUIFromCache() {
        val tenantTitle = findViewById<TextView>(R.id.tenantTitle) ?: return
        val gymLogo = findViewById<ImageView>(R.id.gymLogo)
        
        // Always reset logo state first to ensure default icons are gone
        gymLogo?.setImageDrawable(null)
        gymLogo?.imageTintList = null
        
        val savedName = GymManager.getGymName(this)
        val savedLogo = GymManager.getGymLogo(this)
        
        // Default to HORIZON SYSTEMS to match standard branding
        tenantTitle.text = if (savedName == "HORIZON SYSTEMS" || savedName.isEmpty()) "HORIZON SYSTEMS" else savedName.uppercase()
        
        if (!savedLogo.isNullOrEmpty()) {
            GymManager.loadLogo(this, savedLogo, gymLogo)
        } else {
            if (savedName == "HORIZON SYSTEMS" || savedName.isEmpty()) {
                gymLogo?.setImageResource(R.drawable.horizon_logo)
                gymLogo?.imageTintList = null
            } else {
                gymLogo?.setImageResource(R.drawable.ic_dumbbell)
                try {
                    gymLogo?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(GymManager.getThemeColor(this)))
                } catch (e:Exception) {}
            }
        }
        
        // Pre-emptively apply the theme color from cache so it stays on restart
        val savedColor = GymManager.getThemeColor(this)
        applyDynamicColors(savedColor)
        
        // Ensure background color is also applied from cache (crucial for disconnect/revert)
        try {
            val savedBg = GymManager.getBgColor(this).ifEmpty { "#0a090d" }
            findViewById<android.view.View>(R.id.rootLayout)?.setBackgroundColor(android.graphics.Color.parseColor(savedBg))
        } catch (e: Exception) {}

        // Static Icon for Switch Gym button (QR Code as requested)
        val btnSwitchGym = findViewById<MaterialButton>(R.id.btnSwitchGym)
        btnSwitchGym?.setIconResource(R.drawable.ic_qr_code)
    }

    private fun updateUIWithBranding(tenant: TenantPage) {
        val tenantTitle = findViewById<TextView>(R.id.tenantTitle)
        val gymLogo = findViewById<ImageView>(R.id.gymLogo)
        val gymName = tenant.gymName ?: "HORIZON SYSTEMS"
        
        // Always reset logo state first
        gymLogo?.setImageDrawable(null)
        gymLogo?.imageTintList = null
        
        tenantTitle?.text = gymName.uppercase()
        
        if (!tenant.logoPath.isNullOrEmpty()) {
            GymManager.loadLogo(this, tenant.logoPath!!, gymLogo)
        } else {
            if (gymName.uppercase().contains("HORIZON")) {
                gymLogo?.setImageResource(R.drawable.horizon_logo)
                gymLogo?.imageTintList = null
            } else {
                gymLogo?.setImageResource(R.drawable.ic_dumbbell)
                try {
                    gymLogo?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(tenant.themeColor))
                } catch (e:Exception) {}
            }
        }
        
        // Static Icon for Switch Gym button (QR Code as requested)
        val btnSwitchGym = findViewById<MaterialButton>(R.id.btnSwitchGym)
        btnSwitchGym?.setIconResource(R.drawable.ic_qr_code)
    }

    private fun applyDynamicColors(themeColor: String) {
        try {
            val color = android.graphics.Color.parseColor(themeColor)
            val btnSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)
            val btnSwitchGym = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSwitchGym)
            val rememberMe = findViewById<android.widget.CheckBox>(R.id.rememberMe)
            val btnForgotPassword = findViewById<android.widget.TextView>(R.id.btnForgotPassword)
            val btnCreateAccount = findViewById<android.widget.TextView>(R.id.btnCreateAccount)

            btnSignIn?.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            btnSwitchGym?.iconTint = android.content.res.ColorStateList.valueOf(color)
            btnSwitchGym?.rippleColor = android.content.res.ColorStateList.valueOf(color)
            
            btnForgotPassword?.setTextColor(color)
            btnCreateAccount?.setTextColor(color)

            // 5. Update Bottom Navigation (Active Tint & Background)
            val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
            if (bottomNav != null) {
                // Dynamic StateList for icons
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf()
                )
                val colors = intArrayOf(
                    color,
                    android.graphics.Color.parseColor("#80FFFFFF") // 50% White for inactive
                )
                bottomNav.itemIconTintList = android.content.res.ColorStateList(states, colors)
                
                // Match Navigation Background
                val bColor = try {
                    android.graphics.Color.parseColor(GymManager.getBgColor(this))
                } catch (e: Exception) {
                    android.graphics.Color.parseColor("#050505")
                }
                bottomNav.setBackgroundColor(bColor)
            }
        } catch (e: Exception) {
            Log.e("LandingActivity", "Error parsing color: $themeColor")
        }
    }

    private fun applyDynamicColors(tenant: TenantPage) {
        tenant.themeColor?.let {
            applyDynamicColors(it)
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
        val currentSlug = GymManager.getGymSlug(this)
        if (currentSlug == "horizon" || currentSlug.isEmpty()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Connection Required")
                .setMessage("Please connect to a gym first before logging in.")
                .setPositiveButton("Connect Gym") { d, _ ->
                    val intent = Intent(this, SwitchGymActivity::class.java)
                    startActivity(intent)
                    d.dismiss()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
            return
        }

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
                            if (user != null || loginResponse?.userId != null) {
                                val finalUser = user ?: com.example.horizonsystems.models.User(
                                    userId = loginResponse?.userId,
                                    username = username,
                                    role = "Member"
                                )
                                showDashboard(finalUser, branding)
                            } else {
                                Toast.makeText(this@LandingActivity, "User data missing from response", Toast.LENGTH_LONG).show()
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
