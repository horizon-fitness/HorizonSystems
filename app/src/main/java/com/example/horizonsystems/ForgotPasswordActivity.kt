package com.example.horizonsystems

import android.os.Bundle
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private var currentStep = 1
    private lateinit var layoutStepEmail: LinearLayout
    private lateinit var layoutStepOTP: LinearLayout
    private lateinit var layoutStepReset: LinearLayout
    private lateinit var stepIndicator: TextView
    private lateinit var btnNext: MaterialButton
    private lateinit var btnResetPass: MaterialButton
    private lateinit var otpBoxes: List<EditText>
    private lateinit var gymLogoContainer: com.google.android.material.card.MaterialCardView
    private lateinit var forgotGymLogo: android.widget.ImageView
    
    // Layouts (Material TextInputLayout)
    private lateinit var recoveryEmailLayout: TextInputLayout
    private lateinit var newPassLayout: TextInputLayout
    private lateinit var confirmNewPassLayout: TextInputLayout

    private lateinit var recoveryEmailEdit: TextInputEditText
    private lateinit var tenantCodeResetEdit: TextInputEditText
    private lateinit var newPassEdit: TextInputEditText
    private lateinit var confirmNewPassEdit: TextInputEditText
    
    private lateinit var tenantCodeResetLayout: TextInputLayout
    private lateinit var forgotSubtitle: TextView

    // Resend UI
    private lateinit var btnResendOtp: TextView
    private lateinit var txtResendTimer: TextView
    private var resendTimer: android.os.CountDownTimer? = null
    
    // State
    private var userId: Int = -1
    private var verifiedOtp: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Bind layouts
        recoveryEmailLayout = findViewById(R.id.recoveryEmailLayout)
        newPassLayout = findViewById(R.id.newPassLayout)
        confirmNewPassLayout = findViewById(R.id.confirmNewPassLayout)

        // Bind edits
        recoveryEmailEdit = findViewById(R.id.recoveryEmailEdit)
        tenantCodeResetEdit = findViewById(R.id.tenantCodeResetEdit)
        newPassEdit = findViewById(R.id.newPassEdit)
        confirmNewPassEdit = findViewById(R.id.confirmNewPassEdit)
        
        tenantCodeResetLayout = findViewById(R.id.tenantCodeResetLayout)

        layoutStepEmail = findViewById(R.id.layoutStepEmail)
        layoutStepOTP = findViewById(R.id.layoutStepOTP)
        layoutStepReset = findViewById(R.id.layoutStepReset)
        stepIndicator = findViewById(R.id.stepIndicator)
        btnNext = findViewById(R.id.btnNext)
        btnResetPass = findViewById(R.id.btnResetPass)
        forgotSubtitle = findViewById(R.id.forgotSubtitle)
        
        gymLogoContainer = findViewById(R.id.gymLogoContainer)
        forgotGymLogo = findViewById(R.id.forgotGymLogo)
        
        btnResendOtp = findViewById(R.id.btnResendOtp)
        txtResendTimer = findViewById(R.id.txtResendTimer)
        
        btnResendOtp.setOnClickListener { handleResendOtp() }

        otpBoxes = listOf(
            findViewById(R.id.otp1), findViewById(R.id.otp2), findViewById(R.id.otp3),
            findViewById(R.id.otp4), findViewById(R.id.otp5), findViewById(R.id.otp6)
        )

        setupOtpInputs()
        setupErrorClearing()

        findViewById<View>(R.id.btnBack).setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepVisibility()
            } else finish()
        }

        btnNext.setOnClickListener { handleNextStep() }
        btnResetPass.setOnClickListener { handlePasswordReset() }

        setupTenantCodeFilter()
        applyDynamicColors()
        checkGymContext()
    }
    
    private fun checkGymContext() {
        val currentCode = GymManager.getTenantCode(this)
        val currentLogo = GymManager.getGymLogo(this)
        
        // Strictly show only if no valid connection
        val notConnected = currentCode.isNullOrEmpty() || currentCode == "000" || currentCode == "horizon" || currentCode == "default"
        tenantCodeResetLayout.visibility = if (notConnected) View.VISIBLE else View.GONE
        
        // Header polish
        if (!notConnected && !currentLogo.isNullOrEmpty()) {
            gymLogoContainer.visibility = View.VISIBLE
            forgotSubtitle.visibility = View.GONE // Remove "Experience the difference" when connected
            GymManager.loadLogo(this, currentLogo, forgotGymLogo)
        } else {
            gymLogoContainer.visibility = View.GONE
            forgotSubtitle.visibility = View.VISIBLE
        }
    }
    
    private fun setupTenantCodeFilter() {
        tenantCodeResetEdit.filters = arrayOf(android.text.InputFilter { source, start, end, dest, dstart, dend ->
            if (start == end) return@InputFilter null
            val added = source.subSequence(start, end).toString().uppercase()
            val currentDest = java.lang.StringBuilder(dest.subSequence(0, dstart))
            val sb = java.lang.StringBuilder()
            for (i in added.indices) {
                val c = added[i]
                val len = currentDest.length
                if (len < 3) { if (c.isLetter()) { currentDest.append(c); sb.append(c) } }
                else if (len == 3) {
                    if (c == '-') { currentDest.append(c); sb.append(c) }
                    else if (c.isDigit()) { currentDest.append("-").append(c); sb.append("-").append(c) }
                } else { if (c.isDigit() && currentDest.length < 8) { currentDest.append(c); sb.append(c) } }
            }
            val suffix = dest.subSequence(dend, dest.length)
            val finalStr = currentDest.toString() + suffix
            if (finalStr.matches(Regex("^([A-Z]{0,3}|[A-Z]{3}-\\d{0,4})$"))) sb.toString() else ""
        })

        // Fix for the blinking cursor (Selection) jumping to the start
        tenantCodeResetEdit.addTextChangedListener(object : TextWatcher {
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
    }

    private fun setupErrorClearing() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recoveryEmailLayout.error = null
                newPassLayout.error = null
                confirmNewPassLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        recoveryEmailEdit.addTextChangedListener(watcher)
        tenantCodeResetEdit.addTextChangedListener(watcher)
        newPassEdit.addTextChangedListener(watcher)
        confirmNewPassEdit.addTextChangedListener(watcher)
    }

    private fun applyDynamicColors() {
        val themeColor = GymManager.getThemeColor(this)
        val bgColor = GymManager.getBgColor(this)
        try {
            val color = android.graphics.Color.parseColor(themeColor)
            val bg = android.graphics.Color.parseColor(bgColor)
            val colorStateList = android.content.res.ColorStateList.valueOf(color)
            findViewById<View>(R.id.rootLayout)?.setBackgroundColor(bg)
            findViewById<TextView>(R.id.stepIndicator)?.setTextColor(color)
            findViewById<MaterialButton>(R.id.btnNext)?.backgroundTintList = colorStateList
            findViewById<MaterialButton>(R.id.btnResetPass)?.backgroundTintList = colorStateList
            btnResendOtp.setTextColor(color)
        } catch (e: Exception) {}
    }

    private fun setupOtpInputs() {
        otpBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < otpBoxes.size - 1) otpBoxes[index + 1].requestFocus()
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
                val email = recoveryEmailEdit.text.toString().trim()
                if (email.isEmpty()) {
                    recoveryEmailLayout.error = "Please enter your email"
                    return
                }
                btnNext.isEnabled = false
                btnNext.text = "Validating..."
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@ForgotPasswordActivity), GymManager.getBypassUA(this@ForgotPasswordActivity))
                        
                        // Use manual code if visible, else use saved code
                        val tenantCode = if (tenantCodeResetLayout.visibility == View.VISIBLE) {
                            tenantCodeResetEdit.text.toString().trim()
                        } else {
                            GymManager.getTenantCode(this@ForgotPasswordActivity)
                        }
                        
                        if (tenantCode.isEmpty() && tenantCodeResetLayout.visibility == View.VISIBLE) {
                            withContext(Dispatchers.Main) {
                                btnNext.isEnabled = true
                                btnNext.text = "Next Step"
                                tenantCodeResetLayout.error = "Gym Code required"
                            }
                            return@launch
                        }

                        val request = mapOf("action" to "request_otp", "email" to email, "tenant_code" to tenantCode)
                        val response = api.forgotPasswordAction(request)
                        withContext(Dispatchers.Main) {
                            btnNext.isEnabled = true
                            btnNext.text = "Next Step"
                            if (response.isSuccessful && response.body()?.success == true) {
                                userId = response.body()?.userId ?: -1
                                currentStep = 2
                                updateStepVisibility()
                                startResendTimer()
                            } else {
                                recoveryEmailLayout.error = response.body()?.message ?: "Account not found"
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            btnNext.isEnabled = true
                            btnNext.text = "Next Step"
                            Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            2 -> {
                val pin = otpBoxes.joinToString("") { it.text.toString() }
                if (pin.length != 6) { Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show(); return }
                btnNext.isEnabled = false
                btnNext.text = "Verifying..."
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@ForgotPasswordActivity), GymManager.getBypassUA(this@ForgotPasswordActivity))
                        val request = mapOf("action" to "verify_otp", "user_id" to userId, "otp" to pin)
                        val response = api.forgotPasswordAction(request)
                        withContext(Dispatchers.Main) {
                            btnNext.isEnabled = true
                            btnNext.text = "Next Step"
                            if (response.isSuccessful && response.body()?.success == true) {
                                verifiedOtp = pin
                                currentStep = 3
                                updateStepVisibility()
                            } else Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "Invalid PIN", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { btnNext.isEnabled = true; btnNext.text = "Next Step"; Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
    }

    private fun handlePasswordReset() {
        val newPass = newPassEdit.text.toString()
        val confirmPass = confirmNewPassEdit.text.toString()
        if (newPass.isEmpty()) { newPassLayout.error = "Required"; return }
        if (confirmPass.isEmpty()) { confirmNewPassLayout.error = "Required"; return }
        if (newPass != confirmPass) { confirmNewPassLayout.error = "Passwords do not match"; return }

        btnResetPass.isEnabled = false
        btnResetPass.text = "Updating..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@ForgotPasswordActivity), GymManager.getBypassUA(this@ForgotPasswordActivity))
                val request = mapOf("action" to "update_password", "user_id" to userId, "otp" to verifiedOtp, "password" to newPass)
                val response = api.forgotPasswordAction(request)
                withContext(Dispatchers.Main) {
                    btnResetPass.isEnabled = true
                    btnResetPass.text = "Reset Password"
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ForgotPasswordActivity, "Password Updated!", Toast.LENGTH_LONG).show()
                        finish()
                    } else Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "Failed", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { btnResetPass.isEnabled = true; btnResetPass.text = "Reset Password"; Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun updateStepVisibility() {
        layoutStepEmail.visibility = if (currentStep == 1) View.VISIBLE else View.GONE
        layoutStepOTP.visibility = if (currentStep == 2) View.VISIBLE else View.GONE
        layoutStepReset.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        btnNext.visibility = if (currentStep < 3) View.VISIBLE else View.GONE
        btnResetPass.visibility = if (currentStep == 3) View.VISIBLE else View.GONE
        
        val stepName = when(currentStep) {
            1 -> "Identity Verification"
            2 -> "PIN Verification"
            3 -> "Secure Your Account"
            else -> ""
        }
        stepIndicator.text = "Step $currentStep of 3: $stepName"
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        btnResendOtp.isClickable = false
        btnResendOtp.alpha = 0.3f
        resendTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) { txtResendTimer.text = "You can resend in ${m / 1000}s" }
            override fun onFinish() { btnResendOtp.isClickable = true; btnResendOtp.alpha = 1.0f; txtResendTimer.text = "You can now resend the code" }
        }.start()
    }

    private fun handleResendOtp() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@ForgotPasswordActivity), GymManager.getBypassUA(this@ForgotPasswordActivity))
                val request = mapOf("action" to "request_otp", "email" to recoveryEmailEdit.text.toString().trim(), "tenant_code" to GymManager.getTenantCode(this@ForgotPasswordActivity))
                val response = api.forgotPasswordAction(request)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ForgotPasswordActivity, "New OTP sent!", Toast.LENGTH_SHORT).show()
                        otpBoxes.forEach { it.setText("") }; otpBoxes[0].requestFocus(); startResendTimer()
                    } else Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    override fun onDestroy() { resendTimer?.cancel(); super.onDestroy() }
}
