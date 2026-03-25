package com.example.horizonsystems.utils

import android.content.Context
import android.content.SharedPreferences

object GymManager {
    private const val PREF_NAME = "GymPrefs"
    private const val KEY_GYM_SLUG = "selected_gym_slug"
    private const val KEY_GYM_ID = "selected_gym_id" // numeric
    private const val KEY_TENANT_CODE = "selected_tenant_code" // alphanumeric
    private const val KEY_GYM_NAME = "selected_gym_name"
    private const val KEY_BYPASS_COOKIE = "bypass_cookie"
    private const val KEY_BYPASS_UA = "bypass_ua"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_SAVED_USERNAME = "saved_username"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val DEFAULT_SLUG = "horizon"


    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveGymSlug(context: Context, slug: String) {
        getPrefs(context).edit().putString(KEY_GYM_SLUG, slug).apply()
    }

    fun saveGymData(context: Context, slug: String, id: Int, code: String, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_GYM_SLUG, slug)
            putInt(KEY_GYM_ID, id)
            putString(KEY_TENANT_CODE, code)
            putString(KEY_GYM_NAME, name)
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

    fun saveLoginCredentials(context: Context, username: String, password: String) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ME, true)
            putString(KEY_SAVED_USERNAME, username)
            putString(KEY_SAVED_PASSWORD, password)
        }.apply()
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
}
