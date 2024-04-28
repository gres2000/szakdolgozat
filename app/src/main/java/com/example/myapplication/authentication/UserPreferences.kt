package com.example.myapplication.authentication

import android.content.Context
import android.content.SharedPreferences

object UserPreferences {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setUserLoggedIn(context: Context, isLoggedIn: Boolean) {
        val editor = getSharedPreferences(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
        editor.apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logoutUser(context: Context) {
        setUserLoggedIn(context, false)
    }
}