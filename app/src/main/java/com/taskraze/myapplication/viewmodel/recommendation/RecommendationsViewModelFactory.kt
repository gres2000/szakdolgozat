package com.taskraze.myapplication.viewmodel.recommendation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RecommendationsViewModelFactory(
    private val userId: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecommendationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecommendationsViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
