package com.example.horizonsystems

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {

    private var currentStep = 1
    private lateinit var layoutStepEmail: LinearLayout
    private lateinit var layoutStepOTP: LinearLayout
    private lateinit var layoutStepReset: LinearLayout
    private lateinit var stepIndicator: TextView
    private lateinit var btnNext: MaterialButton
    private lateinit var btnResetPass: MaterialButton
    private lateinit var otpBoxes: List<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Bind Views
        layoutStepEmail = findViewById(R.id.layoutStepEmail)
        layoutStepOTP = findViewById(R.id.layoutStepOTP)
        layoutStepReset = findViewById(R.id.layoutStepReset)
        stepIndicator = findViewById(R.id.stepIndicator)
        btnNext = findViewById(R.id.btnNext)
        btnResetPass = findViewById(R.id.btnResetPass)

        otpBoxes = listOf(
            findViewById(R.id.otp1),
            findViewById(R.id.otp2),
            findViewById(R.id.otp3),
            findViewById(R.id.otp4),
            findViewById(R.id.otp5),
            findViewById(R.id.otp6)
        )

        setupOtpInputs()

        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepVisibility()
            } else {
                finish()
            }
        }

        btnNext.setOnClickListener {
            handleNextStep()
        }

        btnResetPass.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun setupOtpInputs() {
        otpBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < otpBoxes.size - 1) {
                        otpBoxes[index + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        otpBoxes[index - 1].requestFocus()
                        otpBoxes[index - 1].setText("")
                        true
                    } else false
                } else false
            }
        }
    }

    private fun handleNextStep() {
        when (currentStep) {
            1 -> {
                val email = findViewById<TextInputEditText>(R.id.recoveryEmailEdit).text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return
                }
                // Placeholder for reset_password_request API
                currentStep = 2
            }
            2 -> {
                val pin = otpBoxes.joinToString("") { it.text.toString() }
                if (pin.length != 6) {
                    Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show()
                    return
                }
                // Placeholder for verify_reset_pin API
                currentStep = 3
            }
        }
        updateStepVisibility()
    }

    private fun handlePasswordReset() {
        val newPass = findViewById<TextInputEditText>(R.id.newPassEdit).text.toString()
        val confirmPass = findViewById<TextInputEditText>(R.id.confirmNewPassEdit).text.toString()

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Placeholder for update_password API
        Toast.makeText(this, "Password Reset Successfully!", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun updateStepVisibility() {
        layoutStepEmail.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutStepOTP.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        layoutStepReset.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        btnNext.visibility = if (currentStep < 3) View.VISIBLE else View.GONE
        btnResetPass.visibility = if (currentStep == 3) View.VISIBLE else View.GONE

        stepIndicator.text = "Step $currentStep of 3"
    }
}
