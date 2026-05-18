package com.example.horizonsystems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import com.example.horizonsystems.utils.ThemeUtils
import com.example.horizonsystems.utils.GymManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ThemeUtils.applyThemeToView(view)
        applyBranding(view)

        // Profile Button Click
        view.findViewById<View>(R.id.btnSettingProfile)?.setOnClickListener {
            dismiss()
            val landingActivity = activity as? LandingActivity
            landingActivity?.loadFragment(ProfileFragment())
        }

        // Transactions Button Click
        view.findViewById<View>(R.id.btnSettingTransactions)?.setOnClickListener {
            dismiss()
            val landingActivity = activity as? LandingActivity
            landingActivity?.loadFragment(PaymentFragment())
        }

        // Membership Button Click
        view.findViewById<View>(R.id.btnSettingMembership)?.setOnClickListener {
            dismiss()
            val landingActivity = activity as? LandingActivity
            if (landingActivity != null) {
                landingActivity.loadFragment(MembershipFragment())
            } else {
                (activity as? MainActivity)?.loadFragment(MembershipFragment())
            }
        }

        // Sign Out Button Click
        view.findViewById<View>(R.id.btnSettingLogout)?.setOnClickListener {
            dismiss()
            val landingActivity = activity as? LandingActivity
            landingActivity?.performLogout()
        }
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val bgColorStr = GymManager.getBgColor(ctx)
        
        if (!themeColorStr.isNullOrEmpty()) {
            try {
                val themeColor = android.graphics.Color.parseColor(themeColorStr)
                view.findViewById<TextView>(R.id.sheetSubtitle)?.setTextColor(themeColor)
                
                // Brand the icon tints inside the actions
                val iconProfile = view.findViewById<ImageView>(R.id.iconProfile)
                val iconTransactions = view.findViewById<ImageView>(R.id.iconTransactions)
                val iconMembership = view.findViewById<ImageView>(R.id.iconMembership)
                
                iconProfile?.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)
                iconTransactions?.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)
                iconMembership?.imageTintList = android.content.res.ColorStateList.valueOf(themeColor)
            } catch (e: Exception) {}
        }

        if (!bgColorStr.isNullOrEmpty()) {
            try {
                val bgColor = android.graphics.Color.parseColor(bgColorStr)
                val shape = android.graphics.drawable.GradientDrawable()
                shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                shape.setColor(bgColor)
                val radius = (28 * ctx.resources.displayMetrics.density)
                shape.cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
                view.background = shape
            } catch (e: Exception) {}
        }
    }

    companion object {
        const val TAG = "SettingsSheet"
    }
}
