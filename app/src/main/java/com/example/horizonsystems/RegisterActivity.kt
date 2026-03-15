package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
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

        val firstNameEdit = findViewById<TextInputEditText>(R.id.firstNameEdit)
        val lastNameEdit = findViewById<TextInputEditText>(R.id.lastNameEdit)
        val emailEdit = findViewById<TextInputEditText>(R.id.emailEdit)
        val userRegEdit = findViewById<TextInputEditText>(R.id.userRegEdit)
        val passRegEdit = findViewById<TextInputEditText>(R.id.passRegEdit)
        val gymIdEdit = findViewById<TextInputEditText>(R.id.gymIdEdit)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val first = firstNameEdit.text.toString()
            val last = lastNameEdit.text.toString()
            val email = emailEdit.text.toString()
            val user = userRegEdit.text.toString()
            val pass = passRegEdit.text.toString()
            val gymIdStr = gymIdEdit.text.toString()

            if (first.isEmpty() || last.isEmpty() || email.isEmpty() || user.isEmpty() || pass.isEmpty() || gymIdStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gymId = gymIdStr.toIntOrNull() ?: 1
            performRegistration(user, email, pass, first, last, gymId)
        }
    }

    private fun performRegistration(user: String, email: String, pass: String, first: String, last: String, gymId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi("", "")
                val response = api.register(user, email, pass, first, last, gymId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val regResponse = response.body()
                        if (regResponse?.status == "success") {
                            Toast.makeText(this@RegisterActivity, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
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
                    Log.e("AuthError", "Register Error", e)
                    Toast.makeText(this@RegisterActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
