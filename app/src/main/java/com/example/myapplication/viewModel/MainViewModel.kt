package com.example.myapplication.viewModel

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.example.myapplication.authentication.User
import com.example.myapplication.calendar.Event
import com.example.myapplication.calendar.MyCalendar
import com.example.myapplication.calendar.UserFirestoreData
import com.example.myapplication.local_database_room.AppDatabase
import com.example.myapplication.local_database_room.CalendarData
import com.example.myapplication.local_database_room.EventData
import com.example.myapplication.local_database_room.UserData
import com.example.myapplication.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

class MainViewModel : ViewModel() {
    private val _weeklyTasksList: List<MutableList<Task>> = List(7) { mutableListOf() }
    private val firestoreDB = FirebaseFirestore.getInstance()
    private val calendarsCollection = firestoreDB.collection("calendars")
    private val _someEvent = MutableLiveData<Task>()
    private var _taskReady = false
    private val _dayId = MutableLiveData<Int>()
    var taskId: Int = -1
    private var _isNewTask = false
    lateinit var taskStorage: Task
    var auth = Firebase.auth
    var loggedInUser: User? = null
    val loggedInDeferred = CompletableDeferred<User?>()
    private var _myCalendarToPass: MyCalendar? = null
    private var _isExistingEvent: Boolean = false
    var newEventStartingDay: Calendar? = null
    init {

    }
    val isExistingEvent
        get() = _isExistingEvent
    val dayId
        get() = _dayId
    val taskReady
        get() = _taskReady
    val someEvent
        get() = _someEvent
    val isNewTask
        get() = _isNewTask
    val weeklyTasksList
        get() = _weeklyTasksList
    init {
        for (i in 0 until 7) {
            val task1 = Task(0, "Munka", "leírás", "16:02", false)
            val task2 = Task(1, "Edzés", "leírás", "18:02", false)
            val task3 = Task(2, "Séta", "leírás", "20:02", false)
            _weeklyTasksList[i].apply {
                add(task1)
                add(task2)
                add(task3)
            }
        }
    }
    fun toggleExistingEvent() {
        _isExistingEvent = !_isExistingEvent
    }

    fun updateEvent(eventData: Task) {
        _someEvent.value = eventData
    }

    fun toggleNewTask() {
        _isNewTask = !_isNewTask
    }

    fun setNewTaskFalse() {
        _isNewTask = false
    }

    fun toggleTaskReady() {
        _taskReady = !_taskReady
    }

    fun authenticateUser() {
        val docRef = firestoreDB.collection("registered_users").document(Firebase.auth.currentUser?.email.toString())
        docRef.get().addOnSuccessListener { documentSnapshot ->
            val username = documentSnapshot.getString("username") ?: ""
            val emailAddress = documentSnapshot.getString("email") ?: ""
            loggedInDeferred.complete(User(username, emailAddress))
            Log.d("authenticateUser", "successfully fetched user data ")
        }.addOnFailureListener { exception ->
            // Log the error
            Log.e("authenticateUser", "Error fetching user data: ", exception)
            loggedInDeferred.complete(null)
        }
    }

    suspend fun getAllCalendars(context: Context): MutableList<MyCalendar> {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val eventDao = roomDB.eventItemDao()

        authenticateUser()

        loggedInUser = loggedInDeferred.await()

        val loggedInUserJson = Gson().toJson(UserData(null, loggedInUser!!.username, loggedInUser!!.email)).toString()

        val myCalendarList = mutableListOf<MyCalendar>()

        val calendarDataList = calendarDao.getAllCalendarsForUser(loggedInUserJson, loggedInUser!!.email)

        if (calendarDataList.isNotEmpty()) {

            for (calendarData in calendarDataList) {
                val sharedPeopleData = calendarDao.getSharedPeopleForCalendar(calendarData.id.toString())
                val eventDataForCalendar = calendarDao.getEventsForCalendar(calendarData.name)

                val sharedPeopleList = mutableListOf<User>()
                for (userData in sharedPeopleData) {
                    sharedPeopleList.add(User(userData.username, userData.emailAddress))
                }

                val eventList = mutableListOf<Event>()
                for (eventData in eventDataForCalendar) {
                    eventList.add(Event(
                        eventData.title,
                        eventData.description,
                        eventData.startTime,
                        eventData.endTime,
                        eventData.location,
                        eventData.wholeDayEvent)
                    )
                }

                myCalendarList.add(
                    MyCalendar(
                        name = calendarData.name,
                        sharedPeopleNumber = calendarData.sharedPeopleNumber,
                        sharedPeople = sharedPeopleList,
                        owner = User(calendarData.owner.username, calendarData.owner.emailAddress),
                        events = eventList,
                        lastUpdated = calendarData.lastUpdated
                    )
                )
            }
        }


        return myCalendarList
    }
    suspend fun addCalendar(context: Context, myCalendar: MyCalendar) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val sharedPeopleDao = roomDB.sharedUsersDao()
        val eventDao = roomDB.eventItemDao()

        val existingCalendar = calendarDao.getCalendarByName(myCalendar.name)

        if (existingCalendar == null) {
            for (user in myCalendar.sharedPeople) {
                val userData = UserData(myCalendar.name, user.username, user.email)
                sharedPeopleDao.insertUser(userData)
            }

            val eventList = myCalendar.events.map { event ->
                EventData(
                    calendarId = myCalendar.name,
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    location = event.location,
                    wholeDayEvent = event.wholeDayEvent
                )
            }.toList()

            for (event in eventList) {
                eventDao.insertEvent(event)
            }

            val newCalendar = CalendarData(
                name = myCalendar.name,
                sharedPeopleNumber = myCalendar.sharedPeopleNumber,
                owner = UserData(null, myCalendar.owner.username, myCalendar.owner.email),
                lastUpdated = myCalendar.lastUpdated
            )
            calendarDao.insertCalendarItem(newCalendar)
        } else {
            // Update existing calendar
            existingCalendar.apply {
                sharedPeopleNumber = myCalendar.sharedPeopleNumber
                lastUpdated = myCalendar.lastUpdated
            }
            calendarDao.updateCalendarItem(existingCalendar)
        }
        saveAllCalendarsToFirestoreDB(context, loggedInUser!!.email)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEventToCalendar(context: Context, event: Event, myCalendar: MyCalendar) {
        // maybe update events here as well
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val eventDao = roomDB.eventItemDao()

        val newEventData = EventData(
            myCalendar.name,
            event.title,
            event.description,
            event.startTime,
            event.endTime,
            event.location,
            event.wholeDayEvent
        )

        eventDao.insertEvent(newEventData)
        val enetList = eventDao.getEventByCalendarName(myCalendar.name)?.toMutableList()?.map { eventData ->
            Event(
                eventData.title,
                eventData.description,
                eventData.startTime,
                eventData.endTime,
                eventData.location,
                eventData.wholeDayEvent
            )
        }?.toMutableList()
        if (enetList != null) {
            myCalendar.events.clear()
            myCalendar.events.addAll(enetList)
        }

        myCalendar.lastUpdated = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
        saveAllCalendarsToFirestoreDB(context, loggedInUser!!.email)
    }

    suspend fun saveAllCalendarsToFirestoreDB(context: Context, userId: String) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val calendars = getAllCalendars(context)
            loggedInUser = loggedInDeferred.await()
            val loggedInUserEmail = loggedInUser?.email

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

            Log.d(TAG, "Calendars saved to Firestore for user: $loggedInUserEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendars to Firestore: $e")
        }
    }

    suspend fun getAllCalendarsFromFirestoreDB(context: Context) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")

        loggedInUser = loggedInDeferred.await()
        val userId = loggedInUser!!.email
        val allData = try {
            val userDocument = calendarsCollection.document(userId).get().await()

            if (userDocument.exists()) {
                val calendars = userDocument.toObject(UserFirestoreData::class.java)?.calendars
                Log.d(TAG, "Calendars retrieved from Firestore for user: $userId")
                calendars
            } else {
                Log.d(TAG, "No calendars found in Firestore for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving calendars from Firestore: $e")
            null
        }

        if (allData != null) {
            val previousCalendars = getAllCalendars(context)

            val roomDB = Room.databaseBuilder(
                context,
                AppDatabase::class.java, "database-name"
            ).build()

            val calendarsDao = roomDB.calendarItemDao()
            for (tempCalendar in previousCalendars) {
                if (!allData.contains(tempCalendar)) {
                    val existingCalendar = calendarsDao.getCalendarByName(tempCalendar.name)
                    if (existingCalendar != null) {
                        calendarsDao.deleteCalendarItem(existingCalendar)
                    }
                }
            }
            for (cal in allData) {
                addCalendar(context, cal)
            }

        }


    }

    suspend fun deleteCalendarFromRoom(context: Context, myCalendar: MyCalendar) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val sharedPeopleDao = roomDB.sharedUsersDao()

        for (user in myCalendar.sharedPeople) {
            val userData = sharedPeopleDao.getUserByEmail(user.email)
            if (userData != null) {
                sharedPeopleDao.deleteUser(userData)
            }
        }

        val newCalendar = calendarDao.getCalendarByName(myCalendar.name)
        if (newCalendar != null) {
            calendarDao.deleteCalendarItem(newCalendar)
        }
    }

    fun passCalendarToFragment(myCalendar: MyCalendar) {
        _myCalendarToPass = myCalendar
    }

    fun getCalendarToFragment(): MyCalendar? {
        val temp = _myCalendarToPass
        _myCalendarToPass = null
        if (temp != null) {
            return temp
        }
        else {
            return null
        }
    }

    suspend fun deleteEventFromRoom(context: Context, event: Event, calendarId: String) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val eventDao = roomDB.eventItemDao()

        val eventData = eventDao.getSpecificEvent(calendarId, event.title, event.startTime, event.endTime)
        if (eventData != null) {
            eventDao.deleteEvent(eventData)
        }
    }

    suspend fun getFriendRequests(callback: (List<FriendRequest>) -> Unit) {

        loggedInUser = loggedInDeferred.await()
        if (loggedInUser != null) {
            firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", loggedInUser!!.email)
                .get()
                .addOnSuccessListener { result ->
                    val friendRequests = mutableListOf<FriendRequest>()
                    for (document in result) {
                        val receiverId = document.getString("receiverId") ?: ""
                        val senderId = document.getString("senderId") ?: ""
                        val status = document.getString("status") ?: ""
                        val friendRequest = FriendRequest(receiverId, senderId, status)
                        if(status == "pending") {
                            friendRequests.add(friendRequest)
                        }
                    }
                    callback(friendRequests)
                }
                .addOnFailureListener { _ ->
                    Log.d(TAG, "failed to retrieve friend requests")
                }
        }
    }

    fun handleFriendRequest(choice: String, friendRequest: FriendRequest, callback: (Boolean) -> Unit) {
        val friendRequestsCollection = firestoreDB.collection("friend_requests")


        friendRequestsCollection
            .whereEqualTo("senderId", friendRequest.senderId)
            .whereEqualTo("receiverId", auth.currentUser?.email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    document.reference.update("status", choice)
                        .addOnSuccessListener {
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    suspend fun getFriends(callback: (List<User>) -> Unit) {

        loggedInUser = loggedInDeferred.await()
        if (loggedInUser != null) {
            firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", loggedInUser!!.email)
                .get()
                .addOnSuccessListener { result ->
                    val acceptedFriends = mutableListOf<FriendRequest>()
                    val rejectedFriends = mutableListOf<FriendRequest>()
                    for (document in result) {
                        val receiverId = document.getString("receiverId") ?: ""
                        val senderId = document.getString("senderId") ?: ""
                        val status = document.getString("status") ?: ""
                        val friendRequest = FriendRequest(receiverId, senderId, status)
                        if(status == "accepted") {
                            acceptedFriends.add(friendRequest)
                        }
                        else if (status == "rejected") {
                            rejectedFriends.add(friendRequest)
                        }
                    }
                    CoroutineScope(Dispatchers.Default).launch {
                        updateOrCreateUserFriendsDocument(acceptedFriends).await()
                        deleteRejectedFriendRequests(rejectedFriends)
                        deleteAcceptedFriendRequests(acceptedFriends)

                        fetchUsersFromFriendsList(callback)
                    }


                }
                .addOnFailureListener { _ ->
                    Log.d(TAG, "failed to retrieve friend requests")
                }
        }
    }

    suspend fun fetchUsersFromFriendsList(callback: (List<User>) -> Unit) {
        authenticateUser()
        loggedInUser = loggedInDeferred.await()
        firestoreDB.collection("user_friends")
            .document(loggedInUser!!.email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val friendsList = documentSnapshot.toObject(UserFriends::class.java)?.friends ?: emptyList()


                val users = mutableListOf<User>()
                val userRef = firestoreDB.collection("registered_users")


                val queries = friendsList.map { friendId ->
                    userRef.document(friendId).get()
                }

                Tasks.whenAllSuccess<DocumentSnapshot>(queries)
                    .addOnSuccessListener { snapshots ->
                        snapshots.forEach { snapshot ->
                            val user = snapshot.toObject(User::class.java)
                            user?.let {
                                users.add(user)
                            }
                        }
                        callback(users)
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error fetching user data for friends", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error fetching user friends document", exception)
            }
    }

    private fun updateOrCreateUserFriendsDocument(acceptedFriends: List<FriendRequest>): Deferred<Unit>  {
        val currentEmail = loggedInUser!!.email
        val deferred = CompletableDeferred<Unit>()

        // Update or create the document in Firestore
        firestoreDB.collection("user_friends")
            .document(currentEmail)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val currentUserFriends = documentSnapshot.toObject(UserFriends::class.java)
                val existingFriends = currentUserFriends?.friends ?: emptyList()

                // Merge existing friends with new friends
                val updatedFriends = existingFriends.toMutableList()
                acceptedFriends.forEach { friendRequest ->
                    if (!existingFriends.contains(friendRequest.senderId)) {
                        updatedFriends.add(friendRequest.senderId)
                    }
                }

                // Update the document with merged friends list
                val userFriendsMap = mapOf("userId" to currentEmail, "friends" to updatedFriends)
                firestoreDB.collection("user_friends")
                    .document(currentEmail)
                    .set(userFriendsMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "User friends document updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating user friends document", e)
                    }

                for (friendRequest in acceptedFriends) {
                    val friendEmail = friendRequest.senderId
                    firestoreDB.collection("user_friends")
                        .document(friendEmail)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            val friendFriends = documentSnapshot.toObject(UserFriends::class.java)
                            val updatedFriends = friendFriends?.friends?.toMutableList() ?: mutableListOf()
                            if (!updatedFriends.contains(loggedInUser!!.email)) {
                                updatedFriends.add(loggedInUser!!.email)
                                val data = mapOf("friends" to updatedFriends)
                                firestoreDB.collection("user_friends")
                                    .document(friendEmail)
                                    .set(data, SetOptions.merge())
                                    .addOnSuccessListener {
                                        Log.d(TAG, "User $friendEmail friends document updated successfully.")
                                        deferred.complete(Unit)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "Error updating user $friendEmail friends document", e)
                                        deferred.completeExceptionally(e)
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error getting user $friendEmail friends document", e)
                            deferred.completeExceptionally(e)
                        }
                }
                deferred.complete(Unit)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error fetching user friends document", e)
                deferred.completeExceptionally(e)
            }
        return deferred
    }

    private fun deleteRejectedFriendRequests(rejectedFriends: List<FriendRequest>) {
        val batch = firestoreDB.batch()

        // Iterate through the rejected friend requests and delete them
        rejectedFriends.forEach { friendRequest ->
            val query = firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", friendRequest.receiverId)
                .whereEqualTo("senderId", friendRequest.senderId)
                .whereEqualTo("status", friendRequest.status)

            query.get()
                .addOnSuccessListener { snapshot ->
                    snapshot.forEach { document ->
                        batch.delete(document.reference)
                    }

                    // Commit the batch delete operation
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Rejected friend requests deleted successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error deleting rejected friend requests", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting rejected friend requests to delete", e)
                }
        }
    }

    private fun deleteAcceptedFriendRequests(acceptedFriends: List<FriendRequest>) {
        val batch = firestoreDB.batch()

        // Iterate through the rejected friend requests and delete them
        acceptedFriends.forEach { friendRequest ->
            val query = firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", friendRequest.receiverId)
                .whereEqualTo("senderId", friendRequest.senderId)
                .whereEqualTo("status", friendRequest.status)

            query.get()
                .addOnSuccessListener { snapshot ->
                    snapshot.forEach { document ->
                        batch.delete(document.reference)
                    }

                    // Commit the batch delete operation
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Accepted friend requests deleted successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error deleting accepted friend requests", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting accepted friend requests to delete", e)
                }
        }
    }


}