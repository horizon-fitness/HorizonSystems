package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class PaymentFilterSheet : BottomSheetDialogFragment() {

    interface FilterListener {
        fun onFiltersApplied(status: String, type: String, start: Long?, end: Long?)
    }

    private var listener: FilterListener? = null
    private var currentStatus = "ALL"
    private var currentType = "ALL"
    private var startDate: Long? = null
    private var endDate: Long? = null

    fun setParams(status: String, type: String, start: Long?, end: Long?, listener: FilterListener) {
        this.currentStatus = status
        this.currentType = type
        this.startDate = start
        this.endDate = end
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = try {
            inflater.inflate(R.layout.sheet_payment_filter, container, false)
        } catch (e: Exception) {
            Log.e("PaymentFilterSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        setupUI(view)
        applyBranding(view)
        return view
    }

    private fun setupUI(view: View) {
        val cgType = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgType)
        val cbAll = view.findViewById<android.widget.CheckBox>(R.id.cbStatusAll)
        val cbPending = view.findViewById<android.widget.CheckBox>(R.id.cbStatusPending)
        val cbCompleted = view.findViewById<android.widget.CheckBox>(R.id.cbStatusCompleted)
        val calendar = view.findViewById<android.widget.CalendarView>(R.id.calendarFilter)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilters)
        val tvClear = view.findViewById<View>(R.id.tvClearAll)

        // Initial State
        when(currentType) {
            "ALL" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeAll)?.isChecked = true
            "MEMBERSHIP" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeMembership)?.isChecked = true
            "BOOKING" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeBooking)?.isChecked = true
        }

        when(currentStatus) {
            "ALL" -> cbAll.isChecked = true
            "PENDING" -> cbPending.isChecked = true
            "COMPLETED" -> cbCompleted.isChecked = true
        }

        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            startDate = cal.timeInMillis
            endDate = cal.timeInMillis + (24 * 60 * 60 * 1000) // 1 day window for simplified integrated selection
        }

        tvClear.setOnClickListener {
            currentStatus = "ALL"
            currentType = "ALL"
            startDate = null
            endDate = null
            listener?.onFiltersApplied(currentStatus, currentType, startDate, endDate)
            dismiss()
        }

        btnApply.setOnClickListener {
            currentType = when(cgType.checkedChipId) {
                R.id.chipTypeMembership -> "MEMBERSHIP"
                R.id.chipTypeBooking -> "BOOKING"
                else -> "ALL"
            }

            currentStatus = if (cbPending.isChecked && !cbCompleted.isChecked) "PENDING"
                           else if (cbCompleted.isChecked && !cbPending.isChecked) "COMPLETED"
                           else "ALL"

            listener?.onFiltersApplied(currentStatus, currentType, startDate, endDate)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                
                // Root Background (28dp rounded)
                view.findViewById<android.widget.LinearLayout>(R.id.rootFilterContainer)?.let { root ->
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    val bgColor = try { android.graphics.Color.parseColor(GymManager.getBgColor(ctx)) } 
                                  catch(e: Exception) { android.graphics.Color.parseColor("#151518") }
                    shape.setColor(bgColor)
                    val radius = (28 * ctx.resources.displayMetrics.density)
                    shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                    root.background = shape
                }

                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApplyFilters)?.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                
                // Checkboxes branding
                val checkBoxes = listOf(R.id.cbStatusAll, R.id.cbStatusPending, R.id.cbStatusCompleted)
                checkBoxes.forEach { id ->
                    view.findViewById<android.widget.CheckBox>(id)?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }

                // Chip branding (High visibility selection)
                val typeChips = listOf(R.id.chipTypeAll, R.id.chipTypeMembership, R.id.chipTypeBooking)
                typeChips.forEach { id ->
                    view.findViewById<com.google.android.material.chip.Chip>(id)?.let { chip ->
                        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                        
                        // Text color: White when checked, Muted grey when unchecked
                        val textColors = intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.parseColor("#94A3B8"))
                        chip.setTextColor(android.content.res.ColorStateList(states, textColors))
                        
                        // Background: High alpha theme color when checked (60%), Subtle overlay when unchecked (5%)
                        val checkedBg = android.graphics.Color.argb(153, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor))
                        val bgColors = intArrayOf(checkedBg, android.graphics.Color.parseColor("#0DFFFFFF"))
                        chip.chipBackgroundColor = android.content.res.ColorStateList(states, bgColors)
                        
                        // Stroke: Theme color when checked
                        chip.chipStrokeColor = android.content.res.ColorStateList(states, intArrayOf(themeColor, android.graphics.Color.TRANSPARENT))
                        chip.chipStrokeWidth = if (chip.isChecked) 1f else 0f
                    }
                }

            } catch (e: Exception) {
                Log.e("PaymentFilterSheet", "Branding Error: ${e.message}")
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
