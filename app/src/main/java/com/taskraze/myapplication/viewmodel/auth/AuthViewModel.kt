package com.taskraze.myapplication.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.room_database.data_classes.User
import kotlinx.coroutines.launch

object AuthViewModel: ViewModel() {
    private val authRepository = AuthRepository()
    lateinit var loggedInUser: User

    init {
        viewModelScope.launch {
            loggedInUser = authRepository.fetchUserDetails()
        }
    }

}