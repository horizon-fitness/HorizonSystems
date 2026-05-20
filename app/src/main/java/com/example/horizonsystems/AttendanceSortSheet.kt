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

class AttendanceSortSheet : BottomSheetDialogFragment() {

    interface SortListener {
        fun onSortSelected(sort: String)
    }

    private var listener: SortListener? = null
    private var currentSort = "NEWEST"

    fun setParams(sort: String, listener: SortListener) {
        this.currentSort = sort
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.sheet_attendance_sort, container, false)
        applyBranding(view)
        setupLogic(view)
        return view
    }

    private fun setupLogic(view: View) {
        val rgSort = view.findViewById<android.widget.RadioGroup>(R.id.rgSortOptions)
        val btnApply = view.findViewById<View>(R.id.btnApplySort)

        when(currentSort) {
            "NEWEST" -> view.findViewById<android.widget.RadioButton>(R.id.rbNewest)?.isChecked = true
            "OLDEST" -> view.findViewById<android.widget.RadioButton>(R.id.rbOldest)?.isChecked = true
        }

        btnApply?.setOnClickListener {
            val selectedSort = when(rgSort?.checkedRadioButtonId) {
                R.id.rbOldest -> "OLDEST"
                else -> "NEWEST"
            }
            listener?.onSortSelected(selectedSort)
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

        val themeColorStr = GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApplySort)?.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                
                val rg = view.findViewById<android.widget.RadioGroup>(R.id.rgSortOptions)
                for (i in 0 until (rg?.childCount ?: 0)) {
                    val rb = rg?.getChildAt(i) as? android.widget.RadioButton
                    rb?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }
            } catch (e: Exception) {}
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
