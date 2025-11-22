package com.taskraze.myapplication.model.calendar

import android.content.ContentValues
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

class FirestoreCalendarRepository {

    private val firestoreDB = FirebaseFirestore.getInstance()
    private val calendarsCollection = firestoreDB.collection("calendars")
    private val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")

    // Add a new calendar
    suspend fun addCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        calendars.add(calendar)
        updateCalendars(calendars)
    }

    // Update an existing calendar
    suspend fun updateCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        val index = calendars.indexOfFirst { it.id == calendar.id }
        if (index != -1) {
            calendars[index] = calendar
            updateCalendars(calendars)
        }
    }

    // Remove a calendar
    suspend fun removeCalendar(calendarId: Long) {
        val calendars = getOwnCalendars().toMutableList()
        calendars.removeIf { it.id == calendarId }
        updateCalendars(calendars)
    }

    // Helper function to update Firestore
    private suspend fun updateCalendars(calendars: List<CalendarData>) {
        try {
            val userCalendars = UserCalendarsData(
                userId = AuthViewModel.getUserId(),
                calendars = calendars.toMutableList()
            )

            calendarsCollection.document(AuthViewModel.getUserId())
                .set(userCalendars) // use set() instead of update()
                .await()

            Log.d("FirestoreRepo", "Updated calendars for ${AuthViewModel.getUserId()}")
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error updating calendars: $e")
        }
    }

    // Get all calendars for the current user
    suspend fun getOwnCalendars(): List<CalendarData> {
        return try {
            val doc = calendarsCollection.document(AuthViewModel.getUserId()).get().await()
            if (doc.exists()) {
                doc.toObject(UserCalendarsData::class.java)?.calendars ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error fetching calendars: $e")
            emptyList()
        }
    }

    suspend fun getSharedCalendars(): List<CalendarData> {
        val currentUserId = AuthViewModel.getUserId()
        val sharedCalendars = mutableListOf<CalendarData>()

        try {
            val snapshot = userInCalendarsCollection.document(currentUserId).get().await()
            val owners = snapshot.get("owners") as? List<Map<String, Any>> ?: emptyList()

            for (ownerEntry in owners) {
                val ownerId = ownerEntry["userId"] as? String ?: continue
                val calendarId = (ownerEntry["calendarId"] as? Number)?.toLong() ?: continue

                val ownerDoc = calendarsCollection.document(ownerId).get().await()
                val ownerCalendars = ownerDoc.toObject(UserCalendarsData::class.java)?.calendars ?: emptyList()

                ownerCalendars.firstOrNull { it.id == calendarId }?.let {
                    sharedCalendars.add(it)
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreCalendarRepo", "Error fetching shared calendars: $e")
        }

        return sharedCalendars
    }

    // Add a shared user to a calendar
    suspend fun addSharedUserToCalendar(sharedUser: UserData, calendarId: Long) {
        try {
            val doc = userInCalendarsCollection.document(sharedUser.email).get().await()
            val owners = (doc.get("owners") as? MutableList<Map<String, Any>>) ?: mutableListOf()
            owners.add(mapOf("userId" to AuthViewModel.getUserId(), "calendarId" to calendarId))
            userInCalendarsCollection.document(sharedUser.email).set(mapOf("owners" to owners)).await()
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error adding shared user: $e")
        }
    }

    // Remove a shared user from a calendar
    suspend fun removeSharedUserFromCalendar(sharedUserId: String, calendarId: Long) {
        try {
            val doc = userInCalendarsCollection.document(sharedUserId).get().await()
            if (doc.exists()) {
                val owners = (doc.get("owners") as? MutableList<Map<String, Any>>) ?: mutableListOf()
                owners.removeIf { it["calendarId"] == calendarId }
                if (owners.isNotEmpty()) {
                    userInCalendarsCollection.document(sharedUserId).update("owners", owners).await()
                } else {
                    userInCalendarsCollection.document(sharedUserId).delete().await()
                }
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error removing shared user: $e")
        }
    }

    // Get shared users for a specific calendar
    suspend fun getSharedUsersForCalendar(calendarId: Long): List<String> {
        val sharedUsers = mutableListOf<String>()
        try {
            val snapshot = userInCalendarsCollection.get().await()
            for (doc in snapshot.documents) {
                val owners = doc.get("owners") as? List<Map<String, Any>> ?: continue
                if (owners.any { it["calendarId"] == calendarId }) {
                    sharedUsers.add(doc.id) // email of shared user
                }
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error fetching shared users: $e")
        }
        return sharedUsers
    }


}