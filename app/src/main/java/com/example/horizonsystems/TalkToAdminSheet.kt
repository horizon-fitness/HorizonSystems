package com.example.horizonsystems

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class TalkToAdminSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = try {
            inflater.inflate(R.layout.sheet_talk_to_admin, container, false)
        } catch (e: Exception) {
            Log.e("TalkToAdminSheet", "Inflation error: ${e.message}")
            null
        } ?: return null

        val editSubject = view.findViewById<TextInputEditText>(R.id.editSubject)
        val editMessage = view.findViewById<TextInputEditText>(R.id.editMessage)
        val btnSubmit = view.findViewById<View>(R.id.btnSubmitInquiry)

        btnSubmit?.setOnClickListener {
            val subject = editSubject?.text.toString()
            val message = editMessage?.text.toString()

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(context, "Please enter both subject and message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mock submission
            Toast.makeText(context, "Message sent to Admin! We'll get back to you soon.", Toast.LENGTH_LONG).show()
            dismiss()
        }

        ThemeUtils.applyThemeToView(view)
        applyBranding(view)

        return view
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
        val cardColorStr = GymManager.getCardColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) android.graphics.Color.parseColor(themeColorStr) else android.graphics.Color.parseColor("#8c2bee")
            
            // 1. Titles & Buttons
            view.findViewById<TextView>(R.id.sheetSubTitle)?.setTextColor(themeColor)
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitInquiry)?.let {
                it.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            }
            
            view.findViewById<android.widget.ImageView>(R.id.imgAdminIcon)?.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)

            // 2. Card Appearance Synchronization
            val cardColor = if (isAutoCard) {
                val r = android.graphics.Color.red(themeColor)
                val g = android.graphics.Color.green(themeColor)
                val b = android.graphics.Color.blue(themeColor)
                android.graphics.Color.argb(13, r, g, b) 
            } else {
                try { android.graphics.Color.parseColor(cardColorStr) } catch(e: Exception) { android.graphics.Color.parseColor("#141216") }
            }

            // Apply to Root Modal Background
            view.findViewById<android.widget.LinearLayout>(R.id.rootSheetContainer)?.let { root ->
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                shape.setColor(cardColor)
                val radius = (28 * ctx.resources.displayMetrics.density)
                shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                root.background = shape
            }

            // Apply to internal cards
            view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAdminTip)?.apply {
                setCardBackgroundColor(cardColor)
                setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1AFFFFFF")))
            }

        } catch (e: Exception) {
            Log.e("TalkToAdminSheet", "Branding Error: ${e.message}")
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
