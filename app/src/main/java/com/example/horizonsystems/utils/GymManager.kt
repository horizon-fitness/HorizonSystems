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
    private const val KEY_ICON_COLOR = "selected_icon_color"
    private const val KEY_TEXT_COLOR = "selected_text_color"
    private const val KEY_BG_COLOR = "selected_bg_color"
    private const val KEY_CARD_COLOR = "selected_card_color"
    private const val KEY_AUTO_CARD_THEME = "selected_auto_card_theme"
    private const val KEY_BYPASS_COOKIE = "bypass_cookie"
    private const val KEY_BYPASS_UA = "bypass_ua"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_MEMBER_ID = "member_id"
    private const val KEY_OPENING_TIME = "selected_opening_time"
    private const val KEY_CLOSING_TIME = "selected_closing_time"
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
        iconColor: String? = null,
        textColor: String? = null,
        bgColor: String? = null,
        cardColor: String? = null,
        autoCardTheme: String? = null,
        openingTime: String? = null,
        closingTime: String? = null
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_GYM_SLUG, slug)
            putInt(KEY_GYM_ID, id)
            putString(KEY_TENANT_CODE, code)
            putString(KEY_GYM_NAME, name)
            putString(KEY_GYM_LOGO, logo)
            putString(KEY_THEME_COLOR, themeColor)
            putString(KEY_ICON_COLOR, iconColor)
            putString(KEY_TEXT_COLOR, textColor)
            putString(KEY_BG_COLOR, bgColor)
            putString(KEY_CARD_COLOR, cardColor)
            putString(KEY_AUTO_CARD_THEME, autoCardTheme)
            putString(KEY_OPENING_TIME, openingTime)
            putString(KEY_CLOSING_TIME, closingTime)
        }.apply()
    }

    fun updateBranding(
        context: Context,
        themeColor: String?,
        iconColor: String?,
        textColor: String?,
        bgColor: String?,
        cardColor: String?,
        autoCardTheme: String?
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_THEME_COLOR, themeColor)
            putString(KEY_ICON_COLOR, iconColor)
            putString(KEY_TEXT_COLOR, textColor)
            putString(KEY_BG_COLOR, bgColor)
            putString(KEY_CARD_COLOR, cardColor)
            putString(KEY_AUTO_CARD_THEME, autoCardTheme)
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

    fun getIconColor(context: Context): String {
        return getPrefs(context).getString(KEY_ICON_COLOR, "#A1A1AA") ?: "#A1A1AA"
    }

    fun getTextColor(context: Context): String {
        return getPrefs(context).getString(KEY_TEXT_COLOR, "#D1D5DB") ?: "#D1D5DB"
    }

    fun getBgColor(context: Context): String {
        return getPrefs(context).getString(KEY_BG_COLOR, "#0a090d") ?: "#0a090d"
    }

    fun getCardColor(context: Context): String {
        return getPrefs(context).getString(KEY_CARD_COLOR, "#141216") ?: "#141216"
    }

    fun getAutoCardTheme(context: Context): String {
        return getPrefs(context).getString(KEY_AUTO_CARD_THEME, "1") ?: "1"
    }

    fun getOpeningTime(context: Context): String {
        return getPrefs(context).getString(KEY_OPENING_TIME, "07:00:00") ?: "07:00:00"
    }

    fun getClosingTime(context: Context): String {
        return getPrefs(context).getString(KEY_CLOSING_TIME, "21:00:00") ?: "21:00:00"
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

        val baseUrl = "https://horizonfitnesscorp.gt.tc/"
        val fullLogoUrl = when {
            logoPath.startsWith("http") -> logoPath
            logoPath.startsWith("data:image") -> logoPath
            else -> baseUrl + logoPath.removePrefix("../").removePrefix("/")
        }

        // Reset image state to prevent flicker or tinting from previous views
        imageView.imageTintList = null
        imageView.setPadding(0, 0, 0, 0)
        
        // Use standard CENTER_INSIDE for logos to ensure they fit without being cut off
        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE

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
            .into(imageView)
    }

    /**
     * Re-fetch the latest branding data from the server and update local preferences.
     */
    suspend fun syncBranding(context: Context) {
        val tenantCode = getTenantCode(context)
        if (tenantCode == "000") return 

        try {
            val api = com.example.horizonsystems.network.RetrofitClient.getApi(getBypassCookie(context), getBypassUA(context))
            val response = api.connectGym(mapOf("gym" to tenantCode))
            
            if (response.isSuccessful && response.body() != null) {
                val b = response.body()!!
                updateBranding(
                    context,
                    b.themeColor,
                    b.iconColor,
                    b.textColor,
                    b.bgColor,
                    null, // cardColor not in connect_gym yet
                    null  // autoCardTheme not in connect_gym yet
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
