package com.taskraze.myapplication.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _loggedInUser = MutableStateFlow<UserData?>(null)
    val loggedInUser = _loggedInUser

    init {
        viewModelScope.launch {
            val user = authRepository.fetchUserDetails()
            _loggedInUser.value = user
            Log.d("AuthViewModel", "User loaded: $user")
        }
    }

    fun getUserId(): String {
        return _loggedInUser.value?.userId.orEmpty()
    }

    suspend fun awaitUserId(): String {
        val user = loggedInUser.first { it?.userId?.isNotEmpty() == true }
        return user!!.userId
    }
}
