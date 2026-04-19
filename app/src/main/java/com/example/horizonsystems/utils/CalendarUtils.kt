package com.example.horizonsystems.utils

import android.content.Context
import android.graphics.Color
import com.example.horizonsystems.R

object CalendarUtils {

    /**
     * Returns the appropriate MaterialDatePicker theme style based on the gym's brand color.
     */
    fun getCalendarTheme(context: Context): Int {
        val themeColorStr = GymManager.getThemeColor(context)
        if (themeColorStr.isNullOrEmpty()) return R.style.CustomMaterialCalendarTheme

        return try {
            val color = Color.parseColor(themeColorStr)
            val hsv = FloatArray(3)
            Color.colorToHSV(color, hsv)
            val hue = hsv[0]
            val saturation = hsv[1]

            // If saturation is very low, it's a neutral/gray color
            if (saturation < 0.2f) {
                return R.style.CustomMaterialCalendarTheme
            }

            // Map Hue to common branding flavors
            when {
                (hue >= 340 || hue < 15) -> R.style.CustomMaterialCalendarTheme_Red
                (hue >= 15 && hue < 45) -> R.style.CustomMaterialCalendarTheme_Red // Orange-ish mapped to Red
                (hue >= 45 && hue < 75) -> R.style.CustomMaterialCalendarTheme_Yellow
                (hue >= 75 && hue < 160) -> R.style.CustomMaterialCalendarTheme_Green
                (hue >= 160 && hue < 260) -> R.style.CustomMaterialCalendarTheme_Blue
                (hue >= 260 && hue < 310) -> R.style.CustomMaterialCalendarTheme_Purple
                (hue >= 310 && hue < 340) -> R.style.CustomMaterialCalendarTheme_Purple // Pinkish
                else -> R.style.CustomMaterialCalendarTheme
            }
        } catch (e: Exception) {
            R.style.CustomMaterialCalendarTheme
        }
    }
}
