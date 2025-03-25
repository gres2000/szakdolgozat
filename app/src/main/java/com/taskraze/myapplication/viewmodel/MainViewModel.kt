package com.taskraze.myapplication.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.UserFirestoreData
import com.taskraze.myapplication.model.chat.ChatData
import com.taskraze.myapplication.model.chat.FriendlyMessage
import com.taskraze.myapplication.model.room_database.db.AppDatabase
import com.taskraze.myapplication.model.room_database.data_classes.CalendarData
import com.taskraze.myapplication.model.room_database.data_classes.EventData
import com.taskraze.myapplication.model.room_database.data_classes.UserData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.taskraze.myapplication.model.friends.FriendRequestData
import com.taskraze.myapplication.model.friends.UserFriendsData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

object MainViewModel : ViewModel() {
    private val firestoreDB = FirebaseFirestore.getInstance()
    var auth = Firebase.auth
    lateinit var loggedInUser: User
    private var _CalendarDataToPass: com.taskraze.myapplication.model.calendar.CalendarData? = null
    var newEventStartingDay: Calendar? = null

    init {
        viewModelScope.launch {

            authenticateUser()
        }
    }

    suspend fun authenticateUser() {
        withContext(Dispatchers.Main) {
            val docRef = firestoreDB.collection("registered_users")
                .document(Firebase.auth.currentUser?.email.toString())
            docRef.get().addOnSuccessListener { documentSnapshot ->
                val username = documentSnapshot.getString("username") ?: ""
                val emailAddress = documentSnapshot.getString("email") ?: ""
                loggedInUser = User(username, emailAddress)
            }.addOnFailureListener { _ ->
                loggedInUser = User("empty", "empty")
            }
        }
    }

    suspend fun getAllCalendars(context: Context): MutableList<com.taskraze.myapplication.model.calendar.CalendarData> {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val eventDao = roomDB.eventItemDao()
        val sharedUsersDao = roomDB.sharedUsersDao()


        authenticateUser()
        val loggedInUserJson =
            Gson().toJson(UserData(0, loggedInUser.username, loggedInUser.email)).toString()

        val myCalendarList = mutableListOf<com.taskraze.myapplication.model.calendar.CalendarData>()

        val calendarDataList =
            calendarDao.getAllCalendarsForUser(loggedInUserJson)


        if (calendarDataList.isNotEmpty()) {

            for (calendarData in calendarDataList) {
                val sharedPeopleData =
                    calendarDao.getSharedPeopleForCalendar(calendarData.id.toString())
                val eventDataForCalendar = calendarDao.getEventsForCalendar(calendarData.id)

                val sharedPeopleList = mutableListOf<User>()
                for (userData in sharedPeopleData) {
                    sharedPeopleList.add(User(userData.username, userData.emailAddress))
                }

                val eventList = mutableListOf<com.taskraze.myapplication.model.calendar.EventData>()
                for (eventData in eventDataForCalendar) {
                    eventList.add(
                        com.taskraze.myapplication.model.calendar.EventData(
                            eventData.title,
                            eventData.description,
                            eventData.startTime,
                            eventData.endTime,
                            eventData.location,
                            eventData.wholeDayEvent
                        )
                    )
                }

                myCalendarList.add(
                    com.taskraze.myapplication.model.calendar.CalendarData(
                        id = calendarData.id,
                        name = calendarData.name,
                        sharedPeopleNumber = sharedPeopleList.size,
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

    suspend fun addCalendar(context: Context, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val sharedPeopleDao = roomDB.sharedUsersDao()
        val eventDao = roomDB.eventItemDao()

        val existingCalendar = calendarDao.getCalendarById(calendarData.id)
        val existingUsers = sharedPeopleDao.getAllUsers()

        if (existingCalendar == null) {
            for (user in calendarData.sharedPeople) {
                val userData = UserData(calendarData.id, user.username, user.email)
                sharedPeopleDao.insertUser(userData)
            }

            val eventList = calendarData.events.map { event ->
                EventData(
                    calendarId = calendarData.id,
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

            var newCalendar: CalendarData
            val longValue: Long = -1
            if (calendarData.id == longValue) {
                newCalendar = CalendarData(
                    name = calendarData.name,
                    sharedPeopleNumber = calendarData.sharedPeopleNumber,
                    owner = UserData(0, calendarData.owner.username, calendarData.owner.email),
                    lastUpdated = calendarData.lastUpdated
                )
            } else {
                newCalendar = CalendarData(
                    calendarData.id,
                    name = calendarData.name,
                    sharedPeopleNumber = calendarData.sharedPeopleNumber,
                    owner = UserData(0, calendarData.owner.username, calendarData.owner.email),
                    lastUpdated = calendarData.lastUpdated
                )
            }


            calendarDao.insertCalendarItem(newCalendar)
        } else {
            // Update existing calendar
            existingCalendar.apply {
                sharedPeopleNumber = calendarData.sharedPeopleNumber
                lastUpdated = calendarData.lastUpdated
            }
            calendarDao.updateCalendarItem(existingCalendar)

            val previousEventDataList = eventDao.getEventByCalendarId(calendarData.id)
            val newEventDataList = calendarData.events.map { event ->
                EventData(
                    calendarId = calendarData.id,
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    location = event.location,
                    wholeDayEvent = event.wholeDayEvent
                )
            }
            if (previousEventDataList != null) {
                for (event in previousEventDataList) {
                    if (!newEventDataList.contains(event)) {
                        eventDao.deleteEvent(event)
                    }
                }
            }
        }
        saveAllCalendarsToFirestoreDB(context, loggedInUser.email)
    }

    suspend fun removeUserFromCalendar(context: Context, myUser: User, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val usersDao = roomDB.sharedUsersDao()
        val calendarsDao = roomDB.calendarItemDao()

        calendarData.sharedPeopleNumber--

        val newCalendarData = CalendarData(
            calendarData.name,
            calendarData.sharedPeopleNumber,
            UserData(calendarData.id, calendarData.owner.username, calendarData.owner.email),
            calendarData.lastUpdated
        )
        calendarsDao.updateCalendarItem(newCalendarData)


        usersDao.deleteUserByCalendarId(calendarData.id, myUser.email)

        saveAllCalendarsToFirestoreDB(context, loggedInUser.email)
        getAllCalendarsFromFirestoreDB(context)

        removeUserFromFirestoreSharedCalendar(myUser, calendarData)
        deleteUserFromRealtimeSharedChat(myUser, calendarData)
    }

    private suspend fun addUserToRealtimeSharedChat(myUser: User, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        authenticateUser()

        val chatId = generateIdFromOwner(calendarData.owner.email, calendarData.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val existingChatRef = chatsRef.child(chatId)

        //check if chat exists
        existingChatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //chat exists
                    val usersRef = chatsRef.child(chatId).child("users")
                    val newUserList = calendarData.sharedPeople.toMutableList()
                    newUserList.add(calendarData.owner)
                    newUserList.sortWith(compareByDescending { it.email })

                    usersRef.setValue(newUserList)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val leaveMessage = FriendlyMessage(
                                    "# # # # # #\n${myUser.email} has been added to the chat\n# # # # # #",
                                    "System"
                                )
                                val messagesRef = existingChatRef.child("messages")
                                messagesRef.push().setValue(leaveMessage)
                            } else {
                                //error
                            }
                        }
                } else {
                    // Chat does not exist
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // error
            }
        })
    }

    private suspend fun deleteUserFromRealtimeSharedChat(myUser: User, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        authenticateUser()

        val chatId = generateIdFromOwner(calendarData.owner.email, calendarData.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")

        val chatsRef = database.getReference("chats")
        val existingChatRef = chatsRef.child(chatId)

        //check if chat exists
        existingChatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //chat exists
                    val chatsRef = database.getReference("chats")
                    val usersRef = chatsRef.child(chatId).child("users")

                    val newUserList = calendarData.sharedPeople.toMutableList()
                    newUserList.sortWith(compareByDescending { it.email })

                    newUserList.remove(myUser)
                    newUserList.add(calendarData.owner)
                    newUserList.sortWith(compareByDescending { it.email })
                    usersRef.setValue(newUserList).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val leaveMessage = FriendlyMessage(
                                "# # # # # #\n${myUser.email} has been removed from the chat\n# # # # # #",
                                "System"
                            )
                            val messagesRef = chatsRef.child(chatId).child("messages")
                            messagesRef.push().setValue(leaveMessage)
                        }
                    }
                } else {
                    // Chat does not exist
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // error
            }
        })
    }

    private suspend fun removeUserFromFirestoreSharedCalendar(myUser: User, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
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
            Log.e(TAG, "Error removing user from realtime database: $e")
        }
    }

    suspend fun addUserToCalendar(context: Context, myUser: User, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val usersDao = roomDB.sharedUsersDao()
        val calendarsDao = roomDB.calendarItemDao()

        calendarData.sharedPeopleNumber += 1

        val newCalendarData = CalendarData(
            calendarData.name,
            calendarData.sharedPeopleNumber,
            UserData(calendarData.id, myUser.username, myUser.email),
            calendarData.lastUpdated
        )
        calendarsDao.updateCalendarItem(newCalendarData)

        val newUserData = UserData(calendarData.id, myUser.username, myUser.email)

        usersDao.insertUser(newUserData)
        saveAllCalendarsToFirestoreDB(context, loggedInUser.email)
        getAllCalendarsFromFirestoreDB(context)

        saveSharedUserToFirestoreDB(myUser, calendarData.owner, calendarData.id)

        addUserToRealtimeSharedChat(myUser, calendarData)
    }

    private suspend fun saveSharedUserToFirestoreDB(myUser: User, owner: User, id: Long) {
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
            Log.e(TAG, "Error saving calendars to Firestore: $e")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun addEventToCalendar(context: Context, event: com.taskraze.myapplication.model.calendar.EventData, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        // maybe update events here as well
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val eventDao = roomDB.eventItemDao()

        val newEventData = EventData(
            calendarData.id,
            event.title,
            event.description,
            event.startTime,
            event.endTime,
            event.location,
            event.wholeDayEvent
        )

        eventDao.insertEvent(newEventData)
        val enetList = eventDao.getEventByCalendarId(calendarData.id)?.toMutableList()?.map { eventData ->
            com.taskraze.myapplication.model.calendar.EventData(
                eventData.title,
                eventData.description,
                eventData.startTime,
                eventData.endTime,
                eventData.location,
                eventData.wholeDayEvent
            )
        }?.toMutableList()
        if (enetList != null) {
            calendarData.events.clear()
            calendarData.events.addAll(enetList)
        }

        calendarData.lastUpdated = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())
        saveAllCalendarsToFirestoreDB(context, loggedInUser.email)
    }

    suspend fun saveAllCalendarsToFirestoreDB(context: Context, userId: String) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val calendars = getAllCalendars(context)
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

//        loggedInUser = loggedInDeferred.await()
        val userId = loggedInUser.email
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
                addCalendar(context, cal)
            }
        }
    }

    suspend fun deleteCalendarFromRoom(context: Context, calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val sharedPeopleDao = roomDB.sharedUsersDao()

        for (user in calendarData.sharedPeople) {
            val userData = sharedPeopleDao.getUserByEmail(user.email)
            if (userData != null) {
                sharedPeopleDao.deleteUser(userData)
            }
        }

        val newCalendar = calendarDao.getCalendarById(calendarData.id)
        if (newCalendar != null) {
            calendarDao.deleteCalendarItem(newCalendar)
        }
    }

    fun passCalendarToFragment(calendarData: com.taskraze.myapplication.model.calendar.CalendarData) {
        _CalendarDataToPass = calendarData
    }

    fun getCalendarToFragment(): com.taskraze.myapplication.model.calendar.CalendarData? {
        val temp = _CalendarDataToPass
        _CalendarDataToPass = null
        if (temp != null) {
            return temp
        } else {
            return null
        }
    }

    suspend fun deleteEventFromRoom(context: Context, event: com.taskraze.myapplication.model.calendar.EventData, calendarName: String) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val eventDao = roomDB.eventItemDao()

        val eventData = eventDao.getSpecificEvent(calendarName, event.title, event.startTime, event.endTime)
        if (eventData != null) {
            eventDao.deleteEvent(eventData)
        }
    }

    suspend fun getFriendRequests(callback: (List<FriendRequestData>) -> Unit) {
        authenticateUser()

        val receiverQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("receiverId", loggedInUser.email)

        val senderQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("senderId", loggedInUser.email)

        val friendRequestDataList = mutableListOf<FriendRequestData>()
        receiverQuery.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val receiverId = document.getString("receiverId") ?: ""
                    val senderId = document.getString("senderId") ?: ""
                    val status = document.getString("status") ?: ""
                    val friendRequestData = FriendRequestData(receiverId, senderId, status)
                    if (status == "pending") {
                        friendRequestDataList.add(friendRequestData)
                    }
                }

                senderQuery.get().addOnSuccessListener { result2 ->
                    for (document in result2) {
                        val receiverId = document.getString("receiverId") ?: ""
                        val senderId = document.getString("senderId") ?: ""
                        val status = document.getString("status") ?: ""
                        val friendRequestData = FriendRequestData(receiverId, senderId, status)
                        if (status == "pending") {
                            friendRequestDataList.add(friendRequestData)
                        }
                    }

                    callback(friendRequestDataList)
                }

            }
            .addOnFailureListener { _ ->
                Log.d(TAG, "failed to retrieve friend requests")
            }
    }

    fun handleFriendRequest(choice: String, friendRequestData: FriendRequestData, callback: (Boolean) -> Unit) {
        val friendRequestsCollection = firestoreDB.collection("friend_requests")


        friendRequestsCollection
            .whereEqualTo("senderId", friendRequestData.senderId)
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

    suspend fun getFriends(callback: (MutableList<User>) -> Unit) {
        authenticateUser()
//        loggedInUser = loggedInDeferred.await()
        if (loggedInUser != null) {
            firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", loggedInUser.email)
                .get()
                .addOnSuccessListener { result ->
                    val acceptedFriends = mutableListOf<FriendRequestData>()
                    val rejectedFriends = mutableListOf<FriendRequestData>()
                    for (document in result) {
                        val receiverId = document.getString("receiverId") ?: ""
                        val senderId = document.getString("senderId") ?: ""
                        val status = document.getString("status") ?: ""
                        val friendRequestData = FriendRequestData(receiverId, senderId, status)
                        if (status == "accepted") {
                            acceptedFriends.add(friendRequestData)
                        } else if (status == "rejected") {
                            rejectedFriends.add(friendRequestData)
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

    suspend fun fetchUsersFromFriendsList(callback: (MutableList<User>) -> Unit) {
        authenticateUser()
        firestoreDB.collection("user_friends")
            .document(loggedInUser.email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val friendsList = documentSnapshot.toObject(UserFriendsData::class.java)?.friends ?: emptyList()

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

    private fun updateOrCreateUserFriendsDocument(acceptedFriends: List<FriendRequestData>): Deferred<Unit> {
        val currentEmail = loggedInUser.email
        val deferred = CompletableDeferred<Unit>()

        // Update or create the document in Firestore
        firestoreDB.collection("user_friends")
            .document(currentEmail)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val currentUserFriendsData = documentSnapshot.toObject(UserFriendsData::class.java)
                val existingFriends = currentUserFriendsData?.friends ?: emptyList()

                // Merge existing friends with new friends
                val updatedFriends = existingFriends.toMutableList()
                acceptedFriends.forEach { friendRequest ->
                    if (!existingFriends.contains(friendRequest.senderId)) {
                        updatedFriends.add(friendRequest.senderId)
                    }
                }

                // Update the document with merged friends list
                val userFriendsMap = mapOf(
                    "userId" to currentEmail,
                    "friends" to updatedFriends
                )
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
                            val friendFriends = documentSnapshot.toObject(UserFriendsData::class.java)
                            val updatedFriends = friendFriends?.friends?.toMutableList() ?: mutableListOf()
                            if (!updatedFriends.contains(loggedInUser.email)) {
                                updatedFriends.add(loggedInUser.email)
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

    private fun deleteRejectedFriendRequests(rejectedFriends: List<FriendRequestData>) {
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

    private fun deleteAcceptedFriendRequests(acceptedFriends: List<FriendRequestData>) {
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

    fun generateIdFromEmails(receiverUserEmail: String, senderUserEmail: String): String {
        val sortedStrings = listOf(receiverUserEmail, senderUserEmail).sorted()

        val combinedString = sortedStrings.joinToString(separator = "")

        val digest = MessageDigest.getInstance("SHA-256")

        val hashedBytes = digest.digest(combinedString.toByteArray())

        val stringBuilder = StringBuilder()
        for (byte in hashedBytes) {
            stringBuilder.append(String.format("%02x", byte))
        }

        return stringBuilder.toString()
    }

    fun generateIdFromOwner(ownerEmail: String, calendarId: String): String {

        val combinedString = ownerEmail + calendarId

        val digest = MessageDigest.getInstance("SHA-256")

        val hashedBytes = digest.digest(combinedString.toByteArray())

        val stringBuilder = StringBuilder()
        for (byte in hashedBytes) {
            stringBuilder.append(String.format("%02x", byte))
        }

        return stringBuilder.toString()
    }

    private suspend fun checkExistingChat(user1: User, user2: User): Boolean {
        val chatId = '-' + generateIdFromEmails(user1.email, user2.email)

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")

        val chatSnapshot = chatsRef.child(chatId).get().await()
        return chatSnapshot.exists()
    }

    private suspend fun checkExistingChat(calendar: com.taskraze.myapplication.model.calendar.CalendarData): Boolean {
        val chatId = generateIdFromOwner(calendar.owner.email, calendar.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")

        val chatSnapshot = chatsRef.child(chatId).get().await()
        return chatSnapshot.exists()
    }

    suspend fun startNewChat(chosenFriend: User): ChatData? {
        authenticateUser()

        val existingChat = checkExistingChat(loggedInUser, chosenFriend)
        if (existingChat) {
            return null
        }

        val newChat = ChatData()
        val chatId = "-" + generateIdFromEmails(loggedInUser.email, chosenFriend.email)
        newChat.id = chatId
        newChat.title = chosenFriend.email + "&" + loggedInUser.email
        newChat.users.add(loggedInUser)
        newChat.users.add(chosenFriend)
        newChat.users.sortWith(compareByDescending { it.email })

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val chatRef = chatsRef.child(chatId)


        chatRef.setValue(newChat)
            .addOnSuccessListener {
                Log.d(TAG, "New chat created: $chatId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving chat", e)
            }

        chatRef.child("messages").push().setValue(FriendlyMessage("This is the start of your chat.", "", "", ""))

        return newChat
    }

    suspend fun quitChat(context: Context, chatData: ChatData) {
        authenticateUser()

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val usersRef = chatsRef.child(chatData.id).child("users")

        chatData.users.sortWith(compareByDescending { it.email })

        val index = chatData.users.indexOfFirst { it.email == loggedInUser.email }
        chatData.users.removeAt(index)
        usersRef.setValue(chatData.users).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Quit chat successfully", Toast.LENGTH_SHORT).show()

                val leaveMessage =
                    FriendlyMessage("# # # # # #\n${loggedInUser.email} has left the chat\n# # # # # #", "System")
                val messagesRef = chatsRef.child(chatData.id).child("messages")
                messagesRef.push().setValue(leaveMessage)

                if (chatData.users.size == 0) {
                    chatsRef.child(chatData.id).removeValue()
                }
            }
        }
    }

    suspend fun getSharedCalendars(requireContext: Context): MutableList<com.taskraze.myapplication.model.calendar.CalendarData> {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")

        val userId = loggedInUser.email
        val resultList = mutableListOf<com.taskraze.myapplication.model.calendar.CalendarData>()
        try {
            val userSharedCalendarsDoc = userInCalendarsCollection.document(userId).get().await()

            if (userSharedCalendarsDoc.exists()) {
                val userCalendars = userSharedCalendarsDoc.get("owners") as? List<*>


                for (calendar in userCalendars!!) {
                    val calendarMap = calendar as? Map<*, *>
                    val calendarId = calendarMap?.get("calendarId") as? Long
                    val ownerId = calendarMap?.get("userId") as? String

                    if (ownerId != null) {

                        val allData = try {
                            val userDocument = calendarsCollection.document(ownerId).get().await()

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
                            for (cal in allData) {
                                if (cal.sharedPeople.contains(loggedInUser)) {
                                    resultList.add(cal)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // error
        }

        return resultList
    }

    suspend fun addEventToSharedCalendar(event: com.taskraze.myapplication.model.calendar.EventData, thisCalendar: com.taskraze.myapplication.model.calendar.CalendarData) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val existingDoc =
                calendarsCollection.document(thisCalendar.owner.email).get().await()

            val calendarList = existingDoc.toObject(UserFirestoreData::class.java)?.calendars
            if (calendarList != null) {
                for (calendar in calendarList) {
                    if (calendar.id == thisCalendar.id) {
                        calendar.events.add(event)
                        thisCalendar.events.add(event)
                    }
                }

                calendarsCollection.document(thisCalendar.owner.email).update("calendars", calendarList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding event shared calendar: $e")
        }
    }

    suspend fun deleteSharedUsersFromCalendar(sharedUsers: MutableList<User>, calendar: com.taskraze.myapplication.model.calendar.CalendarData) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")
        try {

            for (myUser in sharedUsers) {
                val existingDoc = userInCalendarsCollection.document(myUser.email).get().await()

                if (existingDoc.exists()) {
                    val ownerList = existingDoc.get("owners") as? List<*>
                    val userData = hashMapOf(
                        "userId" to calendar.owner.email,
                        "calendarId" to calendar.id
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendars to Firestore: $e")
        }
    }

    suspend fun deleteEventFromSharedCalendar(event: com.taskraze.myapplication.model.calendar.EventData, userId: String, calendarId: Long) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val existingDoc =
                calendarsCollection.document(userId).get().await()

            val calendarList = existingDoc.toObject(UserFirestoreData::class.java)?.calendars
            if (calendarList != null) {
                for (calendar in calendarList) {
                    if (calendar.id == calendarId) {
                        calendar.events.remove(event)
                    }
                }

                calendarsCollection.document(userId).update("calendars", calendarList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendars to Firestore: $e")
        }
    }

    fun removeUserFromFriends(context: Context, deletedUser: User) {
        firestoreDB.collection("user_friends")
            .document(loggedInUser.email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                //current user
                val currentUserFriendsData = documentSnapshot.toObject(UserFriendsData::class.java)
                val existingFriends = currentUserFriendsData?.friends ?: emptyList()

                val newFriendsList = existingFriends.toMutableList()
                newFriendsList.remove(deletedUser.email)
                firestoreDB.collection("user_friends")
                    .document(loggedInUser.email).update("friends", newFriendsList)

                //other user
                firestoreDB.collection("user_friends")
                    .document(deletedUser.email)
                    .get()
                    .addOnSuccessListener { OtherDocumentSnapshot ->
                        val otherUserFriendsData = OtherDocumentSnapshot.toObject(UserFriendsData::class.java)
                        val otherExistingFriends = otherUserFriendsData?.friends ?: emptyList()

                        val otherNewFriendsList = otherExistingFriends.toMutableList()
                        otherNewFriendsList.remove(loggedInUser.email)

                        firestoreDB.collection("user_friends")
                            .document(deletedUser.email).update("friends", otherNewFriendsList)

                    }
                    .addOnFailureListener { _ ->
                        Toast.makeText(context, "Could not delete user from friends", Toast.LENGTH_SHORT).show()
                    }

            }
            .addOnFailureListener { _ ->
                Toast.makeText(context, "Could not delete user from friends", Toast.LENGTH_SHORT).show()
            }
    }

    suspend fun startGroupChat(calendar: com.taskraze.myapplication.model.calendar.CalendarData): ChatData? {
        authenticateUser()

        val existingChat = checkExistingChat(calendar)
        if (existingChat) {
            return null
        }

        val newChat = ChatData()
        val chatId = generateIdFromOwner(calendar.owner.email, calendar.id.toString())
        newChat.id = chatId
        newChat.title = calendar.name
        newChat.users.add(loggedInUser)
        for (user in calendar.sharedPeople) {
            newChat.users.add(user)
        }
        newChat.users.sortWith(compareByDescending { it.email })

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val chatRef = chatsRef.child(chatId)


        chatRef.setValue(newChat)
            .addOnSuccessListener {
                Log.d(TAG, "New chat created: $chatId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving chat", e)
            }

        chatRef.child("messages").push().setValue(FriendlyMessage("This is the start of your chat.", "", "", ""))

        return newChat
    }
}