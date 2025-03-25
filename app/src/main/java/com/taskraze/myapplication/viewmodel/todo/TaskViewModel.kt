package com.taskraze.myapplication.viewmodel.todo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.model.todo.TaskRepository

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
        downloadTasks()
        _dailyTasksList.value = repository.loadDailyTasksLocally()
        val list = repository.loadWeeklyTasksLocally()
        _weeklyTasksList.value = list
        uploadTasks()

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
        uploadTasks()
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
        uploadTasks()
    }

    private fun uploadTasks() {
        repository.uploadTasksToFirebase( _dailyTasksList.value ?: emptyList(), _weeklyTasksList.value ?: emptyList())
    }

    fun downloadTasks() {
        repository.downloadTasksFromFirebase(
            onSuccess = { dailyTasks, weeklyTasks ->
                _dailyTasksList.value = dailyTasks
                _weeklyTasksList.value = weeklyTasks
                saveTasks(dailyTasks, weeklyTasks)
            },
            onFailure = { e ->
                Log.e("Firebase", "Error downloading tasks", e)
            }
        )
    }
}