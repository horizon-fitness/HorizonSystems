package com.example.horizonsystems.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.CheckBox
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
        val themeColorStr = GymManager.getThemeColor(context)
        
        try {
            val color = Color.parseColor(themeColorStr)
            val colorStateList = ColorStateList.valueOf(color)
            
            internalApplyTheme(view, color, colorStateList)
        } catch (e: Exception) {
            // Error parsing color
        }
    }

    private fun internalApplyTheme(view: View, color: Int, colorList: ColorStateList) {
        when (view) {
            is MaterialButton -> {
                // If the button has a specific ID or style that should be branded
                // Often we only want to tint filled buttons, not text buttons (unless specified)
                if (view.backgroundTintList != null || view.id != View.NO_ID) {
                     view.backgroundTintList = colorList
                }
            }
            is FloatingActionButton -> {
                view.backgroundTintList = colorList
            }
            is ProgressBar -> {
                view.indeterminateTintList = colorList
                view.progressTintList = colorList
            }
            is TabLayout -> {
                view.setSelectedTabIndicatorColor(color)
                view.setTabTextColors(Color.parseColor("#80FFFFFF"), color)
            }
            is ImageView -> {
                // Only tint if it's meant to be an icon (check if it has a specific tag or name)
                if (view.tag == "theme_tint") {
                    view.imageTintList = colorList
                }
            }
            is CheckBox -> {
                val states = arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                )
                val colors = intArrayOf(
                    color,
                    Color.parseColor("#80FFFFFF")
                )
                view.buttonTintList = ColorStateList(states, colors)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                internalApplyTheme(view.getChildAt(i), color, colorList)
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
