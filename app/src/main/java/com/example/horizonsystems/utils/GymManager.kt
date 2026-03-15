package com.example.horizonsystems.utils

import android.content.Context
import android.content.SharedPreferences

object GymManager {
    private const val PREF_NAME = "GymPrefs"
    private const val KEY_GYM_SLUG = "selected_gym_slug"
    private const val KEY_GYM_ID = "selected_gym_id"
    private const val KEY_GYM_NAME = "selected_gym_name"
    private const val DEFAULT_SLUG = "corsanofitness"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveGymData(context: Context, slug: String, id: Int, name: String) {
        getPrefs(context).edit().apply {
            putString(KEY_GYM_SLUG, slug)
            putInt(KEY_GYM_ID, id)
            putString(KEY_GYM_NAME, name)
        }.apply()
    }

    fun getGymSlug(context: Context): String {
        return getPrefs(context).getString(KEY_GYM_SLUG, DEFAULT_SLUG) ?: DEFAULT_SLUG
    }

    fun getGymId(context: Context): Int {
        return getPrefs(context).getInt(KEY_GYM_ID, 1)
    }

    fun getGymName(context: Context): String {
        return getPrefs(context).getString(KEY_GYM_NAME, "Horizon Fitness") ?: "Horizon Fitness"
    }
}

