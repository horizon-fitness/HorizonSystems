package com.example.horizonsystems

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
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
        val tvFromDate = view.findViewById<TextView>(R.id.tvFromDate)
        val tvToDate = view.findViewById<TextView>(R.id.tvToDate)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilters)
        val tvClear = view.findViewById<View>(R.id.tvClearAll)
        val displayFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // Restore state — "All" means all checked
        when(currentStatus) {
            "ALL" -> { cbAll.isChecked = true; cbPending.isChecked = true; cbConfirmed.isChecked = true; cbCompleted.isChecked = true }
            "PENDING" -> { cbAll.isChecked = false; cbPending.isChecked = true; cbConfirmed.isChecked = false; cbCompleted.isChecked = false }
            "APPROVED" -> { cbAll.isChecked = false; cbPending.isChecked = false; cbConfirmed.isChecked = true; cbCompleted.isChecked = false }
            "COMPLETED" -> { cbAll.isChecked = false; cbPending.isChecked = false; cbConfirmed.isChecked = false; cbCompleted.isChecked = true }
        }

        // Restore date labels
        startDate?.let { tvFromDate.text = displayFmt.format(Date(it)); tvFromDate.alpha = 1f }
        endDate?.let { tvToDate.text = displayFmt.format(Date(it)); tvToDate.alpha = 1f }

        // "All" checks/unchecks all
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { cbPending.isChecked = true; cbConfirmed.isChecked = true; cbCompleted.isChecked = true }
        }
        val deAllListener = { _: android.widget.CompoundButton, isChecked: Boolean ->
            if (!isChecked) cbAll.isChecked = false
        }
        cbPending.setOnCheckedChangeListener(deAllListener)
        cbConfirmed.setOnCheckedChangeListener(deAllListener)
        cbCompleted.setOnCheckedChangeListener(deAllListener)

        // From date picker
        tvFromDate.setOnClickListener {
            val cal = Calendar.getInstance()
            startDate?.let { cal.timeInMillis = it }
            val dlg = DatePickerDialog(requireContext(), { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                startDate = sel.timeInMillis
                tvFromDate.text = displayFmt.format(sel.time); tvFromDate.alpha = 1f
                endDate?.let { if (it < sel.timeInMillis) { endDate = null; tvToDate.text = "Select"; tvToDate.alpha = 0.5f } }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dlg.datePicker.maxDate = System.currentTimeMillis()
            dlg.show()
        }

        // To date picker
        tvToDate.setOnClickListener {
            val cal = Calendar.getInstance()
            endDate?.let { cal.timeInMillis = it }
            val dlg = DatePickerDialog(requireContext(), { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                endDate = sel.timeInMillis
                tvToDate.text = displayFmt.format(sel.time); tvToDate.alpha = 1f
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dlg.datePicker.maxDate = System.currentTimeMillis()
            startDate?.let { dlg.datePicker.minDate = it }
            dlg.show()
        }

        tvClear.setOnClickListener {
            currentStatus = "ALL"; startDate = null; endDate = null
            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }

        btnApply.setOnClickListener {
            currentStatus = when {
                cbAll.isChecked || (cbPending.isChecked && cbConfirmed.isChecked && cbCompleted.isChecked) -> "ALL"
                cbPending.isChecked && !cbConfirmed.isChecked && !cbCompleted.isChecked -> "PENDING"
                cbConfirmed.isChecked && !cbPending.isChecked && !cbCompleted.isChecked -> "APPROVED"
                cbCompleted.isChecked && !cbPending.isChecked && !cbConfirmed.isChecked -> "COMPLETED"
                else -> "ALL"
            }
            listener?.onFiltersApplied(currentStatus, startDate, endDate)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundResource(android.R.color.transparent)
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
