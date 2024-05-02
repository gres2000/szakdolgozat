package com.example.myapplication.viewModel

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.Room
import com.example.myapplication.authentication.User
import com.example.myapplication.calendar.MyCalendar
import com.example.myapplication.local_database_room.AppDatabase
import com.example.myapplication.local_database_room.CalendarData
import com.example.myapplication.local_database_room.EventData
import com.example.myapplication.local_database_room.UserData
import com.example.myapplication.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await

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
    init {

    }
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

        authenticateUser()

        loggedInUser = loggedInDeferred.await()

        val loggedInUserJson = Gson().toJson(UserData(null, loggedInUser!!.username, loggedInUser!!.emailAddress)).toString()

        val myCalendarList = mutableListOf<MyCalendar>()

        val calendarDataList = calendarDao.getAllCalendarsForUser(loggedInUserJson, loggedInUser!!.emailAddress)
//        val calendarDataList = calendarDao.getAllCalendars()
        val tempcales = calendarDao.getAllCalendars()
        val k = tempcales.size
        if (calendarDataList.isNotEmpty()) {

            for (calendarData in calendarDataList) {
                val sharedPeopleData = calendarDao.getSharedPeopleForCalendar(calendarData.id.toString())

                val sharedPeopleList = mutableListOf<User>()
                for (userData in sharedPeopleData) {
                    sharedPeopleList.add(User(userData.username, userData.emailAddress))
                }

                myCalendarList.add(
                    MyCalendar(
                        name = calendarData.name,
                        sharedPeopleNumber = calendarData.sharedPeopleNumber,
                        sharedPeople = sharedPeopleList,
                        owner = User(calendarData.owner.username, calendarData.owner.emailAddress),
                        events = mutableListOf(), // Populate events as needed
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
        val sharedPeopleDao = roomDB.sharedUsersDao() // Assuming you have a DAO for shared people

        for (user in myCalendar.sharedPeople) {
            val userData = UserData(myCalendar.name, user.username, user.emailAddress)
            sharedPeopleDao.insertUser(userData)
        }

        // Create CalendarData object
        val newCalendar = CalendarData(
            name = myCalendar.name,
            sharedPeopleNumber = myCalendar.sharedPeopleNumber,
            owner = UserData(null, myCalendar.owner.username, myCalendar.owner.emailAddress),
            eventList = myCalendar.events.map { event ->
                EventData(
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime,
                    endTime = event.endTime,
                    location = event.location
                )
            }.toMutableList(),
            lastUpdated = myCalendar.lastUpdated
        )
        calendarDao.insertCalendarItem(newCalendar)
    }

    suspend fun saveAllCalendarsToFirestoreDB(context: Context, userId: String) {
        val firestoreDB = FirebaseFirestore.getInstance()
        val calendarsCollection = firestoreDB.collection("calendars")
        try {
            val calendars = getAllCalendars(context)
            loggedInUser = loggedInDeferred.await()
            val loggedInUserEmail = loggedInUser?.emailAddress

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

            // Log success message
            Log.d(TAG, "Calendars saved to Firestore for user: $loggedInUserEmail")
        } catch (e: Exception) {
            // Log error message
            Log.e(TAG, "Error saving calendars to Firestore: $e")
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
            val userData = sharedPeopleDao.getUserByEmail(user.emailAddress)
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
}