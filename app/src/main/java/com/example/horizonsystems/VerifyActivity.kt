package com.example.horizonsystems

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.models.RegisterRequest
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerifyActivity : AppCompatActivity() {

    private var registrationData: RegisterRequest? = null
    private lateinit var btnVerify: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var gymLogoContainer: com.google.android.material.card.MaterialCardView
    private lateinit var verifyGymLogo: ImageView
    private lateinit var btnResendPin: TextView
    private lateinit var txtResendTimer: TextView
    private lateinit var txtTargetEmail: TextView
    private lateinit var otpBoxes: List<EditText>
    
    private var resendTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verify)

        registrationData = intent.getSerializableExtra("registration_data") as? RegisterRequest
        
        if (registrationData == null) {
            Toast.makeText(this, "Session Expired. Please try again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupOtpInputs(otpBoxes)
        setupListeners()
        applyDynamicColors()
        setupBackNavigation()

        // Display targeting email
        txtTargetEmail.text = registrationData?.email ?: "your email"
        
        // Start initial timer
        startResendTimer()
    }

    private fun bindViews() {
        btnVerify = findViewById(R.id.btnVerify)
        btnBack = findViewById(R.id.btnBack)
        gymLogoContainer = findViewById(R.id.gymLogoContainer)
        verifyGymLogo = findViewById(R.id.verifyGymLogo)
        btnResendPin = findViewById(R.id.btnResendPin)
        txtResendTimer = findViewById(R.id.txtResendTimer)
        txtTargetEmail = findViewById(R.id.txtTargetEmail)

        otpBoxes = listOf(
            findViewById(R.id.otp1), findViewById(R.id.otp2), findViewById(R.id.otp3),
            findViewById(R.id.otp4), findViewById(R.id.otp5), findViewById(R.id.otp6)
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { handleBackNavigation() }
        
        btnVerify.setOnClickListener {
            val pinInput = otpBoxes.joinToString("") { it.text.toString() }
            if (pinInput.length != 6) {
                Toast.makeText(this, "Enter 6-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyAndRegister(pinInput)
        }
        
        btnResendPin.setOnClickListener { handleResendPin() }
    }

    private fun handleResendPin() {
        val data = registrationData?.copy(action = "request_otp") ?: return
        
        btnResendPin.isClickable = false
        btnResendPin.alpha = 0.3f
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(GymManager.getBypassCookie(this@VerifyActivity), GymManager.getBypassUA(this@VerifyActivity))
                val resp = api.register(data)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        Toast.makeText(this@VerifyActivity, "New PIN sent!", Toast.LENGTH_SHORT).show()
                        startResendTimer()
                    } else {
                        btnResendPin.isClickable = true
                        btnResendPin.alpha = 1.0f
                        Toast.makeText(this@VerifyActivity, resp.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnResendPin.isClickable = true
                    btnResendPin.alpha = 1.0f
                    Toast.makeText(this@VerifyActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        btnResendPin.isClickable = false
        btnResendPin.alpha = 0.3f
        txtResendTimer.visibility = View.VISIBLE
        
        resendTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) {
                txtResendTimer.text = "You can resend in ${m / 1000}s"
            }
            override fun onFinish() {
                btnResendPin.isClickable = true
                btnResendPin.alpha = 1.0f
                txtResendTimer.text = "You can now resend the code"
            }
        }.start()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun handleBackNavigation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exit Verification?")
            .setMessage("Are you sure you want to go back? You will need to request a new Verification PIN to complete your registration.")
            .setPositiveButton("Go Back") { _, _ ->
                getSharedPreferences("reg_cache", MODE_PRIVATE).edit().putInt("step", 1).apply()
                val intent = Intent(this, RegisterActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupOtpInputs(boxes: List<EditText>) {
        boxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && index < boxes.size - 1) boxes[index + 1].requestFocus()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (editText.text.isEmpty() && index > 0) {
                        boxes[index - 1].requestFocus()
                        boxes[index - 1].setText("")
                        true
                    } else false
                } else false
            }
        }
    }

    private fun verifyAndRegister(pin: String) {
        val finalPayload = registrationData?.copy(
            action = "register",
            pin = pin
        ) ?: return

        btnVerify.isEnabled = false
        btnVerify.text = "Verifying..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getApi(
                    GymManager.getBypassCookie(this@VerifyActivity), 
                    GymManager.getBypassUA(this@VerifyActivity)
                )
                val response = api.register(finalPayload)
                
                withContext(Dispatchers.Main) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify & Continue"
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@VerifyActivity, "Welcome to the Family!", Toast.LENGTH_LONG).show()
                        getSharedPreferences("reg_cache", MODE_PRIVATE).edit().clear().apply()
                        startActivity(Intent(this@VerifyActivity, LandingActivity::class.java).apply {
                            putExtra("SKIP_AUTO_LOGIN", true)
                        })
                        finish()
                    } else {
                        // Clear inputs if wrong code
                        otpBoxes.forEach { it.setText("") }
                        otpBoxes[0].requestFocus()
                        Toast.makeText(this@VerifyActivity, response.body()?.message ?: "Verification Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify & Continue"
                    Toast.makeText(this@VerifyActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyDynamicColors() {
        val themeColor = GymManager.getThemeColor(this)
        val bgColor = GymManager.getBgColor(this)
        val logo = GymManager.getGymLogo(this)

        try {
            if (!themeColor.isNullOrEmpty()) {
                val color = android.graphics.Color.parseColor(themeColor)
                val csl = android.content.res.ColorStateList.valueOf(color)
                btnVerify.backgroundTintList = csl
                btnResendPin.setTextColor(color)
            }
            if (!bgColor.isNullOrEmpty()) {
                val bg = android.graphics.Color.parseColor(bgColor)
                findViewById<View>(R.id.rootLayout)?.setBackgroundColor(bg)
            }
            
            val code = GymManager.getTenantCode(this)
            val isRealGym = !code.isNullOrEmpty() && code != "000" && code != "default" && code != "horizon"
            
            if (isRealGym && !logo.isNullOrEmpty()) {
                gymLogoContainer.visibility = View.VISIBLE
                GymManager.loadLogo(this, logo, verifyGymLogo)
            } else {
                gymLogoContainer.visibility = View.GONE
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
}
