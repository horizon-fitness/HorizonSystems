package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.horizonsystems.utils.GymManager

class PaymentSortSheet : BottomSheetDialogFragment() {

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
        val view = try {
            inflater.inflate(R.layout.sheet_payment_sort, container, false)
        } catch (e: Exception) {
            Log.e("PaymentSortSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        setupUI(view)
        applyBranding(view)
        return view
    }

    private fun setupUI(view: View) {
        val rgSort = view.findViewById<RadioGroup>(R.id.rgSortOptions)
        val btnApply = view.findViewById<View>(R.id.btnApplySort)

        when(currentSort) {
            "NEWEST" -> view.findViewById<RadioButton>(R.id.rbNewest)?.isChecked = true
            "OLDEST" -> view.findViewById<RadioButton>(R.id.rbOldest)?.isChecked = true
            "HIGH_PRICE" -> view.findViewById<RadioButton>(R.id.rbHighPrice)?.isChecked = true
            "LOW_PRICE" -> view.findViewById<RadioButton>(R.id.rbLowPrice)?.isChecked = true
        }

        btnApply.setOnClickListener {
            val selectedSort = when(rgSort.checkedRadioButtonId) {
                R.id.rbOldest -> "OLDEST"
                R.id.rbHighPrice -> "HIGH_PRICE"
                R.id.rbLowPrice -> "LOW_PRICE"
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
        val themeColorStr = GymManager.getThemeColor(ctx)
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                
                // Root Background (28dp rounded)
                view.findViewById<android.widget.LinearLayout>(R.id.rootSortContainer)?.let { root ->
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    val bgColor = try { android.graphics.Color.parseColor(GymManager.getBgColor(ctx)) } 
                                  catch(e: Exception) { android.graphics.Color.parseColor("#151518") }
                    shape.setColor(bgColor)
                    val radius = (28 * ctx.resources.displayMetrics.density)
                    shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                    root.background = shape
                }

                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnApplySort)?.backgroundTintList = 
                    android.content.res.ColorStateList.valueOf(themeColor)
                
                // Radio buttons tint
                val rg = view.findViewById<RadioGroup>(R.id.rgSortOptions)
                for (i in 0 until rg.childCount) {
                    val rb = rg.getChildAt(i) as? RadioButton
                    rb?.buttonTintList = android.content.res.ColorStateList.valueOf(themeColor)
                }

            } catch (e: Exception) {
                Log.e("PaymentSortSheet", "Branding Error: ${e.message}")
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
