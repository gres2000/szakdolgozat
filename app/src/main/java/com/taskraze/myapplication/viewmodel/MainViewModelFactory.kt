package com.taskraze.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.model.calendar.UserData

class MainViewModelFactory(
    private val userId: String,
    private val userData: UserData
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(userId, userData) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}