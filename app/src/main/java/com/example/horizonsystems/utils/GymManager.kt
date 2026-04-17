package com.example.horizonsystems.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

object GymManager {
    private const val PREF_NAME = "GymPrefs"
    private const val KEY_GYM_SLUG = "selected_gym_slug"
    private const val KEY_GYM_ID = "selected_gym_id" // numeric
    private const val KEY_TENANT_CODE = "selected_tenant_code" // alphanumeric
    private const val KEY_GYM_NAME = "selected_gym_name"
    private const val KEY_GYM_LOGO = "selected_gym_logo"
    private const val KEY_THEME_COLOR = "selected_theme_color"
    private const val KEY_BG_COLOR = "selected_bg_color"
    private const val KEY_TEXT_COLOR = "selected_text_color"
    private const val KEY_ICON_COLOR = "selected_icon_color"
    private const val KEY_SURFACE_COLOR = "selected_surface_color"
    private const val KEY_BYPASS_COOKIE = "bypass_cookie"
    private const val KEY_BYPASS_UA = "bypass_ua"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_MEMBER_ID = "member_id"
    private const val DEFAULT_SLUG = "horizon"


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveGymSlug(context: Context, slug: String) {
        getPrefs(context).edit().putString(KEY_GYM_SLUG, slug).apply()
    }

    fun saveGymData(
        context: Context, 
        slug: String, 
        id: Int, 
        code: String, 
        name: String, 
        logo: String? = null,
        themeColor: String? = null,
        bgColor: String? = null,
        textColor: String? = null,
        iconColor: String? = null,
        surfaceColor: String? = null
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_GYM_SLUG, slug)
            putInt(KEY_GYM_ID, id)
            putString(KEY_TENANT_CODE, code)
            putString(KEY_GYM_NAME, name)
            putString(KEY_GYM_LOGO, logo)
            putString(KEY_THEME_COLOR, themeColor)
            putString(KEY_BG_COLOR, bgColor)
            putString(KEY_TEXT_COLOR, textColor)
            putString(KEY_ICON_COLOR, iconColor)
            putString(KEY_SURFACE_COLOR, surfaceColor)
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
        return getPrefs(context).getString(KEY_GYM_NAME, "HORIZON SYSTEMS") ?: "HORIZON SYSTEMS"
    }
    
    fun getGymLogo(context: Context): String? {
        return getPrefs(context).getString(KEY_GYM_LOGO, null)
    }

    fun getThemeColor(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_COLOR, "#8c2bee") ?: "#8c2bee"
    }

    fun getBgColor(context: Context): String {
        return getPrefs(context).getString(KEY_BG_COLOR, "#0d0d0d") ?: "#0d0d0d"
    }

    fun getTextColor(context: Context): String {
        return getPrefs(context).getString(KEY_TEXT_COLOR, "#d1d5db") ?: "#d1d5db"
    }

    fun getIconColor(context: Context): String {
        return getPrefs(context).getString(KEY_ICON_COLOR, "#a1a1aa") ?: "#a1a1aa"
    }

    fun getSurfaceColor(context: Context): String {
        return getPrefs(context).getString(KEY_SURFACE_COLOR, "#141216") ?: "#141216"
    }

    fun saveLoginCredentials(context: Context, username: String, password: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ME, true)
            putString(KEY_SAVED_USERNAME, username)
            putString(KEY_SAVED_PASSWORD, password)
        }.apply()
    }

    fun updateSavedPassword(context: Context, newPassword: String) {
        getPrefs(context).edit().putString(KEY_SAVED_PASSWORD, newPassword).apply()
    }

    fun clearLoginCredentials(context: Context) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ME, false)
            remove(KEY_SAVED_USERNAME)
            remove(KEY_SAVED_PASSWORD)
        }.apply()
    }

    fun isRememberMeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_REMEMBER_ME, false)
    }

    fun getSavedUsername(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_USERNAME, "") ?: ""
    }

    fun getSavedPassword(context: Context): String {
        return getPrefs(context).getString(KEY_SAVED_PASSWORD, "") ?: ""
    }

    fun saveUserId(context: Context, userId: Int) {
        getPrefs(context).edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): Int {
        return getPrefs(context).getInt(KEY_USER_ID, -1)
    }

    fun saveMemberId(context: Context, memberId: Int) {
        getPrefs(context).edit().putInt(KEY_MEMBER_ID, memberId).apply()
    }

    fun getMemberId(context: Context): Int {
        return getPrefs(context).getInt(KEY_MEMBER_ID, -1)
    }

    /**
     * Standardized method to load gym logos with security bypass headers
     */
    fun loadLogo(context: Context, logoPath: String?, imageView: ImageView?) {
        if (imageView == null || logoPath.isNullOrEmpty()) return

        val baseUrl = "https://horizonfitnesscorp.gt.tc/horizon/"
        val fullLogoUrl = when {
            logoPath.startsWith("http") -> logoPath
            logoPath.startsWith("data:image") -> logoPath
            else -> baseUrl + logoPath.removePrefix("../").removePrefix("/")
        }

        // Reset image state to prevent flicker or tinting from previous views
        imageView.imageTintList = null
        imageView.setPadding(0, 0, 0, 0)
        
        // Use standard FIT_CENTER for logos
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

        val cookie = getBypassCookie(context)
        val ua = getBypassUA(context)

        val glideUrl = if (cookie.isNotEmpty() && ua.isNotEmpty() && fullLogoUrl.startsWith("http")) {
            GlideUrl(
                fullLogoUrl,
                LazyHeaders.Builder()
                    .addHeader("Cookie", cookie)
                    .addHeader("User-Agent", ua)
                    .build()
            )
        } else {
            fullLogoUrl
        }

        Glide.with(context)
            .load(glideUrl)
            .circleCrop()
            .into(imageView)
    }
}
