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
        medical: String, eName: String, ePhone: String, gymId: String
    ) {
        val registrationData = RegisterRequest(
            firstName = first,
            lastName = last,
            email = email,
            username = user,
            password = pass,
            phoneNumber = phone,
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
                    Log.e("RegisterError", "Error", e)
                    Toast.makeText(this@RegisterActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}
