package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ImageView
import android.content.Intent
import com.example.horizonsystems.utils.NetworkBypass
import com.bumptech.glide.Glide

// CameraX & ML Kit
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SwitchGymActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var isScanning = true
    private val CAMERA_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_switch_gym)
        cameraExecutor = Executors.newSingleThreadExecutor()

        updateCurrentGymUI()

        val tenantCodeEdit = findViewById<EditText>(R.id.tenantCodeEdit)
        val gymLinkEdit = findViewById<EditText>(R.id.gymLinkEdit)

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Strictly Fixed Formatting (LLL-NNNN)
        tenantCodeEdit.hint = "COR-9820"
        tenantCodeEdit.filters = arrayOf(android.text.InputFilter.AllCaps(), android.text.InputFilter.LengthFilter(8))
        tenantCodeEdit.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            private var isInternal = false
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isInternal) return
                isInternal = true
                val original = s.toString()
                val cleaned = original.replace("-", "")
                
                var formatted = ""
                for (i in cleaned.indices) {
                    formatted += cleaned[i]
                    if (i == 2 && cleaned.length > 3) {
                        formatted += "-"
                    }
                }
                
                if (original != formatted) {
                    s?.replace(0, s.length, formatted)
                }
                isInternal = false
            }
        })

        findViewById<MaterialButton>(R.id.btnConnectCode).setOnClickListener {
            val code = tenantCodeEdit.text.toString().trim()
            if (code.isNotEmpty()) {
                switchGym(code)
            } else {
                Toast.makeText(this, "Please enter a tenant code", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.btnConnectLink).setOnClickListener {
            val link = gymLinkEdit.text.toString().trim()
            if (link.isNotEmpty()) {
                val slug = extractSlugFromLink(link)
                switchGym(slug)
            } else {
                Toast.makeText(this, "Please enter a gym link", Toast.LENGTH_SHORT).show()
            }
        }

        // Disconnect button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDisconnectGym)?.setOnClickListener {
            // Reset to global default (no gym)
            GymManager.saveGymData(
                this, 
                "horizon", 
                1, 
                "000", 
                "HORIZON SYSTEMS", 
                null,
                "#8c2bee", // Default Purple
                "#0a090d"  // Default Dark BG
            )
            Toast.makeText(this, "Disconnected from gym", Toast.LENGTH_SHORT).show()
            updateCurrentGymUI()
            // Card will auto-hide since isDefault becomes true
        }

        checkCameraPermission()
    }

    private fun updateCurrentGymUI() {
        val gymNameTxt = findViewById<TextView>(R.id.currentGymName)
        val gymLogoImg = findViewById<ImageView>(R.id.currentGymLogo)
        
        val currentName = GymManager.getGymName(this)
        val currentLogo = GymManager.getGymLogo(this)
        
        // Hide connection card if it's just the global default branding (Horizon Systems)
        val currentSlug = GymManager.getGymSlug(this)
        val isDefault = currentName.equals("HORIZON SYSTEMS", ignoreCase = true) || currentSlug == "horizon" || currentSlug.isNullOrEmpty()
        findViewById<View>(R.id.currentGymCard).visibility = if (isDefault) View.GONE else View.VISIBLE
        
        gymNameTxt.text = currentName.uppercase()
        
        if (!currentLogo.isNullOrEmpty()) {
            GymManager.loadLogo(this, currentLogo, gymLogoImg)
            applyDynamicColors()
        } else {
            gymLogoImg.setImageDrawable(null)
            gymLogoImg.setPadding(0, 0, 0, 0)
            gymLogoImg.scaleType = ImageView.ScaleType.CENTER_INSIDE
            gymLogoImg.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)
            )
        }
    }

    private fun applyDynamicColors() {
        val themeColor = GymManager.getThemeColor(this)
        try {
            val color = android.graphics.Color.parseColor(themeColor)
            val colorStateList = android.content.res.ColorStateList.valueOf(color)
            
            findViewById<ImageView>(R.id.statusIcon)?.imageTintList = colorStateList
            findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDisconnectGym)?.setTextColor(color)
        } catch (e: Exception) {
            Log.e("SwitchGymActivity", "Error applying theme color: $themeColor", e)
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required for QR scanning", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val previewView = findViewById<PreviewView>(R.id.previewView)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("SwitchGym", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue ?: continue
                        handleScannedValue(rawValue)
                    }
                }
                .addOnFailureListener {
                    Log.e("SwitchGym", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedValue(rawValue: String) {
        if (!isScanning) return
        
        Log.d("SwitchGym", "Detected: $rawValue")
        
        var targetCode: String? = null

        if (rawValue.contains("horizon://connect")) {
            val uri = Uri.parse(rawValue)
            targetCode = uri.getQueryParameter("tenant_code")
        } else if (rawValue.contains("gt.tc/portal.php")) {
            val uri = Uri.parse(rawValue)
            targetCode = uri.getQueryParameter("gym") ?: uri.getQueryParameter("tenant_code")
        } else if (rawValue.uppercase().matches(Regex("[A-Z0-9]{3}-?[A-Z0-9]{4,5}"))) {
            targetCode = rawValue.uppercase()
        }

        if (targetCode != null) {
            isScanning = false // Stop processing new frames
            runOnUiThread {
                Toast.makeText(this, "Code Detected: $targetCode", Toast.LENGTH_SHORT).show()
                switchGym(targetCode)
            }
        }
    }

    private fun extractSlugFromLink(link: String): String {
        return try {
            val uri = Uri.parse(link)
            val gymParam = uri.getQueryParameter("gym") ?: uri.getQueryParameter("tenant_code")
            if (gymParam != null) return gymParam
            
            // Fallback to last path segment if no query param
            val path = uri.path ?: ""
            if (path.contains("portal.php") || path.endsWith("/")) {
                 uri.lastPathSegment ?: link
            } else {
                 uri.lastPathSegment ?: link
            }
            uri.lastPathSegment ?: link
        } catch (e: Exception) {
            link
        }
    }

    private fun switchGym(
        slug: String,
        isRetry: Boolean = false,
        forcedCookie: String? = null,
        forcedUA: String? = null
    ) {
        val cookie = forcedCookie ?: GymManager.getBypassCookie(this)
        val ua = forcedUA ?: GymManager.getBypassUA(this)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.VISIBLE
            }
            
            try {
                val api = RetrofitClient.getApi(cookie, ua)
                val response = api.getTenantInfo(slug)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val tenant = response.body()
                        if (tenant != null && tenant.success != false) {
                            // Persist selection for global app use
                            GymManager.saveGymData(
                                this@SwitchGymActivity,
                                tenant.pageSlug ?: slug,
                                tenant.gymId ?: 0,
                                tenant.tenantCode ?: slug,
                                tenant.gymName ?: "Gym",
                                tenant.logoPath,
                                tenant.themeColor,
                                tenant.bgColor
                            )
                            Toast.makeText(this@SwitchGymActivity, "Connected to ${tenant.gymName ?: "Gym"}", Toast.LENGTH_SHORT).show()
                            updateCurrentGymUI()
                            
                            // Delay slightly so the user sees the "Connected" state and branding
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                // Explicitly go back to Landing (Login) screen
                                val intent = Intent(this@SwitchGymActivity, LandingActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }, 1500)
                        } else {
                            isScanning = true
                            val msg = tenant?.message ?: "Gym not found. Please verify the code."
                            Toast.makeText(this@SwitchGymActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    } else if (response.code() == 404) {
                        isScanning = true
                        val errorString = response.errorBody()?.string() ?: ""
                        val msg = try {
                            val json = com.google.gson.JsonParser.parseString(errorString).asJsonObject
                            json.get("message").asString
                        } catch (e: Exception) {
                            "Gym not found ($slug)"
                        }
                        Toast.makeText(this@SwitchGymActivity, msg, Toast.LENGTH_LONG).show()
                    } else if (response.code() == 403 && !isRetry) {
                         refreshSecurityAndRetry(slug)
                    } else {
                        isScanning = true
                        Toast.makeText(this@SwitchGymActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                val isParsingError = e is IllegalStateException || e is com.google.gson.JsonSyntaxException || e.message?.contains("Expected BEGIN_OBJECT") == true
                
                if (isParsingError && !isRetry) {
                    refreshSecurityAndRetry(slug)
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        isScanning = true
                        val errorMsg = if (isParsingError) "Security check failed. Please restart." else "Connection Error: ${e.message}"
                        Toast.makeText(this@SwitchGymActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        Log.e("SwitchGymActivity", "Error switching gym", e)
                    }
                }
            }
        }
    }

    private fun refreshSecurityAndRetry(slug: String) {
        runOnUiThread {
            Toast.makeText(this, "Refreshing security... Please wait", Toast.LENGTH_SHORT).show()
        }
        NetworkBypass.getSecurityCookie(this, forceRefresh = true) { newCookie, newUA ->
            switchGym(slug, isRetry = true, forcedCookie = newCookie, forcedUA = newUA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
