package com.taskraze.myapplication.model.calendar

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

class CalendarRepository(private val authViewModel: AuthViewModel) {
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val calendarsCollection = firestoreDB.collection("calendars")
    private val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")
    private val cachedUser get() = authViewModel.getCachedUser()
    private val userId get() = cachedUser?.userId.orEmpty()

    suspend fun addCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        calendars.add(calendar)
        updateCalendars(calendars)
    }

    suspend fun updateOwnCalendar(calendar: CalendarData) {
        val calendars = getOwnCalendars().toMutableList()
        val index = calendars.indexOfFirst { it.id == calendar.id }
        if (index != -1) {
            calendars[index] = calendar
            updateCalendars(calendars)
        }
    }

    suspend fun updateSharedCalendar(calendar: CalendarData) {
        try {
            val snapshot = userInCalendarsCollection.document(userId).get().await()
            val owners = snapshot.get("owners") as? List<Map<String, Any>> ?: return
            val ownerEntry = owners.firstOrNull { (it["calendarId"] as? Number)?.toLong() == calendar.id } ?: return
            val ownerId = ownerEntry["userId"] as? String ?: return

            val ownerDoc = calendarsCollection.document(ownerId).get().await()
            val ownerCalendars = ownerDoc.toObject(UserCalendarsData::class.java)?.calendars?.toMutableList() ?: mutableListOf()

            val index = ownerCalendars.indexOfFirst { it.id == calendar.id }
            if (index != -1) {
                ownerCalendars[index] = calendar
                calendarsCollection.document(ownerId).update("calendars", ownerCalendars).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating shared calendar: $e")
        }
    }


    suspend fun removeCalendar(calendarId: Long) {
        val calendars = getOwnCalendars().toMutableList()
        val removed = calendars.removeIf { it.id == calendarId }

        Log.d("CalendarRepo", "Current calendars before removal: $calendars")
        if (removed) {

            updateCalendars(calendars)
        }
    }

    private suspend fun updateCalendars(calendars: List<CalendarData>) {
        Log.d("CalendarRepo", "Current calendars before removal: $calendars")
        try {
            val userCalendars = UserCalendarsData(
                userId = userId,
                calendars = calendars.toMutableList()
            )

            calendarsCollection.document(userId)
                .set(userCalendars)
                .await()
        } catch (e: Exception) {
            Log.e("CalendarRepo", "Error updating calendars: $e")
        }
    }

    suspend fun getOwnCalendars(): List<CalendarData> {
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

    suspend fun addSharedUserToCalendar(sharedUser: UserData, calendarId: Long, ownerId: String) {
        Log.d("VALAMINEMJO", "Adding shared user: $sharedUser to calendarId: $calendarId owned by: $ownerId")
        try {
            val userDocRef = userInCalendarsCollection.document(sharedUser.email)
            val existingDoc = userDocRef.get().await()
            val owners = (existingDoc.get("owners") as? MutableList<Map<String, Any>>) ?: mutableListOf()
            owners.add(mapOf("userId" to ownerId, "calendarId" to calendarId))
            userDocRef.set(mapOf("owners" to owners)).await()

            val ownerDocRef = calendarsCollection.document(ownerId)
            val ownerCalendars = ownerDocRef.get().await()
                .toObject(UserCalendarsData::class.java)?.calendars?.toMutableList() ?: mutableListOf()
            Log.d("VALAMINEMJO", "Owner calendars before adding shared user: $ownerCalendars")
            val targetCalendar = ownerCalendars.firstOrNull { it.id == calendarId }
            if (targetCalendar != null) {

                // avoid duplicates
                val userIdentifier = sharedUser.userId.ifBlank { sharedUser.email }
                if (targetCalendar.sharedPeople.none { it.userId == userIdentifier }) {
                    targetCalendar.sharedPeople.add(sharedUser)
                    targetCalendar.sharedPeopleNumber++
                    ownerDocRef.update("calendars", ownerCalendars).await()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error adding shared user: $e")
        }
    }

    suspend fun removeSharedUserFromCalendar(sharedUserId: String, calendarId: Long, ownerId: String) {
        try {
            val userDocRef = userInCalendarsCollection.document(sharedUserId)
            val existingDoc = userDocRef.get().await()
            if (existingDoc.exists()) {
                val owners = (existingDoc.get("owners") as? MutableList<Map<String, Any>>) ?: mutableListOf()
                owners.removeIf { (it["calendarId"] as? Number)?.toLong() == calendarId }
                if (owners.isNotEmpty()) {
                    userDocRef.update("owners", owners).await()
                } else {
                    userDocRef.delete().await()
                }
            }

            val ownerDocRef = calendarsCollection.document(ownerId)
            val ownerCalendars = ownerDocRef.get().await()
                .toObject(UserCalendarsData::class.java)?.calendars?.toMutableList() ?: mutableListOf()

            val targetCalendar = ownerCalendars.firstOrNull { it.id == calendarId }

            targetCalendar?.let {
                val removed = it.sharedPeople.removeIf { user -> user.userId == sharedUserId || user.email == sharedUserId }
                if (removed) {
                    ownerDocRef.update("calendars", ownerCalendars).await()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error removing shared user: $e")
        }
    }


    suspend fun addEventToCalendar(event: EventData, calendarId: Long) {
        try {
            val existingDoc =
                calendarsCollection.document(userId).get().await()

            val calendarList = existingDoc
                .toObject(UserCalendarsData::class.java)
                ?.calendars ?: return

            val target = calendarList.firstOrNull { it.id == calendarId } ?: return

            target.events.add(event)

            calendarsCollection
                .document(userId)
                .update("calendars", calendarList)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error adding event to shared calendar: $e")
        }
    }

    suspend fun addEventToSharedCalendar(event: EventData, calendarId: Long) {
        try {
            val doc = userInCalendarsCollection.document(userId).get().await()
            val owners = doc.get("owners") as? List<Map<String, Any>> ?: return
            val ownerId = owners.firstOrNull { it["calendarId"] == calendarId }?.get("userId") as? String ?: return

            val ownerDoc = calendarsCollection.document(ownerId).get().await()
            val calendarList = ownerDoc.toObject(UserCalendarsData::class.java)?.calendars ?: return

            val target = calendarList.firstOrNull { it.id == calendarId } ?: return
            target.events.add(event)

            calendarsCollection.document(ownerId)
                .update("calendars", calendarList)
                .await()

        } catch (e: Exception) {
            Log.e(TAG, "Error adding event to shared calendar: $e")
        }
    }

    suspend fun deleteSharedUsersFromCalendar(sharedUsers: MutableList<UserData>, calendarId: Long) {
        try {

            for (user in sharedUsers) {
                val existingDoc = userInCalendarsCollection.document(user.email).get().await()

                if (existingDoc.exists()) {
                    val ownerList = existingDoc.get("owners") as? List<*>
                    val userData = hashMapOf(
                        "userId" to userId,
                        "calendarId" to calendarId
                    )

                    val mutableOwnerList = ownerList!!.toMutableList()

                    mutableOwnerList.remove(userData)

                    if (mutableOwnerList.isNotEmpty()) {
                        userInCalendarsCollection.document(user.email)
                            .update("owners", mutableOwnerList)
                            .await()
                    } else {
                        userInCalendarsCollection.document(user.email)
                            .delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendars to Firestore: $e")
        }
    }
}