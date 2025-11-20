package com.taskraze.myapplication.viewmodel

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.model.calendar.UserFirestoreData
import com.taskraze.myapplication.model.chat.ChatData
import com.taskraze.myapplication.model.chat.FriendlyMessage
import com.taskraze.myapplication.model.room_database.db.AppDatabase
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.friends.FriendRequestData
import com.taskraze.myapplication.model.friends.UserFriendsData
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Calendar

object MainViewModel : ViewModel() {
    private val firestoreDB = FirebaseFirestore.getInstance()
    //helper variables
    var auth = Firebase.auth
    private var _CalendarDataToPass: CalendarData? = null
    var newEventStartingDay: Calendar? = null
    private val authViewModel = AuthViewModel


    init {
        viewModelScope.launch {

            AuthViewModel
        }
    }
    fun passCalendarToFragment(calendarData: CalendarData) {
        _CalendarDataToPass = calendarData
    }

    fun getCalendarToFragment(): CalendarData? {
        val temp = _CalendarDataToPass
        _CalendarDataToPass = null
        if (temp != null) {
            return temp
        } else {
            return null
        }
    }

    suspend fun deleteEventFromRoom(context: Context, event: EventData, calendarName: String) {
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
        // authRepository.fetchUserDetails()

        val receiverQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("receiverId", AuthViewModel.loggedInUser.email)

        val senderQuery = firestoreDB.collection("friend_requests")
            .whereEqualTo("senderId", AuthViewModel.loggedInUser.email)

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
        // authRepository.fetchUserDetails()
        // AuthViewModel.loggedInUser = loggedInDeferred.await()
        if (AuthViewModel.loggedInUser != null) {
            firestoreDB.collection("friend_requests")
                .whereEqualTo("receiverId", AuthViewModel.loggedInUser.email)
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
        // authRepository.fetchUserDetails()
        firestoreDB.collection("user_friends")
            .document(AuthViewModel.loggedInUser.email)
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
        val currentEmail = AuthViewModel.loggedInUser.email
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
                            if (!updatedFriends.contains(AuthViewModel.loggedInUser.email)) {
                                updatedFriends.add(AuthViewModel.loggedInUser.email)
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

    private suspend fun checkExistingChat(calendar: CalendarData): Boolean {
        val chatId = generateIdFromOwner(calendar.owner.email, calendar.id.toString())

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")

        val chatSnapshot = chatsRef.child(chatId).get().await()
        return chatSnapshot.exists()
    }

    suspend fun startNewChat(chosenFriend: User): ChatData? {
        // authRepository.fetchUserDetails()

        val existingChat = checkExistingChat(AuthViewModel.loggedInUser, chosenFriend)
        if (existingChat) {
            return null
        }

        val newChat = ChatData()
        val chatId = "-" + generateIdFromEmails(AuthViewModel.loggedInUser.email, chosenFriend.email)
        newChat.id = chatId
        newChat.title = chosenFriend.email + "&" + AuthViewModel.loggedInUser.email
        newChat.users.add(AuthViewModel.loggedInUser)
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
        // authRepository.fetchUserDetails()

        val database =
            FirebaseDatabase.getInstance("https://szakdolgozat-7f789-default-rtdb.europe-west1.firebasedatabase.app/")
        val chatsRef = database.getReference("chats")
        val usersRef = chatsRef.child(chatData.id).child("users")

        chatData.users.sortWith(compareByDescending { it.email })

        val index = chatData.users.indexOfFirst { it.email == AuthViewModel.loggedInUser.email }
        chatData.users.removeAt(index)
        usersRef.setValue(chatData.users).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Quit chat successfully", Toast.LENGTH_SHORT).show()

                val leaveMessage =
                    FriendlyMessage("# # # # # #\n${AuthViewModel.loggedInUser.email} has left the chat\n# # # # # #", "System")
                val messagesRef = chatsRef.child(chatData.id).child("messages")
                messagesRef.push().setValue(leaveMessage)

                if (chatData.users.size == 0) {
                    chatsRef.child(chatData.id).removeValue()
                }
            }
        }
    }

    suspend fun getSharedCalendars(requireContext: Context): MutableList<CalendarData> {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        val userInCalendarsCollection = firestoreDB.collection("user_in_calendars")

        val userId = AuthViewModel.loggedInUser.email
        val resultList = mutableListOf<CalendarData>()
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
                                if (cal.sharedPeople.contains(AuthViewModel.loggedInUser)) {
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

    suspend fun addEventToSharedCalendar(event: EventData, thisCalendar: CalendarData) {
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

    suspend fun deleteSharedUsersFromCalendar(sharedUsers: MutableList<User>, calendar: CalendarData) {
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

    suspend fun deleteEventFromSharedCalendar(event: EventData, userId: String, calendarId: Long) {
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
            .document(AuthViewModel.loggedInUser.email)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                //current user
                val currentUserFriendsData = documentSnapshot.toObject(UserFriendsData::class.java)
                val existingFriends = currentUserFriendsData?.friends ?: emptyList()

                val newFriendsList = existingFriends.toMutableList()
                newFriendsList.remove(deletedUser.email)
                firestoreDB.collection("user_friends")
                    .document(AuthViewModel.loggedInUser.email).update("friends", newFriendsList)

                //other user
                firestoreDB.collection("user_friends")
                    .document(deletedUser.email)
                    .get()
                    .addOnSuccessListener { OtherDocumentSnapshot ->
                        val otherUserFriendsData = OtherDocumentSnapshot.toObject(UserFriendsData::class.java)
                        val otherExistingFriends = otherUserFriendsData?.friends ?: emptyList()

                        val otherNewFriendsList = otherExistingFriends.toMutableList()
                        otherNewFriendsList.remove(AuthViewModel.loggedInUser.email)

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

    suspend fun startGroupChat(calendar: CalendarData): ChatData? {
        // authRepository.fetchUserDetails()

        val existingChat = checkExistingChat(calendar)
        if (existingChat) {
            return null
        }

        val newChat = ChatData()
        val chatId = generateIdFromOwner(calendar.owner.email, calendar.id.toString())
        newChat.id = chatId
        newChat.title = calendar.name
        newChat.users.add(AuthViewModel.loggedInUser)
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