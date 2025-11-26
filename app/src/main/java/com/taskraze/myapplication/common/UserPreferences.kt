package com.taskraze.myapplication.common

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object UserPreferences {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"

    private fun getSharedPref(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setUserLoggedIn(context: Context, isLoggedIn: Boolean) {
        val editor = getSharedPref(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        return getSharedPref(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logoutUser(context: Context) {
        setUserLoggedIn(context, false)
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        return getSharedPref(context)
            .edit {
                putBoolean(KEY_OVERLAY_ENABLED, enabled)
            }
    }

    fun isOverlayEnabled(context: Context): Boolean {
        return getSharedPref(context).getBoolean(KEY_OVERLAY_ENABLED, false)
    }
}