package com.taskraze.myapplication.main.todo.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskraze.myapplication.main.todo.data_classes.TaskData
import com.taskraze.myapplication.view_model.MainViewModel

class TaskRepository(private val context: Context) {

    private val userName = MainViewModel.loggedInUser.email
    private val dailyFileName = userName + "_daily_tasks.json"
    private val weeklyFileName = userName + "_weekly_tasks.json"
    fun saveTasksLocally(dailyTaskList: List<TaskData>, weeklyTaskList: List<List<TaskData>>) {
        // daily save
        val dailyJson = Gson().toJson(dailyTaskList)
        context.openFileOutput(dailyFileName, Context.MODE_PRIVATE).use {
            it.write(dailyJson.toByteArray())
        }

        // weekly save
        val weeklyJson = Gson().toJson(weeklyTaskList)
        context.openFileOutput(weeklyFileName, Context.MODE_PRIVATE).use {
            it.write(weeklyJson.toByteArray())
        }
    }

    fun loadDailyTasksLocally(): List<TaskData> {
        return try {
            val file = context.getFileStreamPath(dailyFileName)
            if (!file.exists()) return emptyList()

            val json = context.openFileInput(dailyFileName).bufferedReader().readText()
            Gson().fromJson(json, object : TypeToken<List<TaskData>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadWeeklyTasksLocally(): List<List<TaskData>> {
        return try {
            val file = context.getFileStreamPath(weeklyFileName)
            if (!file.exists()) return List(7) { mutableListOf<TaskData>() }

            val json = context.openFileInput(weeklyFileName).bufferedReader().readText()
            val some = Gson().fromJson(json, object : TypeToken<List<List<TaskData>>>() {}.type)
                ?: List(7) { mutableListOf<TaskData>() }
            some
        } catch (e: Exception) {
            List(7) { mutableListOf<TaskData>() }
        }
    }

    fun uploadTasksToFirebase(userId: String, dailyList: List<TaskData>, weeklyList: List<List<TaskData>>) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .set(mapOf("tasks" to dailyList))
            .addOnSuccessListener { Log.d("Firebase", "Tasks uploaded!") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error uploading tasks", e) }
    }
}