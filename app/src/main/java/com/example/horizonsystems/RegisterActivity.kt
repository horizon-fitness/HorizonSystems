package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
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
import com.example.horizonsystems.models.RegisterRequest
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import androidx.activity.OnBackPressedCallback
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class RegisterActivity : AppCompatActivity() {

    private var currentStep = 1
    private var isGymLocked = false

    private lateinit var btnRegister: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var btnPrev: MaterialButton
    private lateinit var btnSpace: View

    // Layouts
    private lateinit var gymIdLayout: TextInputLayout
    private lateinit var userRegLayout: TextInputLayout
    private lateinit var passRegLayout: TextInputLayout
    private lateinit var confirmPassRegLayout: TextInputLayout
    
    private lateinit var firstNameLayout: TextInputLayout
    private lateinit var lastNameLayout: TextInputLayout
    private lateinit var birthDateLayout: TextInputLayout
    private lateinit var parentNameLayout: TextInputLayout
    private lateinit var parentPhoneLayout: TextInputLayout
    private lateinit var sexLayout: TextInputLayout
    
    private lateinit var emailLayout: TextInputLayout
    private lateinit var phoneNumberLayout: TextInputLayout
    private lateinit var addressLineLayout: TextInputLayout
    private lateinit var barangayLayout: TextInputLayout
    private lateinit var cityLayout: TextInputLayout
    private lateinit var provinceLayout: TextInputLayout
    private lateinit var regionLayout: TextInputLayout
    
    private lateinit var emergencyNameLayout: TextInputLayout
    private lateinit var emergencyPhoneLayout: TextInputLayout

    // Edits
    private lateinit var gymIdEdit: TextInputEditText
    private lateinit var userRegEdit: TextInputEditText
    private lateinit var passRegEdit: TextInputEditText
    private lateinit var confirmPassRegEdit: TextInputEditText
    
    private lateinit var firstNameEdit: TextInputEditText
    private lateinit var lastNameEdit: TextInputEditText
    private lateinit var birthDateEdit: TextInputEditText
    private lateinit var parentNameEdit: TextInputEditText
    private lateinit var parentPhoneEdit: TextInputEditText
    private lateinit var sexSpinner: AutoCompleteTextView
    
    private lateinit var emailEdit: TextInputEditText
    private lateinit var phoneEdit: TextInputEditText
    private lateinit var addressLineEdit: TextInputEditText
    private lateinit var barangayEdit: TextInputEditText
    private lateinit var cityEdit: TextInputEditText
    private lateinit var provinceEdit: TextInputEditText
    private lateinit var regionEdit: TextInputEditText
    
    private lateinit var middleNameEdit: TextInputEditText
    private lateinit var occupationEdit: TextInputEditText
    private lateinit var medicalHistoryEdit: TextInputEditText
    private lateinit var emergencyNameEdit: TextInputEditText
    private lateinit var emergencyPhoneEdit: TextInputEditText

    private lateinit var reqLen: TextView
    private lateinit var reqNum: TextView
    private lateinit var reqSpecial: TextView
    private lateinit var reqCaps: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_register)

            bindViews()
            setupListeners()
            setupDropdowns()
            setupPasswordComplexityWatcher()
            setupErrorClearing()
            setupPhoneFormatting()
            
            loadGymContext()

            ThemeUtils.applyThemeToView(findViewById(android.R.id.content))
            applyDynamicColors()
            restoreRegistrationData()
            loadGymContext()
            updateWizardUI()
            setupBackNavigation()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Boot Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun bindViews() {
        // Step 1
        gymIdLayout = findViewById(R.id.gymIdLayout)
        userRegLayout = findViewById(R.id.userRegLayout)
        passRegLayout = findViewById(R.id.passRegLayout)
        confirmPassRegLayout = findViewById(R.id.confirmPassRegLayout)
        gymIdEdit = findViewById(R.id.gymIdEdit)
        userRegEdit = findViewById(R.id.userRegEdit)
        passRegEdit = findViewById(R.id.passRegEdit)
        confirmPassRegEdit = findViewById(R.id.confirmPassRegEdit)
        reqLen = findViewById(R.id.reqLen)
        reqNum = findViewById(R.id.reqNum)
        reqSpecial = findViewById(R.id.reqSpecial)
        reqCaps = findViewById(R.id.reqCaps)

        // Step 2
        firstNameLayout = findViewById(R.id.firstNameLayout)
        lastNameLayout = findViewById(R.id.lastNameLayout)
        birthDateLayout = findViewById(R.id.birthDateLayout)
        parentNameLayout = findViewById(R.id.parentNameLayout)
        parentPhoneLayout = findViewById(R.id.parentPhoneLayout)
        sexLayout = findViewById(R.id.sexLayout)
        firstNameEdit = findViewById(R.id.firstNameEdit)
        lastNameEdit = findViewById(R.id.lastNameEdit)
        middleNameEdit = findViewById(R.id.middleNameEdit)
        birthDateEdit = findViewById(R.id.birthDateEdit)
        parentNameEdit = findViewById(R.id.parentNameEdit)
        parentPhoneEdit = findViewById(R.id.parentPhoneEdit)
        sexSpinner = findViewById(R.id.sexSpinner)

        // Step 3
        emailLayout = findViewById(R.id.emailLayout)
        phoneNumberLayout = findViewById(R.id.phoneNumberLayout)
        addressLineLayout = findViewById(R.id.addressLineLayout)
        barangayLayout = findViewById(R.id.barangayLayout)
        cityLayout = findViewById(R.id.cityLayout)
        provinceLayout = findViewById(R.id.provinceLayout)
        regionLayout = findViewById(R.id.regionLayout)
        
        emailEdit = findViewById(R.id.emailEdit)
        phoneEdit = findViewById(R.id.phoneNumberEdit)
        addressLineEdit = findViewById(R.id.addressLineEdit)
        barangayEdit = findViewById(R.id.barangayEdit)
        cityEdit = findViewById(R.id.cityEdit)
        provinceEdit = findViewById(R.id.provinceEdit)
        regionEdit = findViewById(R.id.regionEdit)
        occupationEdit = findViewById(R.id.occupationEdit)

        // Step 4
        emergencyNameLayout = findViewById(R.id.emergencyNameLayout)
        emergencyPhoneLayout = findViewById(R.id.emergencyPhoneLayout)
        medicalHistoryEdit = findViewById(R.id.medicalHistoryEdit)
        emergencyNameEdit = findViewById(R.id.emergencyNameEdit)
        emergencyPhoneEdit = findViewById(R.id.emergencyPhoneEdit)

        // Nav
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnSpace = findViewById(R.id.btnSpace)
        btnRegister = findViewById(R.id.btnRegister)
    }

    private fun setupListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (currentStep > 1) { currentStep--; updateWizardUI() } else finish()
        }

        gymIdEdit.filters = arrayOf(android.text.InputFilter { source, start, end, dest, dstart, dend ->
            if (start == end) return@InputFilter null
            
            val added = source.subSequence(start, end).toString().uppercase()
            val currentDest = java.lang.StringBuilder(dest.subSequence(0, dstart))
            val sb = java.lang.StringBuilder()
            
            for (i in added.indices) {
                val c = added[i]
                val len = currentDest.length
                if (len < 3) {
                    if (c.isLetter()) {
                        currentDest.append(c)
                        sb.append(c)
                    }
                } else if (len == 3) {
                    if (c == '-') {
                        currentDest.append(c)
                        sb.append(c)
                    } else if (c.isDigit()) {
                        currentDest.append("-").append(c)
                        sb.append("-").append(c)
                    }
                } else {
                    if (c.isDigit() && currentDest.length < 8) {
                        currentDest.append(c)
                        sb.append(c)
                    }
                }
            }
            
            val suffix = dest.subSequence(dend, dest.length)
            val finalStr = currentDest.toString() + suffix
            
            if (finalStr.matches(Regex("^([A-Z]{0,3}|[A-Z]{3}-\\d{0,4})$"))) {
                sb.toString()
            } else {
                ""
            }
        })
        
        // Fix for the blinking cursor (Selection) jumping to the start
        gymIdEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.isNotEmpty()) {
                    val sel = Selection.getSelectionEnd(s)
                    if (sel < s.length) {
                        Selection.setSelection(s, s.length)
                    }
                }
            }
        })

        gymIdEdit.setOnFocusChangeListener { _, h ->
            if (!h && gymIdEdit.text?.isNotEmpty() == true) {
                validateTenantCode(gymIdEdit.text.toString()) { v, e -> if (!v) gymIdLayout.error = e else gymIdLayout.error = null }
            }
        }

        btnNext.setOnClickListener {
            if (validateStep(currentStep)) {
                if (currentStep == 1) {
                    validateTenantCode(gymIdEdit.text.toString()) { v, e ->
                        if (v) { currentStep++; updateWizardUI() } else gymIdLayout.error = e
                    }
                } else { currentStep++; updateWizardUI() }
            }
        }

        btnPrev.setOnClickListener { if (currentStep > 1) { currentStep--; updateWizardUI() } }

        btnRegister.setOnClickListener {
            if (validateStep(4)) performRegistration()
        }

        birthDateEdit.setOnClickListener { showDatePicker() }
        
        findViewById<View>(R.id.btnSignBack).setOnClickListener { 
             if (currentStep > 1) { currentStep--; updateWizardUI() } else finish()
        }
    }

    private fun setupDropdowns() {
        val sexOptions = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sexOptions)
        sexSpinner.setAdapter(adapter)
    }

    private fun loadGymContext() {
        val code = GymManager.getTenantCode(this)
        val logo = GymManager.getGymLogo(this)
        
        // Strictly empty unless connected to a real gym (not 000, default, or horizon)
        val isRealGym = !code.isNullOrEmpty() && code != "000" && code != "default" && code != "horizon"
        
        if (isRealGym) {
            gymIdEdit.setText(code)
            gymIdEdit.isEnabled = false
            gymIdEdit.alpha = 0.7f
            isGymLocked = true
            if (!logo.isNullOrEmpty()) {
                val logoImg = findViewById<ImageView>(R.id.registerGymLogo)
                findViewById<View>(R.id.gymLogoContainer)?.visibility = View.VISIBLE
                GymManager.loadLogo(this, logo, logoImg)
            }
        } else {
            // If not connected to a gym, ensure it's empty so they can type
            gymIdEdit.setText("")
            gymIdEdit.isEnabled = true
            gymIdEdit.alpha = 1.0f
            isGymLocked = false
        }
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Birth Date")
            .setCalendarConstraints(constraints)
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = selection
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(cal.time)
            birthDateEdit.setText(dateStr)
            handleAgeLogic(cal)
        }
        picker.show(supportFragmentManager, "BIRTH_DATE_PICKER")
    }

    private fun handleAgeLogic(birthCal: Calendar) {
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--

        if (age < 12) {
            birthDateLayout.error = "Minimum age is 12 years old"
            findViewById<View>(R.id.parentalInfoSection).visibility = View.GONE
        } else if (age < 18) {
            birthDateLayout.error = null
            findViewById<View>(R.id.parentalInfoSection).visibility = View.VISIBLE
            Toast.makeText(this, "Parent/Guardian info required for minors", Toast.LENGTH_SHORT).show()
        } else {
            birthDateLayout.error = null
            findViewById<View>(R.id.parentalInfoSection).visibility = View.GONE
        }
    }

    private fun setupErrorClearing() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Clear all errors
                gymIdLayout.error = null; userRegLayout.error = null; passRegLayout.error = null; confirmPassRegLayout.error = null
                firstNameLayout.error = null; lastNameLayout.error = null; birthDateLayout.error = null
                parentNameLayout.error = null; parentPhoneLayout.error = null; sexLayout.error = null
                emailLayout.error = null; phoneNumberLayout.error = null
                addressLineLayout.error = null; barangayLayout.error = null; cityLayout.error = null; provinceLayout.error = null; regionLayout.error = null
                emergencyNameLayout.error = null; emergencyPhoneLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        val edits = listOf(gymIdEdit, userRegEdit, passRegEdit, confirmPassRegEdit, firstNameEdit, lastNameEdit, parentNameEdit, parentPhoneEdit, emailEdit, phoneEdit, 
            addressLineEdit, barangayEdit, cityEdit, provinceEdit, regionEdit, emergencyNameEdit, emergencyPhoneEdit)
        edits.forEach { it.addTextChangedListener(watcher) }
    }

    private fun validateStep(step: Int): Boolean {
        var valid = true
        when (step) {
            1 -> {
                if (gymIdEdit.text.isNullOrEmpty()) { gymIdLayout.error = "Required"; valid = false }
                if (userRegEdit.text.isNullOrEmpty() || userRegEdit.text!!.length < 4) { userRegLayout.error = "Min 4 characters"; valid = false }
                val p = passRegEdit.text.toString()
                if (p.length < 8 || !p.any { it.isUpperCase() } || !p.any { it.isDigit() } || !p.any { !it.isLetterOrDigit() }) { passRegLayout.error = "Weak password"; valid = false }
                if (p != confirmPassRegEdit.text.toString()) { confirmPassRegLayout.error = "Mismatch"; valid = false }
            }
            2 -> {
                if (firstNameEdit.text.isNullOrEmpty()) { firstNameLayout.error = "Required"; valid = false }
                if (lastNameEdit.text.isNullOrEmpty()) { lastNameLayout.error = "Required"; valid = false }
                if (birthDateEdit.text.isNullOrEmpty()) { birthDateLayout.error = "Required"; valid = false }
                if (sexSpinner.text.isNullOrEmpty()) { sexLayout.error = "Required"; valid = false }
                if (findViewById<View>(R.id.parentalInfoSection).visibility == View.VISIBLE) {
                    if (parentNameEdit.text.isNullOrEmpty()) { parentNameLayout.error = "Required for minors"; valid = false }
                    if (parentPhoneEdit.text.isNullOrEmpty() || parentPhoneEdit.text!!.length < 13) { parentPhoneLayout.error = "Invalid phone"; valid = false }
                }
            }
            3 -> {
                if (emailEdit.text.isNullOrEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailEdit.text!!).matches()) { emailLayout.error = "Invalid email"; valid = false }
                if (phoneEdit.text.isNullOrEmpty() || phoneEdit.text!!.length < 13) { phoneNumberLayout.error = "Invalid phone"; valid = false }
                
                if (addressLineEdit.text.isNullOrEmpty()) { addressLineLayout.error = "Required"; valid = false }
                if (barangayEdit.text.isNullOrEmpty()) { barangayLayout.error = "Required"; valid = false }
                if (cityEdit.text.isNullOrEmpty()) { cityLayout.error = "Required"; valid = false }
                if (provinceEdit.text.isNullOrEmpty()) { provinceLayout.error = "Required"; valid = false }
                if (regionEdit.text.isNullOrEmpty()) { regionLayout.error = "Required"; valid = false }
            }
            4 -> {
                if (emergencyNameEdit.text.isNullOrEmpty()) { emergencyNameLayout.error = "Required"; valid = false }
                if (emergencyPhoneEdit.text.isNullOrEmpty() || emergencyPhoneEdit.text!!.length < 13) { emergencyPhoneLayout.error = "Invalid phone"; valid = false }
            }
        }
        return valid
    }

    private fun updateWizardUI() {
        findViewById<View>(R.id.layoutStepAccount).visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutStepPersonal).visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutStepContact).visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        findViewById<View>(R.id.layoutStepHealth).visibility = if (currentStep == 4) View.VISIBLE else View.GONE

        btnPrev.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        btnSpace.visibility = if (currentStep > 1) View.VISIBLE else View.GONE
        btnNext.visibility = if (currentStep < 4) View.VISIBLE else View.GONE
        btnRegister.visibility = if (currentStep == 4) View.VISIBLE else View.GONE
        findViewById<View>(R.id.registerFooter).visibility = if (currentStep == 1) View.VISIBLE else View.GONE

        val titles = arrayOf("Account Access", "Personal Info", "Contact Details", "Health & Emergency")
        findViewById<TextView>(R.id.registerStepIndicator).text = "Step $currentStep of 4: ${titles[currentStep-1]}"
    }

    private fun performRegistration() {
        val data = RegisterRequest(
            firstName = firstNameEdit.text.toString(),
            middleName = middleNameEdit.text.toString().takeIf { it.isNotEmpty() },
            lastName = lastNameEdit.text.toString(),
            email = emailEdit.text.toString(),
            username = userRegEdit.text.toString(),
            password = passRegEdit.text.toString(),
            contactNumber = phoneEdit.text.toString(),
            birthDate = birthDateEdit.text.toString(),
            sex = sexSpinner.text.toString(),
            occupation = occupationEdit.text.toString().takeIf { it.isNotEmpty() },
            addressLine = addressLineEdit.text.toString(),
            barangay = barangayEdit.text.toString(),
            city = cityEdit.text.toString(),
            province = provinceEdit.text.toString(),
            region = regionEdit.text.toString(),
            medicalHistory = medicalHistoryEdit.text.toString().takeIf { it.isNotEmpty() },
            emergencyContactName = emergencyNameEdit.text.toString(),
            emergencyContactNumber = emergencyPhoneEdit.text.toString(),
            parentName = parentNameEdit.text.toString().takeIf { findViewById<View>(R.id.parentalInfoSection).visibility == View.VISIBLE },
            parentContactNumber = parentPhoneEdit.text.toString().takeIf { findViewById<View>(R.id.parentalInfoSection).visibility == View.VISIBLE },
            tenantCode = gymIdEdit.text.toString(),
            registrationSource = "Mobile",
            action = "request_otp" // Set action to request_otp
        )
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@RegisterActivity), GymManager.getBypassUA(this@RegisterActivity))
                val resp = api.register(data)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        Toast.makeText(this@RegisterActivity, "Verification PIN sent to ${data.email}", Toast.LENGTH_LONG).show()
                        // Pass the whole data object to VerifyActivity for the final step
                        val intent = Intent(this@RegisterActivity, VerifyActivity::class.java).apply {
                            putExtra("registration_data", data)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, resp.body()?.message ?: "Registration Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { 
                withContext(Dispatchers.Main) { 
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() 
                } 
            }
        }
    }

    private fun validateTenantCode(code: String, onResult: (Boolean, String?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@RegisterActivity), GymManager.getBypassUA(this@RegisterActivity))
                val resp = api.validateTenant(mapOf("gym_id" to code))
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) onResult(resp.body()?.success ?: false, resp.body()?.message)
                    else onResult(false, "Server Error")
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { onResult(false, "Network Error") } }
        }
    }

    private fun setupPasswordComplexityWatcher() {
        val ac = try { android.graphics.Color.parseColor(GymManager.getThemeColor(this)) } catch(e:Exception) { android.graphics.Color.WHITE }
        val ic = android.graphics.Color.parseColor("#40FFFFFF")
        passRegEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val p = s.toString()
                updateReq(reqLen, p.length >= 8, ac, ic, "Minimum 8 characters")
                updateReq(reqNum, p.any { it.isDigit() }, ac, ic, "At least 1 number")
                updateReq(reqSpecial, p.any { !it.isLetterOrDigit() }, ac, ic, "At least 1 special character")
                updateReq(reqCaps, p.any { it.isUpperCase() }, ac, ic, "At least 1 uppercase letter")
            }
        })
    }

    private fun updateReq(v: TextView, f: Boolean, ac: Int, ic: Int, t: String) {
        v.text = if (f) "✓ $t" else "• $t"; v.setTextColor(if (f) ac else ic)
    }

    private fun setupPhoneFormatting() {
        val watcher = object : TextWatcher {
            private var isRunning = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isRunning || s == null) return
                isRunning = true
                
                val raw = s.toString().replace(Regex("\\D"), "")
                val formatted = StringBuilder()
                for (i in raw.indices) {
                    // PH Format: 09XX-XXX-XXXX
                    if (i == 4 || i == 7) formatted.append("-")
                    formatted.append(raw[i])
                    if (formatted.length >= 13) break
                }
                
                val result = formatted.toString()
                if (s.toString() != result) {
                    val sel = Selection.getSelectionEnd(s)
                    s.replace(0, s.length, result)
                    if (sel <= s.length) Selection.setSelection(s, sel) else Selection.setSelection(s, s.length)
                }
                isRunning = false
            }
        }
        phoneEdit.addTextChangedListener(watcher)
        parentPhoneEdit.addTextChangedListener(watcher)
        emergencyPhoneEdit.addTextChangedListener(watcher)
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 1) {
                    currentStep--
                    updateWizardUI()
                } else {
                    MaterialAlertDialogBuilder(this@RegisterActivity)
                        .setTitle("Exit Registration?")
                        .setMessage("Are you sure you want to exit? Your progress will be saved locally.")
                        .setPositiveButton("Exit") { _, _ -> 
                            saveRegistrationData()
                            finish() 
                        }
                        .setNegativeButton("Stay", null)
                        .show()
                }
            }
        })
    }

    private fun saveRegistrationData() {
        val prefs = getSharedPreferences("reg_cache", MODE_PRIVATE)
        prefs.edit().apply {
            putInt("step", currentStep)
            putString("gym_id", gymIdEdit.text.toString())
            putString("username", userRegEdit.text.toString())
            putString("first_name", firstNameEdit.text.toString())
            putString("last_name", lastNameEdit.text.toString())
            putString("middle_name", middleNameEdit.text.toString())
            putString("email", emailEdit.text.toString())
            putString("phone", phoneEdit.text.toString())
            putString("birth_date", birthDateEdit.text.toString())
            putString("address", addressLineEdit.text.toString())
            putString("barangay", barangayEdit.text.toString())
            putString("city", cityEdit.text.toString())
            putString("province", provinceEdit.text.toString())
            putString("region", regionEdit.text.toString())
            putString("sex", sexSpinner.text.toString())
            putString("occupation", occupationEdit.text.toString())
            putString("medical_history", medicalHistoryEdit.text.toString())
            putString("emergency_name", emergencyNameEdit.text.toString())
            putString("emergency_phone", emergencyPhoneEdit.text.toString())
            apply()
        }
    }

    private fun restoreRegistrationData() {
        val prefs = getSharedPreferences("reg_cache", MODE_PRIVATE)
        if (prefs.contains("step")) {
            currentStep = prefs.getInt("step", 1)
            gymIdEdit.setText(prefs.getString("gym_id", ""))
            userRegEdit.setText(prefs.getString("username", ""))
            firstNameEdit.setText(prefs.getString("first_name", ""))
            lastNameEdit.setText(prefs.getString("last_name", ""))
            middleNameEdit.setText(prefs.getString("middle_name", ""))
            emailEdit.setText(prefs.getString("email", ""))
            phoneEdit.setText(prefs.getString("phone", ""))
            birthDateEdit.setText(prefs.getString("birth_date", "2000-01-01"))
            addressLineEdit.setText(prefs.getString("address", ""))
            barangayEdit.setText(prefs.getString("barangay", ""))
            cityEdit.setText(prefs.getString("city", ""))
            provinceEdit.setText(prefs.getString("province", ""))
            regionEdit.setText(prefs.getString("region", ""))
            sexSpinner.setText(prefs.getString("sex", ""), false)
            occupationEdit.setText(prefs.getString("occupation", ""))
            medicalHistoryEdit.setText(prefs.getString("medical_history", ""))
            emergencyNameEdit.setText(prefs.getString("emergency_name", ""))
            emergencyPhoneEdit.setText(prefs.getString("emergency_phone", ""))
        }
    }

    private fun clearRegistrationCache() {
        getSharedPreferences("reg_cache", MODE_PRIVATE).edit().clear().apply()
    }

    override fun onPause() {
        super.onPause()
        saveRegistrationData()
    }

    private fun applyDynamicColors() {
        try {
            val themeColor = GymManager.getThemeColor(this)
            val bgColor = GymManager.getBgColor(this)
            
            if (!themeColor.isNullOrEmpty()) {
                val color = android.graphics.Color.parseColor(themeColor)
                val csl = android.content.res.ColorStateList.valueOf(color)
                
                findViewById<TextView>(R.id.registerStepIndicator)?.setTextColor(color)
                findViewById<TextView>(R.id.btnSignBack)?.setTextColor(color)
                btnNext.backgroundTintList = csl
                btnRegister.backgroundTintList = csl
            }
            
            if (!bgColor.isNullOrEmpty()) {
                val bg = android.graphics.Color.parseColor(bgColor)
                findViewById<View>(R.id.rootLayout)?.setBackgroundColor(bg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
