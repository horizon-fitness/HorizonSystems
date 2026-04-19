package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.graphics.Color
import android.content.res.ColorStateList
import com.example.horizonsystems.utils.GymManager

class AttendanceFilterSheet : BottomSheetDialogFragment() {

    interface FilterListener {
        fun onFiltersApplied(status: String, start: Long?, end: Long?)
    }

    private var listener: FilterListener? = null
    private var currentStatus = "ALL"
    private var startDate: Long? = null
    private var endDate: Long? = null

    fun setParams(status: String, start: Long?, end: Long?, listener: FilterListener) {
        this.currentStatus = status
        this.startDate = start
        this.endDate = end
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sheet_attendance_filter, container, false)
        applyBranding(view)
        setupLogic(view)
        return view
    }

    private fun setupLogic(view: View) {
        val btnClose = view.findViewById<View>(R.id.btnCloseSheet)
        btnClose?.setOnClickListener { dismiss() }

        val statusChips = listOf(
            view.findViewById<TextView>(R.id.chipAll),
            view.findViewById<TextView>(R.id.chipActive),
            view.findViewById<TextView>(R.id.chipCompleted)
        )

        statusChips.forEach { chip ->
            chip?.setOnClickListener {
                currentStatus = when(chip.id) {
                    R.id.chipActive -> "Active"
                    R.id.chipCompleted -> "Completed"
                    else -> "ALL"
                }
                updateChips(statusChips)
            }
        }

        updateChips(statusChips)

        view.findViewById<View>(R.id.btnApplyFilters)?.setOnClickListener {
            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }

        view.findViewById<View>(R.id.btnResetFilters)?.setOnClickListener {
            currentStatus = "ALL"
            startDate = null
            endDate = null
            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }
    }

    private fun updateChips(chips: List<TextView?>) {
        val ctx = context ?: return
        val themeColor = Color.parseColor(GymManager.getThemeColor(ctx))
        
        chips.forEach { chip ->
            val isSelected = when(chip?.id) {
                R.id.chipActive -> currentStatus == "Active"
                R.id.chipCompleted -> currentStatus == "Completed"
                else -> currentStatus == "ALL"
            }

            if (isSelected) {
                chip?.setTextColor(Color.WHITE)
                chip?.backgroundTintList = ColorStateList.valueOf(themeColor)
            } else {
                chip?.setTextColor(Color.parseColor("#94A3B8"))
                chip?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1AFFFFFF"))
            }
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColor = Color.parseColor(GymManager.getThemeColor(ctx))
        val bgColor = Color.parseColor(GymManager.getBgColor(ctx))
        
        view.findViewById<View>(R.id.sheetRoot)?.setBackgroundColor(bgColor)
        view.findViewById<TextView>(R.id.btnApplyFilters)?.backgroundTintList = ColorStateList.valueOf(themeColor)
    }
}
