package com.taskraze.myapplication.model.calendar

import android.content.ContentValues
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

class CalendarRepository {
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val calendarsCollection = firestoreDB.collection("calendars")
    private val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")

    suspend fun addCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        calendars.add(calendar)
        updateCalendars(calendars)
    }

    suspend fun updateCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        val index = calendars.indexOfFirst { it.id == calendar.id }
        if (index != -1) {
            calendars[index] = calendar
            updateCalendars(calendars)
        }
    }

    suspend fun removeCalendar(calendarId: Long) {
        val calendars = getOwnCalendars().toMutableList()
        calendars.removeIf { it.id == calendarId }
        updateCalendars(calendars)
    }

    private suspend fun updateCalendars(calendars: List<CalendarData>) {
        val userId = AuthViewModel.awaitUserId()
        try {
            val userCalendars = UserCalendarsData(
                userId = userId,
                calendars = calendars.toMutableList()
            )

            calendarsCollection.document(userId)
                .set(userCalendars)
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreRepo", "Error updating calendars: $e")
        }
    }

    suspend fun getOwnCalendars(): List<CalendarData> {
        val userId = AuthViewModel.awaitUserId()
        return try {
            val doc = calendarsCollection.document(userId).get().await()

            if (doc.exists()) {
                val calendars = doc.toObject(UserCalendarsData::class.java)?.calendars ?: emptyList()
                calendars
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NotificationMINE", "Error fetching calendars for user $userId", e)
            emptyList()
        }
    }

    suspend fun getSharedCalendars(): List<CalendarData> {
        val userId = AuthViewModel.awaitUserId()
        val sharedCalendars = mutableListOf<CalendarData>()

        try {
            val snapshot = userInCalendarsCollection.document(userId).get().await()
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
}