package com.example.horizonsystems

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

        // Pre-fill gym ID from the current tenant
        val currentGymId = com.example.horizonsystems.utils.GymManager.getGymId(this)
        gymIdEdit.setText(currentGymId.toString())

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

            val gymId = gymIdStr.toIntOrNull() ?: 1
            performRegistration(user, email, pass, first, middle, last, phone, birth, sex, occupation, address, medical, eName, ePhone, gymId)
        }
    }

    private fun performRegistration(
        user: String, email: String, pass: String, first: String, middle: String, last: String,
        phone: String, birth: String, sex: String, occupation: String, address: String, 
        medical: String, eName: String, ePhone: String, gymId: Int
    ) {
        val registrationData = com.example.horizonsystems.models.RegisterRequest(
            firstName = first,
            lastName = last,
            email = email,
            username = user,
            password = pass,
            phoneNumber = phone,
            gymId = gymId
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi("", "")
                val response = api.register(registrationData)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val regResponse = response.body()
                        if (regResponse?.success == true) {
                            Toast.makeText(this@RegisterActivity, "Registration Successful! PIN sent to email.", Toast.LENGTH_LONG).show()
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
