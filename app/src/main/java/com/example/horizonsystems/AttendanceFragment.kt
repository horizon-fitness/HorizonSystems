package com.example.horizonsystems

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.horizonsystems.utils.GymManager
import com.example.horizonsystems.utils.ThemeUtils
import com.google.android.material.card.MaterialCardView

class AttendanceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_attendance, container, false)
        
        applyBranding(view)
        ThemeUtils.applyThemeToView(view)
        
        return view
    }

    private fun applyBranding(view: View) {
        val ctx = context ?: return
        val themeColorStr = GymManager.getThemeColor(ctx)
        val cardColorStr = GymManager.getCardColor(ctx)
        val isAutoCard = GymManager.getAutoCardTheme(ctx) == "1"

        try {
            val themeColor = if (!themeColorStr.isNullOrEmpty()) Color.parseColor(themeColorStr) else Color.parseColor("#A855F7")
            
            // 1. Title Accent
            view.findViewById<TextView>(R.id.tvAttendanceThemeSubtitle)?.setTextColor(themeColor)
            
            // 2. Card Surface
            val cardSurface = if (isAutoCard) {
                Color.argb(13, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            } else {
                try { Color.parseColor(cardColorStr) } catch(e: Exception) { Color.parseColor("#141216") }
            }

            val cardConsistency = view.findViewById<MaterialCardView>(R.id.cardConsistency)
            cardConsistency?.setCardBackgroundColor(cardSurface)
            cardConsistency?.setStrokeColor(ColorStateList.valueOf(themeColor).withAlpha(40))

            // 3. Labels
            view.findViewById<TextView>(R.id.tvCheckInCount)?.setTextColor(themeColor)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
