package com.example.myapplication.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.tasks.Task

class MainViewModel : ViewModel() {
    private val _someEvent = MutableLiveData<Task>()
    private var _taskReady = false
    private val _dayId = MutableLiveData<Int>()
    var taskId: Int = -1
    private var _isNewTask = false
    lateinit var taskStorage: Task
    val dayId
        get() = _dayId
    val taskReady
        get() = _taskReady
    val someEvent
        get() = _someEvent
    val isNewTask
        get() = _isNewTask
    private val _weeklyTasksList: List<MutableList<Task>> = List(7) { mutableListOf() }
    val weeklyTasksList
        get() = _weeklyTasksList
        init {
        for (i in 0 until 7) {
            val task1 = Task(0, "Munka", "leírás", "16:02", false)
            val task2 = Task(1, "Edzés", "leírás", "18:02", false)
            val task3 = Task(2, "Séta", "leírás", "20:02", false)
            _weeklyTasksList[i].apply {
                add(task1)
                add(task2)
                add(task3)
            }
        }
    }

    fun updateEvent(eventData: Task) {
        _someEvent.value = eventData
    }

    fun toggleNewTask() {
        _isNewTask = !_isNewTask
    }

    fun setNewTaskFalse() {
        _isNewTask = false
    }

    fun toggleTaskReady() {
        _taskReady = !_taskReady
    }

    fun authenticateUser() {
//        TODO("Not yet implemented")
    }
}