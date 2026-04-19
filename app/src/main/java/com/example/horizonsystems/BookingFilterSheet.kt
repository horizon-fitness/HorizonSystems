package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

class BookingFilterSheet : BottomSheetDialogFragment() {

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
        val view = try {
            inflater.inflate(R.layout.sheet_booking_filter, container, false)
        } catch (e: Exception) {
            Log.e("BookingFilterSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        setupUI(view)
        applyBranding(view)
        return view
    }

    private fun setupUI(view: View) {
        val cbAll = view.findViewById<CheckBox>(R.id.cbStatusAll)
        val cbPending = view.findViewById<CheckBox>(R.id.cbStatusPending)
        val cbConfirmed = view.findViewById<CheckBox>(R.id.cbStatusConfirmed)
        val cbCompleted = view.findViewById<CheckBox>(R.id.cbStatusCompleted)
        val calendar = view.findViewById<android.widget.CalendarView>(R.id.calendarFilter)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilters)
        val tvClear = view.findViewById<View>(R.id.tvClearAll)

        when(currentStatus) {
            "ALL" -> cbAll.isChecked = true
            "PENDING" -> cbPending.isChecked = true
            "APPROVED", "CONFIRMED" -> cbConfirmed.isChecked = true
            "COMPLETED" -> cbCompleted.isChecked = true
        }

        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            startDate = cal.timeInMillis
            endDate = cal.timeInMillis + (24 * 60 * 60 * 1000)
        }

        tvClear.setOnClickListener {
            currentStatus = "ALL"
            startDate = null
            endDate = null
            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }

        btnApply.setOnClickListener {
            currentStatus = if (cbPending.isChecked && !cbConfirmed.isChecked && !cbCompleted.isChecked) "PENDING"
                           else if (cbConfirmed.isChecked && !cbPending.isChecked && !cbCompleted.isChecked) "APPROVED"
                           else if (cbCompleted.isChecked && !cbPending.isChecked && !cbConfirmed.isChecked) "COMPLETED"
                           else "ALL"

            listener?.onFiltersApplied(currentStatus, startDate, endDate)
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
                
                val checkBoxes = listOf(R.id.cbStatusAll, R.id.cbStatusPending, R.id.cbStatusConfirmed, R.id.cbStatusCompleted)
                checkBoxes.forEach { id ->
                    view.findViewById<CheckBox>(id)?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }

            } catch (e: Exception) {
                Log.e("BookingFilterSheet", "Branding Error: ${e.message}")
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
