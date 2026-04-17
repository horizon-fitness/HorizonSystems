package com.example.horizonsystems.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import com.example.horizonsystems.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

object ThemeUtils {

    /**
     * Applies the gym's theme color to the Activity's Status Bar and Navigation Bar.
     */
    fun applyThemeToActivity(activity: Activity) {
        val themeColor = GymManager.getThemeColor(activity)
        try {
            val color = Color.parseColor(themeColor)
            activity.window.statusBarColor = color
            activity.window.navigationBarColor = Color.BLACK
        } catch (e: Exception) {
            // Fallback to default
        }
    }

    /**
     * Re-tints the BottomNavigationView items (active state) with the gym's theme color.
     */
    fun applyThemeToBottomNav(bottomNav: BottomNavigationView) {
        val themeColor = GymManager.getThemeColor(bottomNav.context)
        try {
            val color = Color.parseColor(themeColor)
            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val colors = intArrayOf(
                color,
                Color.parseColor("#80FFFFFF") // 50% White for inactive
            )
            val colorList = ColorStateList(states, colors)
            
            bottomNav.itemIconTintList = colorList
            bottomNav.itemTextColor = colorList
            bottomNav.itemRippleColor = ColorStateList.valueOf(color.adjustAlpha(0.1f))
            
        } catch (e: Exception) {
            // Fallback to default purple
        }
    }

    /**
     * Recursively traverses a view hierarchy and applies the gym's theme color
     * to all common Material components found within.
     */
    fun applyThemeToView(view: View?) {
        if (view == null) return
        val context = view.context
        
        val themeColor = GymManager.getThemeColor(context)
        val bgColor = GymManager.getBgColor(context)
        val textColor = GymManager.getTextColor(context)
        val iconColor = GymManager.getIconColor(context)
        val surfaceColor = GymManager.getSurfaceColor(context)
        
        try {
            val palette = Palette(
                main = Color.parseColor(themeColor),
                bg = Color.parseColor(bgColor),
                text = Color.parseColor(textColor),
                icon = Color.parseColor(iconColor),
                surface = Color.parseColor(surfaceColor)
            )
            
            internalApplyTheme(view, palette)
        } catch (e: Exception) {
            // Error parsing colors
        }
    }

    private data class Palette(
        val main: Int,
        val bg: Int,
        val text: Int,
        val icon: Int,
        val surface: Int
    )

    private fun internalApplyTheme(view: View, p: Palette) {
        val colorListMain = ColorStateList.valueOf(p.main)
        val colorListText = ColorStateList.valueOf(p.text)
        val colorListIcon = ColorStateList.valueOf(p.icon)
        val colorListSurface = ColorStateList.valueOf(p.surface)

        when (view) {
            is MaterialButton -> {
                // Background usually uses Main, unless it's an outlined button
                if (view.backgroundTintList != null || view.id != View.NO_ID) {
                    view.backgroundTintList = colorListMain
                }
                // Text color for buttons
                if (view.currentTextColor == Color.WHITE || view.currentTextColor == Color.BLACK) {
                     // Keep it as is or tint? Usually buttons have their own text color
                }
            }
            is FloatingActionButton -> {
                view.backgroundTintList = colorListMain
            }
            is ProgressBar -> {
                view.indeterminateTintList = colorListMain
                view.progressTintList = colorListMain
            }
            is CompoundButton -> {
                // CheckBox and RadioButton use Main for primary accents
                view.buttonTintList = colorListMain
            }
            is Switch -> {
                view.thumbTintList = colorListMain
                view.trackTintList = ColorStateList.valueOf(p.main.adjustAlpha(0.3f))
            }
            is TabLayout -> {
                view.setSelectedTabIndicatorColor(p.main)
                view.setTabTextColors(p.text.adjustAlpha(0.6f), p.main)
            }
            is TextView -> {
                // Only tint general text if it's not already specialized (like titles)
                // We typically tint labels or descriptive text
                if (view.tag == "theme_text") {
                    view.setTextColor(p.text)
                }
            }
            is ImageView -> {
                // Tint icons specifically
                if (view.tag == "theme_icon") {
                    view.imageTintList = colorListIcon
                } else if (view.tag == "theme_tint") {
                    view.imageTintList = colorListMain
                }
            }
            is com.google.android.material.card.MaterialCardView -> {
                if (view.tag == "theme_surface") {
                    view.setCardBackgroundColor(colorListSurface)
                }
            }
            else -> {
                // For general views with surface tag (like LinearLayout input backgrounds)
                if (view.tag == "theme_surface") {
                    view.background?.setTint(p.surface)
                }
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                internalApplyTheme(view.getChildAt(i), p)
            }
        }
    }

    private fun Int.adjustAlpha(factor: Float): Int {
        val alpha = Math.round(Color.alpha(this) * factor)
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        return Color.argb(alpha, red, green, blue)
    }
}
