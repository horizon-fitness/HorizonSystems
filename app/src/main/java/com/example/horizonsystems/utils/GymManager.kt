package com.example.horizonsystems.utils

import android.content.Context
import android.content.SharedPreferences

object GymManager {
    private const val PREF_NAME = "GymPrefs"
    private const val KEY_GYM_SLUG = "selected_gym_slug"
    private const val KEY_GYM_ID = "selected_gym_id" // numeric
    private const val KEY_TENANT_CODE = "selected_tenant_code" // alphanumeric
    private const val KEY_GYM_NAME = "selected_gym_name"
    private const val KEY_THEME_COLOR = "theme_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_LOGO_PATH = "logo_path"
    private const val KEY_ABOUT_TEXT = "about_text"
    private const val KEY_BYPASS_COOKIE = "bypass_cookie"
    private const val KEY_BYPASS_UA = "bypass_ua"
    private const val DEFAULT_SLUG = "horizon"


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveGymSlug(context: Context, slug: String) {
        getPrefs(context).edit().putString(KEY_GYM_SLUG, slug).apply()
    }

    fun saveGymData(context: Context, slug: String, id: Int, code: String, name: String, theme: String? = null, bg: String? = null, logo: String? = null, about: String? = null) {
        getPrefs(context).edit().apply {
            putString(KEY_GYM_SLUG, slug)
            putInt(KEY_GYM_ID, id)
            putString(KEY_TENANT_CODE, code)
            putString(KEY_GYM_NAME, name)
            theme?.let { putString(KEY_THEME_COLOR, it) }
            bg?.let { putString(KEY_BG_COLOR, it) }
            logo?.let { putString(KEY_LOGO_PATH, it) }
            about?.let { putString(KEY_ABOUT_TEXT, it) }
        }.apply()
    }

    fun saveBypassCredentials(context: Context, cookie: String, ua: String) {
        getPrefs(context).edit().apply {
            putString(KEY_BYPASS_COOKIE, cookie)
            putString(KEY_BYPASS_UA, ua)
        }.apply()
    }

    fun getBypassCookie(context: Context): String {
        return getPrefs(context).getString(KEY_BYPASS_COOKIE, "") ?: ""
    }

    fun getBypassUA(context: Context): String {
        return getPrefs(context).getString(KEY_BYPASS_UA, "") ?: ""
    }

    fun getGymSlug(context: Context): String {

        return getPrefs(context).getString(KEY_GYM_SLUG, DEFAULT_SLUG) ?: DEFAULT_SLUG
    }

    fun getTenantId(context: Context): Int {
        return getPrefs(context).getInt(KEY_GYM_ID, 1)
    }

    fun getTenantCode(context: Context): String {
        return getPrefs(context).getString(KEY_TENANT_CODE, "000") ?: "000"
    }

    fun getGymName(context: Context): String {
        return getPrefs(context).getString(KEY_GYM_NAME, "Horizon Fitness") ?: "Horizon Fitness"
    }

    fun getThemeColor(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_COLOR, "#8c2bee") ?: "#8c2bee"
    }

    fun getBgColor(context: Context): String {
        return getPrefs(context).getString(KEY_BG_COLOR, "#0a090d") ?: "#0a090d"
    }

    fun getLogoPath(context: Context): String? {
        return getPrefs(context).getString(KEY_LOGO_PATH, null)
    }

    fun getAboutText(context: Context): String? {
        return getPrefs(context).getString(KEY_ABOUT_TEXT, null)
    }
}
