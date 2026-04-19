package com.example.horizonsystems.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.horizonsystems.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

object DialogUtils {

    /**
     * Shows a premium, theme-aware confirmation dialog using the Elite design system.
     */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        negativeText: String? = null,
        onPositiveClick: (() -> Unit)? = null,
        onNegativeClick: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context, R.style.CustomDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_standard, null)

        val txtTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val divider = view.findViewById<View>(R.id.dialogDivider)
        val txtMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnPositive = view.findViewById<MaterialButton>(R.id.btnDialogPositive)
        val btnNegative = view.findViewById<MaterialButton>(R.id.btnDialogNegative)
        val card = view.findViewById<MaterialCardView>(R.id.dialogCard)

        txtTitle.text = title
        txtMessage.text = message
        btnPositive.text = positiveText
        
        if (negativeText != null) {
            btnNegative.visibility = View.VISIBLE
            btnNegative.text = negativeText
        }

        // Apply Gym Theme Branding
        val themeColorStr = GymManager.getThemeColor(context)
        val bgColorStr = GymManager.getBgColor(context)
        
        try {
            if (!themeColorStr.isNullOrEmpty()) {
                val themeColor = Color.parseColor(themeColorStr)
                btnPositive.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
                divider.setBackgroundColor(themeColor)
            }
            
            if (!bgColorStr.isNullOrEmpty()) {
                val bgColor = Color.parseColor(bgColorStr)
                card.setCardBackgroundColor(bgColor)
            }
        } catch (e: Exception) {}

        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnPositive.setOnClickListener {
            onPositiveClick?.invoke()
            dialog.dismiss()
        }
        
        btnNegative.setOnClickListener {
            onNegativeClick?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}
