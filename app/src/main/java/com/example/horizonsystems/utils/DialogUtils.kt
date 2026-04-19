package com.example.horizonsystems.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.horizonsystems.R
import com.google.android.material.button.MaterialButton

object DialogUtils {

    /**
     * Shows a premium, theme-aware confirmation dialog.
     */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "OK",
        onPositiveClick: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context, R.style.CustomDialogTheme)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_standard, null)

        val txtTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val txtMessage = view.findViewById<TextView>(R.id.dialogMessage)
        val btnPositive = view.findViewById<MaterialButton>(R.id.btnDialogPositive)

        txtTitle.text = title
        txtMessage.text = message
        btnPositive.text = positiveText

        // Apply Gym Theme Branding
        val themeColorStr = GymManager.getThemeColor(context)
        try {
            val themeColor = Color.parseColor(themeColorStr)
            btnPositive.backgroundTintList = android.content.res.ColorStateList.valueOf(themeColor)
            txtTitle.setTextColor(themeColor)
        } catch (e: Exception) {
            // Fallback used in XML
        }

        builder.setView(view)
        val dialog = builder.create()
        
        // Ensure background is transparent so the card's rounded corners and stroke are visible
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnPositive.setOnClickListener {
            onPositiveClick?.invoke()
            dialog.dismiss()
        }

        dialog.show()
    }
}
