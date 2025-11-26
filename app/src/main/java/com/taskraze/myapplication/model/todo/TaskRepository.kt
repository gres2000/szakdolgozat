package com.taskraze.myapplication.model.todo

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

class TaskRepository {

    data class DailyWrapper(
        val tasks: List<TaskData> = emptyList()
    )

    data class WeeklyWrapper(
        val tasks: Map<String, List<TaskData>> = emptyMap()
    )

    private val firestoreDB = FirebaseFirestore.getInstance()
    private val dailyTasksCollection = firestoreDB.collection("todo_tasks")
    private val weeklyTasksCollection = firestoreDB.collection("todo_tasks")

    suspend fun updateTasks(dailyList: List<TaskData>, weeklyList: List<List<TaskData>>) {
        val userId = AuthViewModel.awaitUserId()
        dailyTasksCollection.document(userId + "_daily_tasks").set(mapOf("tasks" to dailyList))
            .addOnSuccessListener { Log.d("Firebase", "Daily tasks uploaded") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error uploading daily tasks", e) }

        val weeklyMap = weeklyList.mapIndexed { index, tasks -> index.toString() to tasks }.toMap()
        weeklyTasksCollection.document(userId + "_weekly_tasks").set(mapOf("tasks" to weeklyMap))
            .addOnSuccessListener { Log.d("Firebase", "Weekly tasks uploaded") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error uploading weekly tasks", e) }
    }

    suspend fun getTasks(): Pair<List<TaskData>, List<List<TaskData>>> {
        val userId = AuthViewModel.awaitUserId()
        return try {
            val dailySnapshot = dailyTasksCollection.document(userId + "_daily_tasks").get().await()
            val weeklySnapshot = weeklyTasksCollection.document(userId + "_weekly_tasks").get().await()

            val dailyTasks = if (dailySnapshot.exists()) {
                dailySnapshot.toObject(DailyWrapper::class.java)?.tasks ?: emptyList()
            } else emptyList()

            val weeklyMap = if (weeklySnapshot.exists()) {
                weeklySnapshot.toObject(WeeklyWrapper::class.java)?.tasks ?: emptyMap()
            } else emptyMap()

            val weeklyList = weeklyMap.entries
                .sortedBy { it.key.toInt() }
                .map { it.value }
                .let { list -> if (list.size == 7) list else List(7) { i -> list.getOrNull(i) ?: mutableListOf() } }

            Pair(dailyTasks, weeklyList)

        } catch (e: Exception) {
            Log.e("TaskRepository", "Error fetching tasks for $userId", e)
            Pair(emptyList(), emptyList())
        }
    }

}