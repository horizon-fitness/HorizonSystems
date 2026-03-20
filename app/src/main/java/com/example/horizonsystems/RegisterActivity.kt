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
import com.example.horizonsystems.models.RegisterRequest
import com.example.horizonsystems.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Setup Sex Dropdown
        val sexOptions = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sexOptions)
        val sexSpinner = findViewById<AutoCompleteTextView>(R.id.sexSpinner)
        sexSpinner.setAdapter(adapter)

        val firstNameEdit = findViewById<TextInputEditText>(R.id.firstNameEdit)
        val lastNameEdit = findViewById<TextInputEditText>(R.id.lastNameEdit)
        val middleNameEdit = findViewById<TextInputEditText>(R.id.middleNameEdit)
        val emailEdit = findViewById<TextInputEditText>(R.id.emailEdit)
        val userRegEdit = findViewById<TextInputEditText>(R.id.userRegEdit)
        val passRegEdit = findViewById<TextInputEditText>(R.id.passRegEdit)
        val gymIdEdit = findViewById<TextInputEditText>(R.id.gymIdEdit)
        val birthDateEdit = findViewById<TextInputEditText>(R.id.birthDateEdit)
        val occupationEdit = findViewById<TextInputEditText>(R.id.occupationEdit)
        val addressEdit = findViewById<TextInputEditText>(R.id.addressEdit)
        val phoneEdit = findViewById<TextInputEditText>(R.id.phoneNumberEdit)
        val medicalEdit = findViewById<TextInputEditText>(R.id.medicalHistoryEdit)

        val emergencyNameEdit = findViewById<TextInputEditText>(R.id.emergencyNameEdit)
        val emergencyPhoneEdit = findViewById<TextInputEditText>(R.id.emergencyPhoneEdit)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        // Pre-fill tenant code from the current tenant
        val currentTenantCode = com.example.horizonsystems.utils.GymManager.getTenantCode(this)
        gymIdEdit.setText(currentTenantCode)

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

            if (first.isEmpty() || last.isEmpty() || email.isEmpty() || user.isEmpty() || pass.isEmpty() || gymIdStr.isEmpty() || eName.isEmpty() || ePhone.isEmpty()) {
                Toast.makeText(this, "Please fill required fields (Name, Email, Account, Emergency)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performRegistration(user, email, pass, first, middle, last, phone, birth, sex, occupation, address, medical, eName, ePhone, gymIdStr)
        }
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
