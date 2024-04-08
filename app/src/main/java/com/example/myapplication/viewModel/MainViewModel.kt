package com.example.myapplication.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.tasks.Task

class MainViewModel : ViewModel() {
    private val _someEvent = MutableLiveData<Task>()
    val someEvent: LiveData<Task>
        get() = _someEvent
    fun updateEvent(eventData: Task) {
        _someEvent.value = eventData
    }
}