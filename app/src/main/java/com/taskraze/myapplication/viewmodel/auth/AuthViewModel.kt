package com.taskraze.myapplication.viewmodel.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.core.content.edit

class AuthViewModel(private val context: Context) : ViewModel() {

    private val authRepository = AuthRepository()

    private val _loggedInUser = MutableStateFlow<UserData?>(null)
    val loggedInUser = _loggedInUser

    init {
        val cachedUser = getCachedUser()
        if (cachedUser != null) {
            _loggedInUser.value = cachedUser
        }

        viewModelScope.launch {
            val freshUser = authRepository.fetchUserDetails()
            _loggedInUser.value = freshUser
            saveUserToCache(freshUser)
        }
    }

    fun fetchAndCacheUser(userId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val user = authRepository.fetchUserDetails()
            _loggedInUser.value = user
            saveUserToCache(user)
            onComplete?.invoke()
        }
    }

    fun getUserId(): String {
        return _loggedInUser.value?.userId.orEmpty()
    }

    suspend fun awaitUserId(): String {
        val user = loggedInUser.first { it?.userId?.isNotEmpty() == true }
        return user!!.userId
    }

    private fun saveUserToCache(user: UserData) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("userId", user.userId)
            put("username", user.username)
            put("email", user.email)
        }
        prefs.edit { putString("user_data", json.toString()) }
    }

    fun getCachedUser(): UserData? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("user_data", null) ?: return null
        return try {
            val json = JSONObject(jsonString)
            UserData(
                userId = json.getString("userId"),
                username = json.optString("name", ""),
                email = json.optString("name", "")
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to parse cached user", e)
            null
        }
    }
}
