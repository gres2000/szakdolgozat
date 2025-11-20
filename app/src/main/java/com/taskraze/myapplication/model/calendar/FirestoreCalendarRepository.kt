package com.taskraze.myapplication.model.calendar

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.model.room_database.data_classes.UserData
import com.taskraze.myapplication.model.room_database.db.AppDatabase
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

class FirestoreCalendarRepository(private val localCalendarRepository: LocalCalendarRepository) {
    private val authRepository = AuthRepository()

    suspend fun saveAllCalendarsToFirestoreDB(context: Context, userId: String) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val calendars = localCalendarRepository.getAllCalendarsLocal(context)
            val loggedInUserEmail = AuthViewModel.loggedInUser?.email

            if (loggedInUserEmail != null) {

                val userDocument =
                    calendarsCollection.document(loggedInUserEmail).get().await()

                if (userDocument.exists()) {
                    userDocument.reference.update("calendars", calendars).await()
                } else {
                    val userData = hashMapOf(
                        "userId" to userId,
                        "email" to loggedInUserEmail,
                        "calendars" to calendars
                    )
                    calendarsCollection.document(loggedInUserEmail).set(userData)
                        .await()
                }
            }

            Log.d(ContentValues.TAG, "Calendars saved to Firestore for user: $AuthViewModel.loggedInUserEmail")
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error saving calendars to Firestore: $e")
        }
    }

    suspend fun removeUserFromFirestoreSharedCalendar(myUser: User, calendarData: CalendarData) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")
        try {

            val existingDoc =
                userInCalendarsCollection.document(myUser.email).get().await()

            if (existingDoc.exists()) {
                val ownerList = existingDoc.get("owners") as? List<*>
                val userData = hashMapOf(
                    "userId" to calendarData.owner.email,
                    "calendarId" to calendarData.id
                )

                val mutableOwnerList = ownerList!!.toMutableList()

                mutableOwnerList.remove(userData)

                if (mutableOwnerList.isNotEmpty()) {
                    userInCalendarsCollection.document(myUser.email)
                        .update("owners", mutableOwnerList)
                        .await()
                } else {
                    userInCalendarsCollection.document(myUser.email)
                        .delete()
                }
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error removing user from realtime database: $e")
        }
    }

    suspend fun saveSharedUserToFirestoreDB(myUser: User, owner: User, id: Long) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")
        try {

            val existingDoc = userInCalendarsCollection.document(myUser.email).get().await()


            if (existingDoc.exists()) {
                val ownerList = existingDoc.get("owners") as? List<*>
                val userData = hashMapOf(
                    "userId" to owner.email,
                    "calendarId" to id
                )

                val mutableOwnerList = ownerList!!.toMutableList()

                mutableOwnerList.add(userData)

                userInCalendarsCollection.document(myUser.email)
                    .update("owners", mutableOwnerList)
                    .await()
            } else {
                val userData = listOf(
                    hashMapOf(
                        "userId" to owner.email,
                        "calendarId" to id
                    )
                )

                val list = hashMapOf(
                    "owners" to userData
                )
                userInCalendarsCollection.document(myUser.email)
                userInCalendarsCollection.document(myUser.email)
                    .set(list)
                    .await()
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error saving calendars to Firestore: $e")
        }
    }

    suspend fun getAllCalendarsFromFirestoreDB(context: Context) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")

//        AuthViewModel.loggedInUser = loggedInDeferred.await()
        val userId = AuthViewModel.loggedInUser.email
        val allData = try {
            val userDocument = calendarsCollection.document(userId).get().await()

            if (userDocument.exists()) {
                val calendars = userDocument.toObject(UserFirestoreData::class.java)?.calendars
                Log.d(ContentValues.TAG, "Calendars retrieved from Firestore for user: $userId")
                calendars
            } else {
                Log.d(ContentValues.TAG, "No calendars found in Firestore for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error retrieving calendars from Firestore: $e")
            null
        }

        if (allData != null) {
            val previousCalendars = localCalendarRepository.getAllCalendarsLocal(context)

            val roomDB = Room.databaseBuilder(
                context,
                AppDatabase::class.java, "database-name"
            ).build()

            //delete previous calendars from room
            val calendarsDao = roomDB.calendarItemDao()
            for (tempCalendar in previousCalendars) {
                if (!allData.contains(tempCalendar)) {
                    val existingCalendar = calendarsDao.getCalendarById(tempCalendar.id)
                    if (existingCalendar != null) {
                        calendarsDao.deleteCalendarItem(existingCalendar)
                    }
                }
            }

            //delete previous users from room
            val sharedUsersDao = roomDB.sharedUsersDao()
            val users = sharedUsersDao.getAllUsers()
            val convertedUsers = users.map { userData ->
                User(
                    userData.username,
                    userData.emailAddress
                )
            }
            Log.d("USERS", users.toString())
            for (calendar in allData) {
                for (user in convertedUsers) {
                    if (!calendar.sharedPeople.contains(user)) {
                        val userData = UserData(calendar.id, user.username, user.email)
                        sharedUsersDao.deleteUser(userData)
                    }
                }
            }

            for (cal in allData) {
                localCalendarRepository.addOrUpdateCalendarLocal(context, cal)
            }
        }
    }
}