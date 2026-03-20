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

import android.widget.TextView
import com.example.horizonsystems.utils.GymManager
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.textfield.TextInputLayout

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

        // Setup Date Picker
        birthDateEdit.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePickerDialog = android.app.DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    birthDateEdit.setText(formattedDate)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

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

        applyBranding()
    }

    private fun performRegistration(
        user: String, email: String, pass: String, first: String, middle: String, last: String,
        phone: String, birth: String, sex: String, occupation: String, address: String,
        medical: String, eName: String, ePhone: String, gymId: String
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
                val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(this@RegisterActivity)
                val ua = com.example.horizonsystems.utils.GymManager.getBypassUA(this@RegisterActivity)

                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.register(registrationData)


                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val regResponse = response.body()
                        if (regResponse?.success == true) {
                            Toast.makeText(this@RegisterActivity, regResponse.message, Toast.LENGTH_LONG).show()
                            val intent = Intent(this@RegisterActivity, VerifyActivity::class.java)
                            intent.putExtra("user_id", regResponse.userId)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, regResponse?.message ?: "Registration Failed", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val cookie = com.example.horizonsystems.utils.GymManager.getBypassCookie(this@RegisterActivity)
                    if (cookie.isEmpty()) {
                        Toast.makeText(this@RegisterActivity, "Security check not ready. Please return to Landing and wait a moment.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e("RegisterError", "Error", e)
                        Toast.makeText(this@RegisterActivity, "Connection Error: Check internet or try again.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun applyBranding() {
        val themeColorStr = GymManager.getThemeColor(this)
        val bgColorStr = GymManager.getBgColor(this)

        try {
            val themeColor = Color.parseColor(themeColorStr)
            val bgColor = Color.parseColor(bgColorStr)
            val colorStateList = ColorStateList.valueOf(themeColor)

            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)
                .setBackgroundColor(bgColor)

            findViewById<MaterialButton>(R.id.btnRegister)
                .backgroundTintList = colorStateList

            // Style sections
            findViewById<TextView>(R.id.sectionAccount).setTextColor(themeColor)
            findViewById<TextView>(R.id.sectionPersonal).setTextColor(themeColor)
            findViewById<TextView>(R.id.sectionEmergency).setTextColor(themeColor)

            // Style all TextInputLayouts
            val root = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)
            styleInputLayouts(root, themeColor, colorStateList)

        } catch (e: Exception) {
            Log.e("BrandingError", "Failed to apply branding", e)
        }
    }

    private fun styleInputLayouts(view: android.view.View, themeColor: Int, colorStateList: ColorStateList) {
        if (view is TextInputLayout) {
            view.boxStrokeColor = themeColor
            view.hintTextColor = colorStateList
        } else if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                styleInputLayouts(view.getChildAt(i), themeColor, colorStateList)
            }
        }
    }
}
