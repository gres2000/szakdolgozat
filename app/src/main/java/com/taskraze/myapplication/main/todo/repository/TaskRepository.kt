package com.taskraze.myapplication.main.todo.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskraze.myapplication.main.todo.data_classes.TaskData
import com.taskraze.myapplication.view_model.MainViewModel

class TaskRepository(private val context: Context) {

    private val userId = MainViewModel.loggedInUser.email
    private val dailyFileName = userId + "_daily_tasks.json"
    private val weeklyFileName = userId + "_weekly_tasks.json"
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

    fun uploadTasksToFirebase(dailyList: List<TaskData>, weeklyList: List<List<TaskData>>) {
        val db = FirebaseFirestore.getInstance()
        val dailyDoc = db.collection("todo_tasks").document(userId + "_daily_tasks")
        dailyDoc.set(mapOf("tasks" to dailyList))
            .addOnSuccessListener { Log.d("Firebase", "Daily tasks uploaded!") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error uploading daily tasks", e) }

        val weeklyMap = weeklyList.mapIndexed { index, tasks -> index.toString() to tasks }.toMap()
        val weeklyDoc = db.collection("todo_tasks").document(userId + "_weekly_tasks")
        weeklyDoc.set(mapOf("tasks" to weeklyMap))
            .addOnSuccessListener { Log.d("Firebase", "Weekly tasks uploaded!") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error uploading weekly tasks", e) }
    }

    fun downloadTasksFromFirebase(onSuccess: (List<TaskData>, List<List<TaskData>>) -> Unit, onFailure: (Exception) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val dailyDoc = db.collection("todo_tasks").document(userId + "_daily_tasks")
        val weeklyDoc = db.collection("todo_tasks").document(userId + "_weekly_tasks")

        dailyDoc.get()
            .addOnSuccessListener { dailySnapshot ->
                val dailyTasksMap = dailySnapshot.data?.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                val dailyTasks = dailyTasksMap.map { Gson().fromJson(Gson().toJson(it), TaskData::class.java) }

                weeklyDoc.get()
                    .addOnSuccessListener { weeklySnapshot ->
                        val weeklyTasksMap = weeklySnapshot.data?.get("tasks") as? Map<String, List<Map<String, Any>>> ?: emptyMap()
                        val weeklyTasks = List(7) { dayId ->
                            weeklyTasksMap[dayId.toString()]?.map { Gson().fromJson(Gson().toJson(it), TaskData::class.java) }?.toMutableList() ?: mutableListOf()
                        }
                        onSuccess(dailyTasks, weeklyTasks)
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}