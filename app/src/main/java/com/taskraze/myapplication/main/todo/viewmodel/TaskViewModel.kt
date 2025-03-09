package com.taskraze.myapplication.main.todo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.main.todo.data_classes.TaskData
import com.taskraze.myapplication.main.todo.repository.TaskRepository
import com.taskraze.myapplication.view_model.MainViewModel

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(application.applicationContext)

    private val _dailyTasksList = MutableLiveData<List<TaskData>>()
    private val _weeklyTasksList = MutableLiveData<List<List<TaskData>>>()
    val dailyTasksList: LiveData<List<TaskData>> = _dailyTasksList
    val weeklyTasksList: LiveData<List<List<TaskData>>> = _weeklyTasksList

    private fun saveTasks(dailyList: List<TaskData>, weeklyList: List<List<TaskData>>) {
        repository.saveTasksLocally(dailyList, weeklyList)
        _dailyTasksList.value = dailyList
        _weeklyTasksList.value = weeklyList
    }

    fun loadTasks() {
        _dailyTasksList.value = repository.loadDailyTasksLocally()
        val list = repository.loadWeeklyTasksLocally()
        _weeklyTasksList.value = list
    }

    fun addDailyTask(task: TaskData) {

        val currentList = _dailyTasksList.value.orEmpty().toMutableList()
        currentList.add(task)
        _dailyTasksList.value = currentList

        saveTasks(dailyTasksList.value ?: emptyList(), weeklyTasksList.value ?: emptyList())
    }

    fun updateDailyTask(task: TaskData) {
        val currentList = _dailyTasksList.value.orEmpty().toMutableList()

        val index = currentList.indexOfFirst { it.taskId == task.taskId }
        if (index != -1) {
            currentList[index] = task
            _dailyTasksList.value = currentList
        }

        saveTasks(dailyTasksList.value ?: emptyList(), weeklyTasksList.value ?: emptyList())
    }

    fun addWeeklyTask(task: TaskData, dayId: Int) {

        val currentList = _weeklyTasksList.value.orEmpty().toMutableList()

        val updatedDayList = currentList[dayId].toMutableList().apply {
            add(task)
        }

        currentList[dayId] = updatedDayList

        _weeklyTasksList.value = currentList

        saveTasks(dailyTasksList.value ?: emptyList(), weeklyTasksList.value ?: emptyList())
    }

    fun updateWeeklyTask(task: TaskData, dayId: Int) {
        val currentList = _weeklyTasksList.value.orEmpty().toMutableList()

        val dayList = currentList.getOrNull(dayId)?.toMutableList()

        if (dayList != null) {
            val taskIndex = dayList.indexOfFirst { it.taskId == task.taskId }

            if (taskIndex != -1) {
                dayList[taskIndex] = task

                currentList[dayId] = dayList

                _weeklyTasksList.value = currentList
            }
        }



        saveTasks(dailyTasksList.value ?: emptyList(), weeklyTasksList.value ?: emptyList())
    }

    fun uploadTasks(userId: String) {
        repository.uploadTasksToFirebase(userId, _dailyTasksList.value ?: emptyList(), _weeklyTasksList.value ?: emptyList())
    }
}