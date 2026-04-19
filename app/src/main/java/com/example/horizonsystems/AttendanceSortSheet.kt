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
        val btnClose = view.findViewById<View>(R.id.btnCloseSheet)
        btnClose?.setOnClickListener { dismiss() }

        val sortOptions = listOf(
            view.findViewById<TextView>(R.id.optionNewest),
            view.findViewById<TextView>(R.id.optionOldest)
        )

        sortOptions.forEach { option ->
            option?.setOnClickListener {
                val selectedSort = when(option.id) {
                    R.id.optionOldest -> "OLDEST"
                    else -> "NEWEST"
                }
                listener?.onSortSelected(selectedSort)
                dismiss()
            }
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColor = Color.parseColor(GymManager.getThemeColor(ctx))
        val bgColor = Color.parseColor(GymManager.getBgColor(ctx))
        
        view.findViewById<View>(R.id.sheetRoot)?.setBackgroundColor(bgColor)
    }
}
