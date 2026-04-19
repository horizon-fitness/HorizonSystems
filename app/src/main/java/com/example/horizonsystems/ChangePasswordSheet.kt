package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.network.RetrofitClient
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordSheet : BottomSheetDialogFragment() {

    private var isPasswordValid = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.sheet_change_password, container, false)
        
        val editCurrent = view.findViewById<TextInputEditText>(R.id.editCurrentPassword)
        val editNew = view.findViewById<TextInputEditText>(R.id.editNewPassword)
        val editConfirm = view.findViewById<TextInputEditText>(R.id.editConfirmPassword)
        val btnUpdate = view.findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val btnCancel = view.findViewById<View>(R.id.btnCancelPassword)
        val pbUpdating = view.findViewById<ProgressBar>(R.id.pbUpdating)

        val reqLen = view.findViewById<TextView>(R.id.reqLen)
        val reqNum = view.findViewById<TextView>(R.id.reqNum)
        val reqSpecial = view.findViewById<TextView>(R.id.reqSpecial)
        val reqCaps = view.findViewById<TextView>(R.id.reqCaps)

        btnCancel.setOnClickListener { dismiss() }

        setupPasswordComplexityWatcher(editNew, reqLen, reqNum, reqSpecial, reqCaps, btnUpdate)

        btnUpdate.setOnClickListener {
            if (!isPasswordValid) {
                Toast.makeText(requireContext(), "Password does not meet requirements", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val current = editCurrent.text.toString()
            val newPass = editNew.text.toString()
            val confirm = editConfirm.text.toString()

            if (newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = activity?.intent?.getIntExtra("user_id", -1) ?: -1
            if (userId == -1) {
                Toast.makeText(requireContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "user_id" to userId,
                "current_password" to current,
                "new_password" to newPass
            )

            submitPasswordUpdate(updates, pbUpdating, btnUpdate)
        }

        ThemeUtils.applyThemeToView(view)
        applyBranding(view)

        return view
    }

    private fun setupPasswordComplexityWatcher(
        edit: TextInputEditText,
        reqLen: TextView,
        reqNum: TextView,
        reqSpecial: TextView,
        reqCaps: TextView,
        btn: MaterialButton
    ) {
        val themeColorStr = GymManager.getThemeColor(requireContext())
        val accentColor = try { Color.parseColor(themeColorStr) } catch (e: Exception) { Color.WHITE }
        val inactiveColor = Color.parseColor("#40FFFFFF")

        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val p = s.toString()
                
                val hasLen = p.length >= 8
                val hasNum = p.any { it.isDigit() }
                val hasSpecial = p.any { !it.isLetterOrDigit() }
                val hasCaps = p.any { it.isUpperCase() }

                updateReq(reqLen, hasLen, accentColor, inactiveColor, "Minimum 8 characters")
                updateReq(reqNum, hasNum, accentColor, inactiveColor, "At least 1 number")
                updateReq(reqSpecial, hasSpecial, accentColor, inactiveColor, "At least 1 special character")
                updateReq(reqCaps, hasCaps, accentColor, inactiveColor, "At least 1 uppercase letter")

                isPasswordValid = hasLen && hasNum && hasSpecial && hasCaps
                
                // Optional: visual feedback on button
                btn.alpha = if (isPasswordValid) 1.0f else 0.5f
            }
        })
    }

    private fun updateReq(v: TextView, satisfied: Boolean, activeColor: Int, inactiveColor: Int, text: String) {
        v.text = if (satisfied) "✓ $text" else "• $text"
        v.setTextColor(if (satisfied) activeColor else inactiveColor)
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = Color.parseColor(themeColorStr)
                val themeList = ColorStateList.valueOf(themeColor)
                
                val defaultPurple = Color.parseColor("#A855F7")
                tintTextsByColor(view as ViewGroup, defaultPurple, themeColor)

                // Button Branding
                view.findViewById<MaterialButton>(R.id.btnUpdatePassword)?.let { btn ->
                    btn.backgroundTintList = themeList
                    btn.iconTint = ColorStateList.valueOf(Color.WHITE)
                    btn.alpha = 0.5f // Default inactive
                }

                tintInputLayouts(view as ViewGroup, themeColor)
                view.findViewById<ProgressBar>(R.id.pbUpdating)?.indeterminateTintList = themeList

                // Dynamic Background Color
                val bgColorStr = GymManager.getBgColor(ctx)
                if (!bgColorStr.isNullOrEmpty()) {
                    val bgColor = Color.parseColor(bgColorStr)
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    shape.setColor(bgColor)
                    val radius = (28 * ctx.resources.displayMetrics.density)
                    shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                    view.background = shape
                }

            } catch (e: Exception) {}
        }
    }

    private fun tintTextsByColor(view: ViewGroup, targetColor: Int, themeColor: Int) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextView && child.currentTextColor == targetColor) {
                child.setTextColor(themeColor)
            } else if (child is ViewGroup) {
                tintTextsByColor(child, targetColor, themeColor)
            }
        }
    }

    private fun tintInputLayouts(view: ViewGroup, themeColor: Int) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is TextInputLayout) {
                child.boxStrokeColor = themeColor
                child.hintTextColor = ColorStateList.valueOf(themeColor)
                child.setEndIconTintList(ColorStateList.valueOf(themeColor))
            } else if (child is ViewGroup) {
                tintInputLayouts(child, themeColor)
            }
        }
    }

    private fun submitPasswordUpdate(updates: Map<String, Any>, pb: ProgressBar?, btn: MaterialButton?) {
        val ctx = context ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                pb?.visibility = View.VISIBLE
                btn?.isEnabled = false
                btn?.alpha = 0.5f
            }

            try {
                val cookie = GymManager.getBypassCookie(ctx)
                val ua = GymManager.getBypassUA(ctx)
                val api = RetrofitClient.getApi(cookie, ua)
                
                val response = api.changePassword(updates)

                withContext(Dispatchers.Main) {
                    pb?.visibility = View.GONE
                    btn?.isEnabled = true
                    btn?.alpha = if (isPasswordValid) 1.0f else 0.5f

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(ctx, "Password updated successfully!", Toast.LENGTH_LONG).show()
                        GymManager.updateSavedPassword(ctx, updates["new_password"].toString())
                        dismiss()
                    } else {
                        val msg = response.body()?.message ?: "Failed to update password. Please check your current password."
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pb?.visibility = View.GONE
                    btn?.isEnabled = true
                    btn?.alpha = if (isPasswordValid) 1.0f else 0.5f
                    Toast.makeText(ctx, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            it.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
