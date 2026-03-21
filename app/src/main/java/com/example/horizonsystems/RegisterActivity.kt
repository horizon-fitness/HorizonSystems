package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.widget.ImageView
import com.example.horizonsystems.models.RegisterRequest
import com.example.horizonsystems.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    private var currentStep = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
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

        // Pre-fill tenant code
        val currentTenantCode = com.example.horizonsystems.utils.GymManager.getTenantCode(this)
        gymIdEdit.setText(currentTenantCode)

        // Wizard Logic
        fun validateStep(step: Int): Boolean {
            return when (step) {
                1 -> {
                    if (gymIdEdit.text.isNullOrEmpty() || userRegEdit.text.isNullOrEmpty() || passRegEdit.text.isNullOrEmpty()) {
                        Toast.makeText(this, "Please complete account details", Toast.LENGTH_SHORT).show()
                        return false
                    }
                    if (passRegEdit.text.toString() != confirmPassRegEdit.text.toString()) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return false
                    }
                    true
                }
                2 -> {
                    if (firstNameEdit.text.isNullOrEmpty() || lastNameEdit.text.isNullOrEmpty()) {
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                        false
                    } else true
                }
                3 -> {
                    if (emailEdit.text.isNullOrEmpty() || phoneEdit.text.isNullOrEmpty()) {
                        Toast.makeText(this, "Please enter contact info", Toast.LENGTH_SHORT).show()
                        false
                    } else true
                }
                else -> true
            }
        }

        btnNext.setOnClickListener {
            if (validateStep(currentStep)) {
                if (currentStep < 4) {
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

            if (eName.isEmpty() || ePhone.isEmpty()) {
                Toast.makeText(this, "Please enter emergency contact info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
        findViewById<android.view.View>(R.id.btnSignBack).setOnClickListener {
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
        val btnSpace = findViewById<android.view.View>(R.id.btnSpace)
        val registerFooter = findViewById<android.view.View>(R.id.registerFooter)
        val indicator = findViewById<android.widget.TextView>(R.id.registerStepIndicator)

        // Toggle layouts
        layoutStepAccount.visibility = if (currentStep == 1) android.view.View.VISIBLE else android.view.View.GONE
        layoutStepPersonal.visibility = if (currentStep == 2) android.view.View.VISIBLE else android.view.View.GONE
        layoutStepContact.visibility = if (currentStep == 3) android.view.View.VISIBLE else android.view.View.GONE
        layoutStepHealth.visibility = if (currentStep == 4) android.view.View.VISIBLE else android.view.View.GONE

        // Toggle buttons
        btnPrev.visibility = if (currentStep > 1) android.view.View.VISIBLE else android.view.View.GONE
        btnSpace.visibility = if (currentStep > 1) android.view.View.VISIBLE else android.view.View.GONE
        btnNext.visibility = if (currentStep < 4) android.view.View.VISIBLE else android.view.View.GONE
        btnRegister.visibility = if (currentStep == 4) android.view.View.VISIBLE else android.view.View.GONE
        
        // Only show "Already part of family? Sign In" on the first step
        registerFooter.visibility = if (currentStep == 1) android.view.View.VISIBLE else android.view.View.GONE

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
            phoneNumber = phone,
            birthDate = birth,
            sex = sex,
            occupation = if (occupation.isEmpty()) null else occupation,
            address = address,
            medicalHistory = if (medical.isEmpty()) null else medical,
            emergencyContactName = eName,
            emergencyContactNumber = ePhone,
            tenantId = gymId
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use forced credentials if provided
                val cookie = forcedCookie ?: com.example.horizonsystems.utils.GymManager.getBypassCookie(this@RegisterActivity)
                val ua = forcedUA ?: com.example.horizonsystems.utils.GymManager.getBypassUA(this@RegisterActivity)

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

}   
