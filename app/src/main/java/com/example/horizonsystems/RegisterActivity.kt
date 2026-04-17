package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.horizonsystems.models.RegisterRequest
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private var currentStep = 1
    private var isGymLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateWizardUI()
            } else {
                onBackPressed()
            }
        }

        // Setup Sex Dropdown
        val sexOptions = arrayOf("Male", "Female")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sexOptions)
        val sexSpinner = findViewById<AutoCompleteTextView>(R.id.sexSpinner)
        sexSpinner.setAdapter(adapter)
        sexSpinner.setOnClickListener {
            sexSpinner.showDropDown()
        }

        // Fields
        val gymIdEdit = findViewById<TextInputEditText>(R.id.gymIdEdit)
        val userRegEdit = findViewById<TextInputEditText>(R.id.userRegEdit)
        val passRegEdit = findViewById<TextInputEditText>(R.id.passRegEdit)
        val confirmPassRegEdit = findViewById<TextInputEditText>(R.id.confirmPassRegEdit)
        
        val firstNameEdit = findViewById<TextInputEditText>(R.id.firstNameEdit)
        val lastNameEdit = findViewById<TextInputEditText>(R.id.lastNameEdit)
        val middleNameEdit = findViewById<TextInputEditText>(R.id.middleNameEdit)
        val birthDateEdit = findViewById<TextInputEditText>(R.id.birthDateEdit)

        val emailEdit = findViewById<TextInputEditText>(R.id.emailEdit)
        val phoneEdit = findViewById<TextInputEditText>(R.id.phoneNumberEdit)
        val occupationEdit = findViewById<TextInputEditText>(R.id.occupationEdit)
        val addressEdit = findViewById<TextInputEditText>(R.id.addressEdit)

        val medicalEdit = findViewById<TextInputEditText>(R.id.medicalHistoryEdit)
        val emergencyNameEdit = findViewById<TextInputEditText>(R.id.emergencyNameEdit)
        val emergencyPhoneEdit = findViewById<TextInputEditText>(R.id.emergencyPhoneEdit)

        // Navigation Controls
        val btnPrev = findViewById<MaterialButton>(R.id.btnPrev)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        // Pre-fill and Lock tenant code if connected to a gym
        val currentTenantCode = GymManager.getTenantCode(this)
        val currentLogo = GymManager.getGymLogo(this)
        
        if (!currentTenantCode.isNullOrEmpty() && currentTenantCode != "000" && currentTenantCode != "default") {
            gymIdEdit.setText(currentTenantCode)
            gymIdEdit.isEnabled = false // User cannot change if already connected to a gym
            gymIdEdit.alpha = 0.7f
            isGymLocked = true
            
            // Show gym logo in registration header
            if (!currentLogo.isNullOrEmpty()) {
                val logoContainer = findViewById<View>(R.id.gymLogoContainer)
                val logoImg = findViewById<ImageView>(R.id.registerGymLogo)
                logoContainer?.visibility = View.VISIBLE
                GymManager.loadLogo(this, currentLogo, logoImg)
            }
        }

        ThemeUtils.applyThemeToView(findViewById(android.R.id.content))
        applyDynamicColors()

        // Automatic Formatting (e.g., COR-2354)
        gymIdEdit.addTextChangedListener(object : android.text.TextWatcher {
            private var isInternalTag = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isInternalTag) return
                isInternalTag = true
                
                val original = s.toString()
                val upperCase = original.uppercase()
                
                // Remove all non-alphanumeric for cleanup, but keep hyphen if in right place
                val cleaned = upperCase.replace(Regex("[^A-Z0-9]"), "")
                
                val formatted = if (cleaned.length > 3) {
                    cleaned.substring(0, 3) + "-" + cleaned.substring(3)
                } else {
                    cleaned
                }

                if (original != formatted) {
                    s?.replace(0, s.length, formatted)
                }
                
                isInternalTag = false
            }
        })

        // Immediate Tenant Validation on focus lost
        gymIdEdit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val code = gymIdEdit.text.toString()
                if (code.isNotEmpty()) {
                    validateTenantCode(code) { isValid, _ ->
                        if (!isValid) gymIdEdit.error = "Invalid Tenant Code"
                        else gymIdEdit.error = null
                    }
                }
            }
        }

        // Wizard Logic
        fun validateStep(step: Int): Boolean {
            var isValid = true
            when (step) {
                1 -> {
                    if (gymIdEdit.text.isNullOrEmpty()) {
                        gymIdEdit.error = "Tenant field is required"
                        isValid = false
                    }
                    if (userRegEdit.text.isNullOrEmpty()) {
                        userRegEdit.error = "Username is required"
                        isValid = false
                    }
                    if (passRegEdit.text.isNullOrEmpty()) {
                        passRegEdit.error = "Password is required"
                        isValid = false
                    }
                    if (confirmPassRegEdit.text.isNullOrEmpty()) {
                        confirmPassRegEdit.error = "Please confirm password"
                        isValid = false
                    } else if (passRegEdit.text.toString() != confirmPassRegEdit.text.toString()) {
                        confirmPassRegEdit.error = "Passwords do not match"
                        isValid = false
                    }
                }
                2 -> {
                    if (firstNameEdit.text.isNullOrEmpty()) {
                        firstNameEdit.error = "First name is required"
                        isValid = false
                    }
                    if (lastNameEdit.text.isNullOrEmpty()) {
                        lastNameEdit.error = "Last name is required"
                        isValid = false
                    }
                    if (birthDateEdit.text.isNullOrEmpty()) {
                        birthDateEdit.error = "Birth date is required"
                        isValid = false
                    }
                    if (sexSpinner.text.isNullOrEmpty()) {
                        sexSpinner.error = "Sex is required"
                        isValid = false
                    }
                }
                3 -> {
                    if (emailEdit.text.isNullOrEmpty()) {
                        emailEdit.error = "Email address is required"
                        isValid = false
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailEdit.text.toString()).matches()) {
                        emailEdit.error = "Invalid email format"
                        isValid = false
                    }
                    if (phoneEdit.text.isNullOrEmpty()) {
                        phoneEdit.error = "Phone number is required"
                        isValid = false
                    }
                    if (addressEdit.text.isNullOrEmpty()) {
                        addressEdit.error = "Address is required"
                        isValid = false
                    }
                }
                4 -> {
                    if (emergencyNameEdit.text.isNullOrEmpty()) {
                        emergencyNameEdit.error = "Emergency contact name is required"
                        isValid = false
                    }
                    if (emergencyPhoneEdit.text.isNullOrEmpty()) {
                        emergencyPhoneEdit.error = "Emergency contact number is required"
                        isValid = false
                    }
                }
            }
            if (!isValid) {
                Toast.makeText(this, "Please fix highlighted fields", Toast.LENGTH_SHORT).show()
            }
            return isValid
        }

        btnNext.setOnClickListener {
            if (validateStep(currentStep)) {
                if (currentStep == 1) {
                    val code = gymIdEdit.text.toString()
                    validateTenantCode(code) { isValid, error ->
                        if (isValid) {
                            currentStep++
                            updateWizardUI()
                        } else {
                            Toast.makeText(this, error ?: "Invalid Tenant Code", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (currentStep < 4) {
                    currentStep++
                    updateWizardUI()
                }
            }
        }

        btnPrev.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateWizardUI()
            }
        }

        btnRegister.setOnClickListener {
            if (!validateStep(4)) return@setOnClickListener

            val first = firstNameEdit.text.toString()
            val last = lastNameEdit.text.toString()
            val middle = middleNameEdit.text.toString()
            val email = emailEdit.text.toString()
            val user = userRegEdit.text.toString()
            val pass = passRegEdit.text.toString()
            val gymIdStr = gymIdEdit.text.toString()
            val birth = birthDateEdit.text.toString()
            val sex = sexSpinner.text.toString()
            val occupation = occupationEdit.text.toString()
            val address = addressEdit.text.toString()
            val phone = phoneEdit.text.toString()
            val medical = medicalEdit.text.toString()
            val eName = emergencyNameEdit.text.toString()
            val ePhone = emergencyPhoneEdit.text.toString()

            performRegistration(user, email, pass, first, middle, last, phone, birth, sex, occupation, address, medical, eName, ePhone, gymIdStr)
        }


        // Date Picker
        birthDateEdit.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Birth Date")
                .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                calendar.timeInMillis = selection
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                birthDateEdit.setText(format.format(calendar.time))
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        // Footer navigation
        findViewById<View>(R.id.btnSignBack).setOnClickListener {
            onBackPressed()
        }

        val btnTogglePassword = findViewById<ImageView>(R.id.btnTogglePassword)
        val btnToggleConfirmPassword = findViewById<ImageView>(R.id.btnToggleConfirmPassword)

        btnTogglePassword.setOnClickListener {
            if (passRegEdit.transformationMethod is android.text.method.PasswordTransformationMethod) {
                passRegEdit.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                btnTogglePassword.alpha = 1.0f
            } else {
                passRegEdit.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                btnTogglePassword.alpha = 0.5f
            }
            passRegEdit.setSelection(passRegEdit.text?.length ?: 0)
        }

        btnToggleConfirmPassword.setOnClickListener {
            if (confirmPassRegEdit.transformationMethod is android.text.method.PasswordTransformationMethod) {
                confirmPassRegEdit.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                btnToggleConfirmPassword.alpha = 1.0f
            } else {
                confirmPassRegEdit.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                btnToggleConfirmPassword.alpha = 0.5f
            }
            confirmPassRegEdit.setSelection(confirmPassRegEdit.text?.length ?: 0)
        }

        updateWizardUI()
    }

    private fun updateWizardUI() {
        val layoutStepAccount = findViewById<android.widget.LinearLayout>(R.id.layoutStepAccount)
        val layoutStepPersonal = findViewById<android.widget.LinearLayout>(R.id.layoutStepPersonal)
        val layoutStepContact = findViewById<android.widget.LinearLayout>(R.id.layoutStepContact)
        val layoutStepHealth = findViewById<android.widget.LinearLayout>(R.id.layoutStepHealth)

        val btnPrev = findViewById<MaterialButton>(R.id.btnPrev)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val btnSpace = findViewById<View>(R.id.btnSpace)
        val registerFooter = findViewById<View>(R.id.registerFooter)
        val indicator = findViewById<TextView>(R.id.registerStepIndicator)

        // Toggle layouts
        layoutStepAccount.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutStepPersonal.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        layoutStepContact.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        layoutStepHealth.visibility = if (currentStep == 4) View.VISIBLE else View.GONE

        // Toggle buttons
        btnPrev.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        btnSpace.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        btnNext.visibility = if (currentStep < 4) View.VISIBLE else View.GONE
        btnRegister.visibility = if (currentStep == 4) View.VISIBLE else View.GONE
        
        // Only show "Already part of family? Sign In" on the first step
        registerFooter.visibility = if (currentStep == 1) View.VISIBLE else View.GONE

        // Update indicator
        val stepTitle = when(currentStep) {
            1 -> "Account Access"
            2 -> "Personal Information"
            3 -> "Contact Details"
            4 -> "Health & Emergency"
            else -> ""
        }
        indicator.text = "Step $currentStep of 4: $stepTitle"
    }

    private fun performRegistration(
        user: String, email: String, pass: String, first: String, middle: String, last: String,
        phone: String, birth: String, sex: String, occupation: String, address: String,
        medical: String, eName: String, ePhone: String, gymId: String, isRetry: Boolean = false,
        forcedCookie: String? = null, forcedUA: String? = null
    ) {
        val registrationData = RegisterRequest(
            firstName = first,
            middleName = if (middle.isEmpty()) null else middle,
            lastName = last,
            email = email,
            username = user,
            password = pass,
            contactNumber = phone,
            birthDate = birth,
            sex = sex,
            occupation = if (occupation.isEmpty()) null else occupation,
            address = address,
            medicalHistory = if (medical.isEmpty()) null else medical,
            emergencyContactName = eName,
            emergencyContactNumber = ePhone,
            tenantCode = gymId
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use forced credentials if provided
                val cookie = forcedCookie ?: GymManager.getBypassCookie(this@RegisterActivity)
                val ua = forcedUA ?: GymManager.getBypassUA(this@RegisterActivity)

                Log.d("RegisterAuth", "Performing registration (retry=$isRetry). Cookie present: ${cookie.isNotEmpty()}")
                
                val api = RetrofitClient.getApi(cookie.ifEmpty { null }, ua.ifEmpty { null })
                val response = api.register(registrationData)


                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val regResponse = response.body()
                        if (regResponse?.success == true) {
                            Toast.makeText(this@RegisterActivity, regResponse.message ?: "Registration Successful!", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@RegisterActivity, VerifyActivity::class.java).apply {
                                putExtra("user_id", regResponse.userId ?: -1)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, regResponse?.message ?: "Registration Failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("RegisterError", "Server Error Body: $errorBody")
                        Toast.makeText(this@RegisterActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // Specifically catch parsing errors which indicate InfinityFree "Checking your browser" page
                val isParsingError = e is IllegalStateException || e is com.google.gson.JsonSyntaxException || e.message?.contains("Expected BEGIN_OBJECT") == true
                
                Log.e("RegisterError", "Exception in performRegistration: ${e.message}", e)

                if (isParsingError && !isRetry) {
                    withContext(Dispatchers.Main) {
                        Log.w("RegisterAuth", "Bypass might have expired, refreshing...")
                        Toast.makeText(this@RegisterActivity, "Refreshing security... Please wait", Toast.LENGTH_SHORT).show()
                    }
                    
                    // Force refresh security cookie
                    com.example.horizonsystems.utils.NetworkBypass.getSecurityCookie(this@RegisterActivity, forceRefresh = true) { newCookie, newUA ->
                        // Retry registration with fresh credentials
                        performRegistration(user, email, pass, first, middle, last, phone, birth, sex, occupation, address, medical, eName, ePhone, gymId, isRetry = true, forcedCookie = newCookie, forcedUA = newUA)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (isParsingError) "Security check failed. Please restart the app." else "Connection Error: Check internet or try again."
                        Toast.makeText(this@RegisterActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun validateTenantCode(
        code: String,
        isRetry: Boolean = false,
        forcedCookie: String? = null,
        forcedUA: String? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cookie = forcedCookie ?: GymManager.getBypassCookie(this@RegisterActivity)
                val ua = forcedUA ?: GymManager.getBypassUA(this@RegisterActivity)
                
                val api = RetrofitClient.getApi(cookie.ifEmpty { null }, ua.ifEmpty { null })
                val response = api.validateTenant(mapOf("gym_id" to code))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            onResult(true, null)
                        } else {
                            onResult(false, body?.message ?: "Invalid Tenant Code")
                        }
                    } else {
                        onResult(false, "Server Error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                val isParsingError = e is IllegalStateException || e is com.google.gson.JsonSyntaxException || e.message?.contains("Expected BEGIN_OBJECT") == true
                
                if (isParsingError && !isRetry) {
                    withContext(Dispatchers.Main) {
                        Log.w("TenantAuth", "Bypass might have expired, refreshing...")
                    }
                    // Force refresh security cookie
                    com.example.horizonsystems.utils.NetworkBypass.getSecurityCookie(this@RegisterActivity, forceRefresh = true) { newCookie, newUA ->
                        validateTenantCode(code, isRetry = true, forcedCookie = newCookie, forcedUA = newUA, onResult = onResult)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMsg = if (isParsingError) "Security check failed. Please restart the app." else "Connection Error: Try again."
                        onResult(false, errorMsg)
                    }
                }
            }
        }
    }

    private fun applyDynamicColors() {
        val themeColor = GymManager.getThemeColor(this)
        try {
            val color = android.graphics.Color.parseColor(themeColor)
            val colorStateList = android.content.res.ColorStateList.valueOf(color)
            
            findViewById<TextView>(R.id.registerStepIndicator)?.setTextColor(color)
            findViewById<TextView>(R.id.btnSignBack)?.setTextColor(color)
            
            // Buttons
            findViewById<MaterialButton>(R.id.btnNext)?.let { btn ->
                btn.backgroundTintList = colorStateList
            }
            findViewById<MaterialButton>(R.id.btnRegister)?.let { btn ->
                btn.backgroundTintList = colorStateList
            }

            // Explicitly tint action links
            findViewById<TextView>(R.id.btnSignBack)?.setTextColor(color)

            // Apply global theme traversal to catch any nested widgets
            ThemeUtils.applyThemeToView(findViewById(android.R.id.content))
            
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Error applying theme color: $themeColor", e)
        }
    }
}
