package com.taskraze.myapplication.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object UserPreferences {
    private const val KEY_PREF_NAME = "user_prefs"

    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_RECOMMENDATIONS_ENABLED = "recommendations_enabled"

    private fun getSharedPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(KEY_PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setUserLoggedIn(context: Context, isLoggedIn: Boolean) {
        getSharedPref(context).edit {
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        }
    }

    fun isUserLoggedIn(context: Context): Boolean {
        return getSharedPref(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logoutUser(context: Context) {
        setUserLoggedIn(context, false)
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        getSharedPref(context).edit {
            putBoolean(KEY_OVERLAY_ENABLED, enabled)
        }
    }

    fun isOverlayEnabled(context: Context): Boolean {
        return getSharedPref(context).getBoolean(KEY_OVERLAY_ENABLED, false)
    }

    fun setRecommendationsEnabled(context: Context, enabled: Boolean) {
        getSharedPref(context).edit {
            putBoolean(KEY_RECOMMENDATIONS_ENABLED, enabled)
        }
    }

    fun isRecommendationsEnabled(context: Context): Boolean {
        val prefs = getSharedPref(context)
        if (!prefs.contains(KEY_RECOMMENDATIONS_ENABLED)) {
            prefs.edit { putBoolean(KEY_RECOMMENDATIONS_ENABLED, false) }
            return false
        }
        return prefs.getBoolean(KEY_RECOMMENDATIONS_ENABLED, false)
    }
}
