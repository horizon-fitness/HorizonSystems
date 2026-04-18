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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AttendanceFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val CAMERA_PERMISSION_CODE = 1001

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
        attendanceAdapter = AttendanceAdapter(getMockAttendance())
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = attendanceAdapter
        
        // Show the list if not empty (mock is never empty)
        rv?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.emptyStateAttendance)?.visibility = View.GONE
    }

    private fun getMockAttendance(): List<GymAttendance> {
        return listOf(
            GymAttendance("2026-10-18", "08:15 AM", GymManager.getGymName(requireContext()), "PRESENT"),
            GymAttendance("2026-10-17", "05:30 PM", GymManager.getGymName(requireContext()), "PRESENT"),
            GymAttendance("2026-10-16", "07:00 AM", GymManager.getGymName(requireContext()), "PRESENT")
        )
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

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)

                // Transition UI
                launchOverlay?.visibility = View.GONE
                previewView?.visibility = View.VISIBLE
                scannerOverlay?.visibility = View.VISIBLE
                
            } catch (e: Exception) {
                android.util.Log.e("AttendanceFragment", "CameraX initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
