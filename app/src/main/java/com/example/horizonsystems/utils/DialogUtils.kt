package com.example.horizonsystems.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.horizonsystems.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

object DialogUtils {

    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    /**
     * Shows a premium, highly tailored dialog to display session details.
     */
    fun showAttendanceDetailsDialog(
        context: Context,
        log: com.example.horizonsystems.GymAttendance
    ) {
        val builder = AlertDialog.Builder(context, R.style.CustomDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_attendance_details, null)

        val card = view.findViewById<MaterialCardView>(R.id.dialogCard)
        val divider = view.findViewById<View>(R.id.dialogDivider)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnDialogClose)

        // Find detail views
        val tvGymValue = view.findViewById<TextView>(R.id.tvGymValue)
        val tvDateValue = view.findViewById<TextView>(R.id.tvDateValue)
        val tvCheckInValue = view.findViewById<TextView>(R.id.tvCheckInValue)
        val tvCheckOutValue = view.findViewById<TextView>(R.id.tvCheckOutValue)
        val tvDurationValue = view.findViewById<TextView>(R.id.tvDurationValue)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)

        // Find icons to tint
        val ivGymIcon = view.findViewById<ImageView>(R.id.ivGymIcon)
        val ivDateIcon = view.findViewById<ImageView>(R.id.ivDateIcon)
        val ivCheckInIcon = view.findViewById<ImageView>(R.id.ivCheckInIcon)
        val ivCheckOutIcon = view.findViewById<ImageView>(R.id.ivCheckOutIcon)
        val ivDurationIcon = view.findViewById<ImageView>(R.id.ivDurationIcon)
        val ivStatusIcon = view.findViewById<ImageView>(R.id.ivStatusIcon)

        // Date formatting helper
        val displayDate = try {
            val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(log.date)
            java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US).format(parsed!!)
        } catch (e: Exception) {
            log.date
        }

        // Set text values
        tvGymValue.text = log.gymName
        tvDateValue.text = displayDate
        tvCheckInValue.text = log.timeIn
        tvCheckOutValue.text = log.timeOut ?: "Still Active (Checked In)"

        // Calculate session duration
        val durationText = try {
            val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            val checkInDate = sdf.parse(log.timeIn)
            if (checkInDate != null) {
                val isTimeOutActive = log.timeOut == null || 
                                      log.timeOut.trim().isEmpty() || 
                                      log.timeOut.contains("Active", ignoreCase = true) || 
                                      log.timeOut.contains("Checked In", ignoreCase = true)
                
                if (isTimeOutActive) {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                    if (log.date == todayStr) {
                        val now = java.util.Calendar.getInstance()
                        val checkInCal = java.util.Calendar.getInstance().apply {
                            time = checkInDate
                            set(java.util.Calendar.YEAR, now.get(java.util.Calendar.YEAR))
                            set(java.util.Calendar.MONTH, now.get(java.util.Calendar.MONTH))
                            set(java.util.Calendar.DAY_OF_MONTH, now.get(java.util.Calendar.DAY_OF_MONTH))
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        if (now.before(checkInCal)) {
                            checkInCal.add(java.util.Calendar.DATE, -1)
                        }
                        val diffMs = now.timeInMillis - checkInCal.timeInMillis
                        val diffSecs = diffMs / 1000
                        val hours = diffSecs / 3600
                        val minutes = (diffSecs % 3600) / 60
                        val seconds = diffSecs % 60
                        
                        when {
                            hours > 0 -> "${hours}h ${minutes}m ${seconds}s so far"
                            minutes > 0 -> "${minutes}m ${seconds}s so far"
                            else -> "${seconds}s so far"
                        }
                    } else "--"
                } else {
                    val checkOutDate = sdf.parse(log.timeOut!!)
                    if (checkOutDate != null) {
                        var diffMinutes = (checkOutDate.time - checkInDate.time) / (60 * 1000)
                        if (diffMinutes < 0) {
                            diffMinutes += 24 * 60
                        }
                        val hours = diffMinutes / 60
                        val minutes = diffMinutes % 60
                        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    } else "--"
                }
            } else "--"
        } catch (e: Exception) {
            "--"
        }
        tvDurationValue.text = durationText
        tvStatusBadge.text = log.status

        // Theme and Dynamic Styling
        val themeColorStr = GymManager.getThemeColor(context)
        val bgColorStr = GymManager.getBgColor(context)
        
        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#22C55E")
            val bgColor = if (!bgColorStr.isNullOrEmpty()) Color.parseColor(bgColorStr) else Color.parseColor("#100F12")

            // Card background
            card.setCardBackgroundColor(bgColor)

            // Dynamic Stroke styling matching other components
            card.setStrokeColor(ColorStateList.valueOf(themeColor.withAlpha(40)))

            // Title accent divider
            divider.setBackgroundColor(themeColor)

            // Button styling
            btnClose.backgroundTintList = ColorStateList.valueOf(themeColor)

            // Icon tints
            val iconTint = ColorStateList.valueOf(themeColor)
            ivGymIcon.imageTintList = iconTint
            ivDateIcon.imageTintList = iconTint
            ivCheckInIcon.imageTintList = iconTint
            ivCheckOutIcon.imageTintList = iconTint
            ivDurationIcon.imageTintList = iconTint
            ivStatusIcon.imageTintList = iconTint

            // Status Badge pill styling
            if (log.status.equals("ACTIVE NOW", ignoreCase = true)) {
                val activeGreen = Color.parseColor("#10B981")
                tvStatusBadge.setTextColor(activeGreen)
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(activeGreen.withAlpha(30))
            } else {
                tvStatusBadge.setTextColor(themeColor)
                tvStatusBadge.backgroundTintList = ColorStateList.valueOf(themeColor.withAlpha(30))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Shows a premium, theme-aware confirmation dialog using the Elite design system.
     */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        negativeText: String? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context, R.style.CustomDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_standard, null)

        val txtTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val divider = view.findViewById<View>(R.id.dialogDivider)
        val txtMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnPositive = view.findViewById<MaterialButton>(R.id.btnDialogPositive)
        val btnNegative = view.findViewById<MaterialButton>(R.id.btnDialogNegative)
        val card = view.findViewById<MaterialCardView>(R.id.dialogCard)

        txtTitle.text = title
        txtMessage.text = message
        btnPositive.text = positiveText
        
        if (negativeText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = negativeText
        }

        // Apply Gym Theme Branding
        val themeColorStr = GymManager.getThemeColor(context)
        val bgColorStr = GymManager.getBgColor(context)
        
        try {
            if (!themeColorStr.isNullOrEmpty()) {
                val themeColor = Color.parseColor(themeColorStr)
                btnPositive.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
                divider.setBackgroundColor(themeColor)
            }
            
            if (!bgColorStr.isNullOrEmpty()) {
                val bgColor = Color.parseColor(bgColorStr)
                card.setCardBackgroundColor(bgColor)
            }
        } catch (e: Exception) {}

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnPositive.setOnClickListener {
            onPositiveClick?.invoke()
            dialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            onNegativeClick?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}
