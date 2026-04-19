package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val CAMERA_PERMISSION_CODE = 1001
    private var isScanning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupRecyclerView(view)
        applyBranding(view)
        ThemeUtils.applyThemeToView(view)
        
        // Manual Start Launcher
        view.findViewById<View>(R.id.layoutStartScanner)?.setOnClickListener {
            checkCameraPermission(view)
        }
        
        return view
    }

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvAttendanceLogs)
        attendanceAdapter = AttendanceAdapter(emptyList())
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = attendanceAdapter
        
        fetchRealAttendanceLogs(view)
    }

    private fun fetchRealAttendanceLogs(rootView: View) {
        val userId = GymManager.getUserId(requireContext())
        val gymId = GymManager.getTenantId(requireContext())
        
        if (userId == -1) return
        
        val cookie = GymManager.getBypassCookie(requireContext())
        val ua = GymManager.getBypassUA(requireContext())
        val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
        
        lifecycleScope.launch {
            try {
                val response = api.getAttendanceLogs(userId, gymId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val logs = response.body()?.logs ?: emptyList()
                    val mappedLogs = logs.map { log ->
                        val date = log.attendance_date ?: "1970-01-01"
                        
                        val timeRaw = log.check_in_time ?: "00:00:00"
                        var formattedTime = timeRaw
                        var formattedTimeOut: String? = null
                        try {
                            val sdfIn = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                            val sdfOut = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US)
                            sdfIn.parse(timeRaw)?.let { formattedTime = sdfOut.format(it) }
                            
                            val outTimeRaw = log.check_out_time
                            if (outTimeRaw != null) {
                                sdfIn.parse(outTimeRaw)?.let { formattedTimeOut = sdfOut.format(it) }
                            }
                        } catch(e: Exception) {}
                        
                        val status = if (log.attendance_status == "Active") "ACTIVE NOW" else "COMPLETED"
                        val gName = log.gym_name ?: GymManager.getGymName(requireContext())
                        
                        GymAttendance(date, formattedTime, formattedTimeOut, gName, status)
                    }
                    
                    attendanceAdapter.updateLogs(mappedLogs)
                    
                    val rv = rootView.findViewById<RecyclerView>(R.id.rvAttendanceLogs)
                    val emptyState = rootView.findViewById<View>(R.id.emptyStateAttendance)
                    
                    if (mappedLogs.isEmpty()) {
                        rv?.visibility = View.GONE
                        emptyState?.visibility = View.VISIBLE
                    } else {
                        rv?.visibility = View.VISIBLE
                        emptyState?.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                // Keep UI states unmodified or show generic error UI
            }
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val cardColorStr = GymManager.getCardColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#A855F7")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) Color.parseColor(bgColorStr) else Color.parseColor("#0a090d")
            
            // 1. Root Background & Global Labels
            view.findViewById<View>(R.id.attendanceRoot)?.setBackgroundColor(bgColor)
            view.findViewById<TextView>(R.id.tvAttendanceTitle)?.setTextColor(Color.WHITE)
            view.findViewById<TextView>(R.id.tvLogsLabel)?.setTextColor(Color.WHITE)
            
            // 2. Title Accent
            view.findViewById<TextView>(R.id.tvAttendanceThemeSubtitle)?.setTextColor(themeColor)
            
            // 3. Scanner Card Surface
            val cardSurface = if (isAutoCard) {
                Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            } else {
                try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
            }

            val cardScanner = view.findViewById<MaterialCardView>(R.id.cardScanner)
            cardScanner?.setCardBackgroundColor(cardSurface)
            cardScanner?.setStrokeColor(ColorStateList.valueOf(themeColor).withAlpha(40))
            
            // 4. Manual Launch Branding
            view.findViewById<ImageView>(R.id.ivLaunchScannerIcon)?.imageTintList = ColorStateList.valueOf(themeColor)
            view.findViewById<TextView>(R.id.tvLaunchScannerLabel)?.setTextColor(themeColor)

            // 5. Scanner Overlay Branding
            view.findViewById<View>(R.id.vScannerOverlay)?.let {
                it.backgroundTintList = ColorStateList.valueOf(themeColor)
            }
            view.findViewById<ImageView>(R.id.ivScannerIcon)?.let {
                it.imageTintList = ColorStateList.valueOf(themeColor)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkCameraPermission(view: View) {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera(view)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                view?.let { startCamera(it) }
            } else {
                Toast.makeText(requireContext(), "Camera permission is required for attendance scanning", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startCamera(rootView: View) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        val previewView = rootView.findViewById<PreviewView>(R.id.attendancePreviewView)
        val launchOverlay = rootView.findViewById<View>(R.id.layoutStartScanner)
        val scannerOverlay = rootView.findViewById<View>(R.id.vScannerOverlay)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient(
                    com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                        .build()
                )

                val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalysis)

                // Transition UI
                launchOverlay?.visibility = View.GONE
                previewView?.visibility = View.VISIBLE
                scannerOverlay?.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                android.util.Log.e("AttendanceFragment", "CameraX initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(
        barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: androidx.camera.core.ImageProxy
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && !isScanning) {
                            handleScannedQR(rawValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Ignored
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScannedQR(qrCodeContent: String) {
        isScanning = true
        try {
            val json = org.json.JSONObject(qrCodeContent)
            var scannedGymId = -1
            if (json.has("gym_id")) {
                scannedGymId = try { json.getInt("gym_id") } catch (e: Exception) { json.getString("gym_id").toIntOrNull() ?: -1 }
            }
            var action = json.optString("action", "check_in")
            if (action == "checkin") action = "check_in"
            
            val userId = GymManager.getUserId(requireContext())
            
            if (scannedGymId != -1 && userId != -1) {
                Toast.makeText(requireContext(), "Processing Check In...", Toast.LENGTH_SHORT).show()
                val request = com.example.horizonsystems.models.AttendanceRequest(userId, scannedGymId, action)
                
                val cookie = GymManager.getBypassCookie(requireContext())
                val ua = GymManager.getBypassUA(requireContext())
                val api = com.example.horizonsystems.network.RetrofitClient.getApi(cookie, ua)
                
                lifecycleScope.launch {
                    try {
                        val response = api.recordAttendance(request)
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(requireContext(), response.body()?.message ?: "Check-in successful!", Toast.LENGTH_LONG).show()
                            view?.let { fetchRealAttendanceLogs(it) }
                        } else {
                            Toast.makeText(requireContext(), response.body()?.message ?: "Failed to check in.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                    } finally {
                        kotlinx.coroutines.delay(3000)
                        isScanning = false
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Invalid QR Code for Check-in", Toast.LENGTH_SHORT).show()
                view?.postDelayed({ isScanning = false }, 2000)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid QR format", Toast.LENGTH_SHORT).show()
            view?.postDelayed({ isScanning = false }, 2000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
