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
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class AttendanceFragment : Fragment(), AttendanceFilterSheet.FilterListener, AttendanceSortSheet.SortListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var attendanceAdapter: AttendanceAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val CAMERA_PERMISSION_CODE = 1001
    private var isScanning = false
    
    private val fullLogsList = mutableListOf<GymAttendance>()
    private val displayLogsList = mutableListOf<GymAttendance>()
    
    private var currentFilterStatus = "ALL"
    private var currentSort = "NEWEST"
    private var searchQuery = ""
    private var startDate: Long? = null
    private var endDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupRecyclerView(view)
        applyBranding(view)
        ThemeUtils.applyThemeToView(view)
        
        setupTabs(view)
        setupSearchAndFilters(view)
        
        // Manual Start Launcher
        view.findViewById<View>(R.id.layoutStartScanner)?.setOnClickListener {
            checkCameraPermission(view)
        }
        
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        swipeRefresh = view.findViewById(R.id.swipeRefreshAttendance)
        
        applyBranding(view)
        setupRefresh()
    }

    private fun setupSearchAndFilters(view: View) {
        val etSearch = view.findViewById<android.widget.EditText>(R.id.etSearchAttendance)
        val btnSort = view.findViewById<View>(R.id.btnSortAttendance)
        val btnFilter = view.findViewById<View>(R.id.btnFilterAttendance)

        etSearch?.addTextChangedListener(object: android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilterAndSort()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSort?.setOnClickListener {
            val sheet = AttendanceSortSheet()
            sheet.setParams(currentSort, this)
            sheet.show(childFragmentManager, "ATTENDANCE_SORT")
        }

        btnFilter?.setOnClickListener {
            val sheet = AttendanceFilterSheet()
            sheet.setParams(currentFilterStatus, startDate, endDate, this)
            sheet.show(childFragmentManager, "ATTENDANCE_FILTER")
        }
    }

    override fun onFiltersApplied(status: String, start: Long?, end: Long?) {
        this.currentFilterStatus = status
        this.startDate = start
        this.endDate = end
        applyFilterAndSort()
    }

    override fun onSortSelected(sort: String) {
        this.currentSort = sort
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val filtered = fullLogsList.filter { log ->
            val statusMatch = if (currentFilterStatus == "ALL") true else log.status.contains(currentFilterStatus, ignoreCase = true)
            val searchMatch = if (searchQuery.isEmpty()) true else log.gymName.contains(searchQuery, ignoreCase = true) || log.date.contains(searchQuery, ignoreCase = true)
            statusMatch && searchMatch
        }

        val sorted = when(currentSort) {
            "OLDEST" -> filtered.sortedBy { it.date }
            else -> filtered.sortedByDescending { it.date }
        }

        displayLogsList.clear()
        displayLogsList.addAll(sorted)
        attendanceAdapter.updateLogs(displayLogsList)

        val rv = view?.findViewById<RecyclerView>(R.id.rvAttendanceLogs)
        val emptyState = view?.findViewById<View>(R.id.emptyStateAttendance)
        
        if (displayLogsList.isEmpty()) {
            rv?.visibility = View.GONE
            emptyState?.visibility = View.VISIBLE
        } else {
            rv?.visibility = View.VISIBLE
            emptyState?.visibility = View.GONE
        }
    }

    private fun setupTabs(view: View) {
        val btnScan = view.findViewById<MaterialButton>(R.id.btn_tab_scan_web)
        val btnMyQr = view.findViewById<MaterialButton>(R.id.btn_tab_my_qr)
        val cardScanner = view.findViewById<View>(R.id.cardScanner)
        val cardMyQr = view.findViewById<View>(R.id.cardMyQR)
        val ivMyQrCode = view.findViewById<ImageView>(R.id.ivMyQrCode)
        
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val themeColor = try { Color.parseColor(themeColorStr) } catch(e: Exception) { Color.parseColor("#A855F7") }
        
        fun updateTabStyles(isScanActive: Boolean) {
            if (isScanActive) {
                btnScan?.setTextColor(themeColor)
                btnScan?.backgroundTintList = ColorStateList.valueOf(themeColor.withAlpha(15))
                
                btnMyQr?.setTextColor(Color.parseColor("#94A3B8"))
                btnMyQr?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            } else {
                btnMyQr?.setTextColor(themeColor)
                btnMyQr?.backgroundTintList = ColorStateList.valueOf(themeColor.withAlpha(15))
                
                btnScan?.setTextColor(Color.parseColor("#94A3B8"))
                btnScan?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            }
        }

        btnScan?.setOnClickListener {
            cardScanner?.visibility = View.VISIBLE
            cardMyQr?.visibility = View.GONE
            updateTabStyles(true)
        }
        
        btnMyQr?.setOnClickListener {
            cardScanner?.visibility = View.GONE
            cardMyQr?.visibility = View.VISIBLE
            updateTabStyles(false)
            
            // Generate and load QR
            val userId = GymManager.getUserId(ctx)
            val data = "{\"user_id\":$userId,\"action\":\"check_in\"}"
            val encoded = java.net.URLEncoder.encode(data, "UTF-8")
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=$encoded&color=000000&bgcolor=ffffff&margin=4&qzone=1"
            
            Glide.with(this).load(qrUrl).into(ivMyQrCode)
        }
    }

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvAttendanceLogs)
        attendanceAdapter = AttendanceAdapter(displayLogsList)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = attendanceAdapter
        
        fetchAttendanceLogs(view)
    }

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun fetchAttendanceLogs(view: View) {
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
                    
                    fullLogsList.clear()
                    fullLogsList.addAll(mappedLogs)
                    applyFilterAndSort()
                }
            } catch (e: Exception) {
                // Keep UI states unmodified or show generic error UI
            }
        }
    }

    private fun setupRefresh() {
        val themeColor = Color.parseColor(GymManager.getThemeColor(requireContext()))
        swipeRefresh.setColorSchemeColors(themeColor)
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#141216"))
        
        swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                fetchData()
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun fetchData() {
        view?.let {
            fetchAttendanceLogs(it)
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
            view.findViewById<TextView>(R.id.tvAttendanceTitlePart1)?.setTextColor(Color.WHITE)
            view.findViewById<TextView>(R.id.tvLogsLabelPart1)?.setTextColor(Color.WHITE)
            
            // 2. Title & Log Accents
            view.findViewById<TextView>(R.id.tvAttendanceTitlePart2)?.setTextColor(themeColor)
            view.findViewById<TextView>(R.id.tvLogsLabelPart2)?.setTextColor(themeColor)
            
            val textColorStr = GymManager.getTextColor(ctx)
            val textColor = if (!textColorStr.isNullOrEmpty()) Color.parseColor(textColorStr) else Color.parseColor("#D1D5DB")
            
            // Apply text color to labels if needed or keep white for premium contrast
            
            // 3. Search and Filter Branding
            view.findViewById<ImageView>(R.id.btnSortAttendance)?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            view.findViewById<ImageView>(R.id.btnFilterAttendance)?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            
            // Subtitle
            view.findViewById<TextView>(R.id.tvAttendanceThemeSubtitle)?.alpha = 0.6f
            
            // 3. Scanner Card Surface
            val cardSurface = if (isAutoCard) {
                Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            } else {
                try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
            }

            val cardScanner = view.findViewById<MaterialCardView>(R.id.cardScanner)
            cardScanner?.setCardBackgroundColor(cardSurface)
            cardScanner?.setStrokeColor(ColorStateList.valueOf(themeColor.withAlpha(40)))
            
            val cardMyQR = view.findViewById<MaterialCardView>(R.id.cardMyQR)
            cardMyQR?.setCardBackgroundColor(cardSurface)
            cardMyQR?.setStrokeColor(ColorStateList.valueOf(themeColor.withAlpha(40)))
            
            view.findViewById<MaterialButton>(R.id.btn_tab_scan_web)?.let {
                it.setTextColor(themeColor)
                it.backgroundTintList = ColorStateList.valueOf(themeColor.withAlpha(15))
            }
            
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
                            view?.let { fetchAttendanceLogs(it) }
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
