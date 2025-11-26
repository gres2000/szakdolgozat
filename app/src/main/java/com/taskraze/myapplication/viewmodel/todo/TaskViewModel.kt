package com.taskraze.myapplication.viewmodel.todo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.model.todo.TaskRepository
import com.taskraze.myapplication.view.todo.daily.DailyFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository()

    private val _dailyTasksList = MutableStateFlow<List<TaskData>>(emptyList())
    private val _weeklyTasksList = MutableStateFlow(List(7) { mutableListOf<TaskData>() })
    val dailyTasksList: StateFlow<List<TaskData>> = _dailyTasksList
    val weeklyTasksList: StateFlow<List<List<TaskData>>> = _weeklyTasksList

    fun loadTasks() {
        viewModelScope.launch {
            val (daily, weekly) = repository.getTasks()
            _dailyTasksList.value  = daily
            _weeklyTasksList.value  = weekly as List<MutableList<TaskData>>
        }
    }

    fun addDailyTask(task: TaskData) {

        val currentList = _dailyTasksList.value.toMutableList()
        currentList.add(task)
        _dailyTasksList.value = currentList

        uploadTasks()
    }

    fun updateDailyTask(task: TaskData) {
        val currentList = _dailyTasksList.value.toMutableList()

        val index = currentList.indexOfFirst { it.taskId == task.taskId }
        if (index != -1) {
            currentList[index] = task
            _dailyTasksList.value = currentList
        }

        uploadTasks()
    }

    fun addWeeklyTask(task: TaskData, dayId: Int) {

        val currentList = _weeklyTasksList.value.toMutableList()

        val updatedDayList = currentList[dayId].toMutableList().apply {
            add(task)
        }

        currentList[dayId] = updatedDayList
        _weeklyTasksList.value = currentList

        uploadTasks()
    }

    fun updateWeeklyTask(task: TaskData, dayId: Int) {
        val currentList = _weeklyTasksList.value.toMutableList()

        val dayList = currentList.getOrNull(dayId)?.toMutableList()

        if (dayList != null) {
            val taskIndex = dayList.indexOfFirst { it.taskId == task.taskId }

            if (taskIndex != -1) {
                dayList[taskIndex] = task

                currentList[dayId] = dayList

                _weeklyTasksList.value = currentList
            }
        }

        uploadTasks()
    }

    private fun uploadTasks() {
        viewModelScope.launch {
            repository.updateTasks(_dailyTasksList.value, _weeklyTasksList.value)
        }
    }

    fun removeTask(taskId: String, mode: DailyFragment.Mode, dayId: Int? = null) {
        if (mode == DailyFragment.Mode.DAILY) {
            val currentList = _dailyTasksList.value.toMutableList()
            val index = currentList.indexOfFirst { it.taskId == taskId }
            if (index != -1) {
                currentList.removeAt(index)
                _dailyTasksList.value = currentList
            }
        } else if (mode == DailyFragment.Mode.WEEKLY && dayId != null) {
            val currentList = _weeklyTasksList.value.toMutableList()
            val dayList = currentList.getOrNull(dayId)?.toMutableList()
            if (dayList != null) {
                val index = dayList.indexOfFirst { it.taskId == taskId }
                if (index != -1) {
                    dayList.removeAt(index)
                    currentList[dayId] = dayList
                    _weeklyTasksList.value = currentList
                }
            }
        }

        uploadTasks()
    }

    fun toggleChecked(taskId: String, isChecked: Boolean, mode: DailyFragment.Mode, dayId: Int? = null) {
        if (mode == DailyFragment.Mode.DAILY) {
            val currentList = _dailyTasksList.value.toMutableList()
            val index = currentList.indexOfFirst { it.taskId == taskId }
            if (index != -1) {
                val task = currentList[index]
                currentList[index] = task.copy(isChecked = isChecked)
                _dailyTasksList.value = currentList
            }
        } else if (mode == DailyFragment.Mode.WEEKLY && dayId != null) {
            val currentList = _weeklyTasksList.value.toMutableList()
            val dayList = currentList.getOrNull(dayId)?.toMutableList()
            if (dayList != null) {
                val index = dayList.indexOfFirst { it.taskId == taskId }
                if (index != -1) {
                    val task = dayList[index]
                    dayList[index] = task.copy(isChecked = isChecked)
                    currentList[dayId] = dayList
                    _weeklyTasksList.value = currentList
                }
            }
        }

        uploadTasks()
    }

}