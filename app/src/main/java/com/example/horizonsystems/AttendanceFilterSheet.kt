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
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*

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

        val cbAll = view.findViewById<android.widget.CheckBox>(R.id.cbStatusAll)
        val cbActive = view.findViewById<android.widget.CheckBox>(R.id.cbStatusActive)
        val cbCompleted = view.findViewById<android.widget.CheckBox>(R.id.cbStatusCompleted)

        var isUpdating = true
        if (currentStatus == "ALL") {
            cbAll?.isChecked = true
            cbActive?.isChecked = true
            cbCompleted?.isChecked = true
        } else {
            cbAll?.isChecked = false
            val filters = currentStatus.split(",")
            cbActive?.isChecked = filters.contains("Active")
            cbCompleted?.isChecked = filters.contains("Completed")
            if (cbActive?.isChecked == true && cbCompleted?.isChecked == true) {
                cbAll?.isChecked = true
            }
        }
        isUpdating = false

        cbAll?.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdating) return@setOnCheckedChangeListener
            isUpdating = true
            cbActive?.isChecked = isChecked
            cbCompleted?.isChecked = isChecked
            isUpdating = false
        }

        val childListener = android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
            if (isUpdating) return@OnCheckedChangeListener
            isUpdating = true
            if (!isChecked) {
                cbAll?.isChecked = false
            } else {
                if (cbActive?.isChecked == true && cbCompleted?.isChecked == true) {
                    cbAll?.isChecked = true
                }
            }
            isUpdating = false
        }
        cbActive?.setOnCheckedChangeListener(childListener)
        cbCompleted?.setOnCheckedChangeListener(childListener)

        // Date Picker Setup
        val tvFromDate = view.findViewById<TextView>(R.id.tvFromDate)
        val tvToDate = view.findViewById<TextView>(R.id.tvToDate)
        val displayFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        
        startDate?.let { tvFromDate.text = displayFmt.format(Date(it)); tvFromDate.alpha = 1f }
        endDate?.let { tvToDate.text = displayFmt.format(Date(it)); tvToDate.alpha = 1f }

        val themeColor = try { Color.parseColor(GymManager.getThemeColor(requireContext())) } catch(e: Exception) { Color.parseColor("#FFFFFF") }

        tvFromDate?.setOnClickListener {
            val cal = Calendar.getInstance()
            startDate?.let { cal.timeInMillis = it }
            val calTheme = com.example.horizonsystems.utils.CalendarUtils.getCalendarTheme(requireContext())
            val dlg = DatePickerDialog(requireContext(), calTheme, { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                startDate = sel.timeInMillis
                tvFromDate.text = displayFmt.format(sel.time); tvFromDate.alpha = 1f
                endDate?.let { if (it < sel.timeInMillis) { endDate = null; tvToDate.text = "Select"; tvToDate.alpha = 0.5f } }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dlg.datePicker.maxDate = System.currentTimeMillis()
            dlg.show()
            dlg.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(themeColor)
            dlg.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(themeColor)
        }

        tvToDate?.setOnClickListener {
            val cal = Calendar.getInstance()
            endDate?.let { cal.timeInMillis = it }
            val calTheme = com.example.horizonsystems.utils.CalendarUtils.getCalendarTheme(requireContext())
            val dlg = DatePickerDialog(requireContext(), calTheme, { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                endDate = sel.timeInMillis
                tvToDate.text = displayFmt.format(sel.time); tvToDate.alpha = 1f
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dlg.datePicker.maxDate = System.currentTimeMillis()
            startDate?.let { dlg.datePicker.minDate = it }
            dlg.show()
            dlg.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(themeColor)
            dlg.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(themeColor)
        }

        view.findViewById<View>(R.id.btnApplyFilters)?.setOnClickListener {
            val selected = mutableListOf<String>()
            if (cbActive?.isChecked == true) selected.add("Active")
            if (cbCompleted?.isChecked == true) selected.add("Completed")

            currentStatus = if (cbAll?.isChecked == true || selected.size == 2 || selected.isEmpty()) {
                "ALL"
            } else {
                selected.joinToString(",")
            }

            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }

        view.findViewById<View>(R.id.tvClearAll)?.setOnClickListener {
            currentStatus = "ALL"
            startDate = null
            endDate = null
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
        val themeColor = Color.parseColor(GymManager.getThemeColor(ctx))
        
        view.findViewById<android.widget.LinearLayout>(R.id.sheetRoot)?.let { root ->
            val shape = android.graphics.drawable.GradientDrawable()
            shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            val bgColor = try { Color.parseColor(GymManager.getBgColor(ctx)) } 
                          catch(e: Exception) { Color.parseColor("#151518") }
            shape.setColor(bgColor)
            val radius = (28 * ctx.resources.displayMetrics.density)
            shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
            root.background = shape
        }

        view.findViewById<TextView>(R.id.btnApplyFilters)?.let { btn ->
            btn.backgroundTintList = ColorStateList.valueOf(themeColor)
            btn.setTextColor(Color.WHITE)
        }

        val checkBoxes = listOf(R.id.cbStatusAll, R.id.cbStatusActive, R.id.cbStatusCompleted)
        val checkboxStates = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_checked)
            ),
            intArrayOf(
                Color.parseColor("#94A3B8"), // Slate gray border for unchecked
                themeColor                  // Gym theme color for checked
            )
        )
        checkBoxes.forEach { id ->
            view.findViewById<android.widget.CheckBox>(id)?.buttonTintList = checkboxStates
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
