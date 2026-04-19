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
        val cbAll = view.findViewById<CheckBox>(R.id.cbStatusAll)
        val cbPending = view.findViewById<CheckBox>(R.id.cbStatusPending)
        val cbCompleted = view.findViewById<CheckBox>(R.id.cbStatusCompleted)
        val tvFromDate = view.findViewById<TextView>(R.id.tvFromDate)
        val tvToDate = view.findViewById<TextView>(R.id.tvToDate)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilters)
        val tvClear = view.findViewById<View>(R.id.tvClearAll)
        val displayFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // Restore chip state
        when(currentType) {
            "ALL" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeAll)?.isChecked = true
            "MEMBERSHIP" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeMembership)?.isChecked = true
            "BOOKING" -> view.findViewById<com.google.android.material.chip.Chip>(R.id.chipTypeBooking)?.isChecked = true
        }

        // Restore checkbox state — "All" means all checked
        when(currentStatus) {
            "ALL" -> { cbAll.isChecked = true; cbPending.isChecked = true; cbCompleted.isChecked = true }
            "PENDING" -> { cbAll.isChecked = false; cbPending.isChecked = true; cbCompleted.isChecked = false }
            "COMPLETED" -> { cbAll.isChecked = false; cbPending.isChecked = false; cbCompleted.isChecked = true }
        }

        // Restore date labels
        startDate?.let { tvFromDate.text = displayFmt.format(Date(it)); tvFromDate.alpha = 1f }
        endDate?.let { tvToDate.text = displayFmt.format(Date(it)); tvToDate.alpha = 1f }

        // "All" checkbox — checks/unchecks all others
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbPending.isChecked = true
                cbCompleted.isChecked = true
            }
        }
        // Individual checkboxes — uncheck "All" if any deselected
        val deAllListener = { _: android.widget.CompoundButton, isChecked: Boolean ->
            if (!isChecked) cbAll.isChecked = false
        }
        cbPending.setOnCheckedChangeListener(deAllListener)
        cbCompleted.setOnCheckedChangeListener(deAllListener)

        // From date picker — max date = today
        tvFromDate.setOnClickListener {
            val cal = Calendar.getInstance()
            startDate?.let { cal.timeInMillis = it }
            val dlg = DatePickerDialog(requireContext(), { _, y, m, d ->
                val sel = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                startDate = sel.timeInMillis
                tvFromDate.text = displayFmt.format(sel.time); tvFromDate.alpha = 1f
                // If "to" date is before new "from", clear it
                endDate?.let { if (it < sel.timeInMillis) { endDate = null; tvToDate.text = "Select"; tvToDate.alpha = 0.5f } }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dlg.datePicker.maxDate = System.currentTimeMillis()
            dlg.show()
        }

        // To date picker — min = startDate, max = today
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
            currentStatus = "ALL"; currentType = "ALL"; startDate = null; endDate = null
            listener?.onFiltersApplied(currentStatus, currentType, startDate, endDate)
            dismiss()
        }

        btnApply.setOnClickListener {
            currentType = when(cgType.checkedChipId) {
                R.id.chipTypeMembership -> "MEMBERSHIP"
                R.id.chipTypeBooking -> "BOOKING"
                else -> "ALL"
            }
            currentStatus = when {
                cbAll.isChecked || (cbPending.isChecked && cbCompleted.isChecked) -> "ALL"
                cbPending.isChecked -> "PENDING"
                cbCompleted.isChecked -> "COMPLETED"
                else -> "ALL"
            }
            listener?.onFiltersApplied(currentStatus, currentType, startDate, endDate)
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

                // Checkboxes — outline style (use theme color for tick, transparent bg)
                val checkBoxes = listOf(R.id.cbStatusAll, R.id.cbStatusPending, R.id.cbStatusCompleted)
                checkBoxes.forEach { id ->
                    view.findViewById<CheckBox>(id)?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }

                // Chip branding — high visibility selection
                val typeChips = listOf(R.id.chipTypeAll, R.id.chipTypeMembership, R.id.chipTypeBooking)
                typeChips.forEach { id ->
                    view.findViewById<com.google.android.material.chip.Chip>(id)?.let { chip ->
                        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
                        val textColors = intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.parseColor("#94A3B8"))
                        chip.setTextColor(android.content.res.ColorStateList(states, textColors))
                        val checkedBg = android.graphics.Color.argb(153, android.graphics.Color.red(themeColor), android.graphics.Color.green(themeColor), android.graphics.Color.blue(themeColor))
                        chip.chipBackgroundColor = android.content.res.ColorStateList(states, intArrayOf(checkedBg, android.graphics.Color.parseColor("#0DFFFFFF")))
                        chip.chipStrokeColor = android.content.res.ColorStateList(states, intArrayOf(themeColor, android.graphics.Color.TRANSPARENT))
                    }
                }

            } catch (e: Exception) {
                Log.e("PaymentFilterSheet", "Branding Error: ${e.message}")
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
