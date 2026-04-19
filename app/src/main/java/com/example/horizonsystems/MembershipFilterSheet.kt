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

class MembershipFilterSheet : BottomSheetDialogFragment() {

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
            inflater.inflate(R.layout.sheet_membership_filter, container, false)
        } catch (e: Exception) {
            Log.e("MembershipFilterSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        setupUI(view)
        applyBranding(view)
        return view
    }

    private fun setupUI(view: View) {
        val cbAll = view.findViewById<CheckBox>(R.id.cbStatusAll)
        val cbPending = view.findViewById<CheckBox>(R.id.cbStatusPending)
        val cbApproved = view.findViewById<CheckBox>(R.id.cbStatusApproved)
        val cbExpired = view.findViewById<CheckBox>(R.id.cbStatusExpired)
        val tvFromDate = view.findViewById<TextView>(R.id.tvFromDate)
        val tvToDate = view.findViewById<TextView>(R.id.tvToDate)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilters)
        val tvClear = view.findViewById<View>(R.id.tvClearAll)
        val displayFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        // Restore checkbox state — "All" means all checked
        when(currentStatus) {
            "ALL" -> { cbAll.isChecked = true; cbPending.isChecked = true; cbApproved.isChecked = true; cbExpired.isChecked = true }
            "Pending" -> { cbAll.isChecked = false; cbPending.isChecked = true; cbApproved.isChecked = false; cbExpired.isChecked = false }
            "Approved" -> { cbAll.isChecked = false; cbPending.isChecked = false; cbApproved.isChecked = true; cbExpired.isChecked = false }
            "Expired" -> { cbAll.isChecked = false; cbPending.isChecked = false; cbApproved.isChecked = false; cbExpired.isChecked = true }
        }

        // Restore date labels
        startDate?.let { tvFromDate.text = displayFmt.format(Date(it)); tvFromDate.alpha = 1f }
        endDate?.let { tvToDate.text = displayFmt.format(Date(it)); tvToDate.alpha = 1f }

        // "All" checks all others
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { cbPending.isChecked = true; cbApproved.isChecked = true; cbExpired.isChecked = true }
        }
        val deAllListener = { _: android.widget.CompoundButton, isChecked: Boolean ->
            if (!isChecked) cbAll.isChecked = false
        }
        cbPending.setOnCheckedChangeListener(deAllListener)
        cbApproved.setOnCheckedChangeListener(deAllListener)
        cbExpired.setOnCheckedChangeListener(deAllListener)

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
                cbAll.isChecked || (cbPending.isChecked && cbApproved.isChecked && cbExpired.isChecked) -> "ALL"
                cbPending.isChecked && !cbApproved.isChecked && !cbExpired.isChecked -> "Pending"
                cbApproved.isChecked && !cbPending.isChecked && !cbExpired.isChecked -> "Approved"
                cbExpired.isChecked && !cbPending.isChecked && !cbApproved.isChecked -> "Expired"
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

                val checkBoxes = listOf(R.id.cbStatusAll, R.id.cbStatusPending, R.id.cbStatusApproved, R.id.cbStatusExpired)
                checkBoxes.forEach { id ->
                    view.findViewById<CheckBox>(id)?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }

            } catch (e: Exception) {
                Log.e("MembershipFilterSheet", "Branding Error: ${e.message}")
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
