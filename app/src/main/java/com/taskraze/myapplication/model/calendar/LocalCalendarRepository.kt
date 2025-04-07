package com.taskraze.myapplication.model.calendar

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.chat.ChatRepository
import com.taskraze.myapplication.model.room_database.data_classes.RoomCalendarData
import com.taskraze.myapplication.model.room_database.data_classes.RoomEventData
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.model.room_database.data_classes.UserData
import com.taskraze.myapplication.model.room_database.db.AppDatabase
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class LocalCalendarRepository {
    private val authRepository = AuthRepository()
    private val firestoreCalendarRepository = FirestoreCalendarRepository(this)
    private val chatRepository = ChatRepository()
    suspend fun getAllCalendarsLocal(context: Context): MutableList<CalendarData> {

        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
//        val eventDao = roomDB.eventItemDao()
//        val sharedUsersDao = roomDB.sharedUsersDao()


        // authRepository.fetchUserDetails()
        val loggedInUserJson =
            Gson().toJson(UserData(0, AuthViewModel.loggedInUser.username, AuthViewModel.loggedInUser.email)).toString()

        val myCalendarList = mutableListOf<CalendarData>()

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

                val eventList = mutableListOf<EventData>()
                for (eventData in eventDataForCalendar) {
                    eventList.add(
                        EventData(
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
                    CalendarData(
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

    suspend fun addOrUpdateCalendarLocal(context: Context, calendarData: CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val calendarDao = roomDB.calendarItemDao()
        val sharedPeopleDao = roomDB.sharedUsersDao()
        val eventDao = roomDB.eventItemDao()

        val existingCalendar = calendarDao.getCalendarById(calendarData.id)
//        val existingUsers = sharedPeopleDao.getAllUsers()

        if (existingCalendar == null) {
            for (user in calendarData.sharedPeople) {
                val userData = UserData(calendarData.id, user.username, user.email)
                sharedPeopleDao.insertUser(userData)
            }

            val eventList = calendarData.events.map { event ->
                RoomEventData(
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

            var newCalendar: RoomCalendarData
            val longValue: Long = -1
            if (calendarData.id == longValue) {
                newCalendar = RoomCalendarData(
                    name = calendarData.name,
                    sharedPeopleNumber = calendarData.sharedPeopleNumber,
                    owner = UserData(0, calendarData.owner.username, calendarData.owner.email),
                    lastUpdated = calendarData.lastUpdated
                )
            } else {
                newCalendar = RoomCalendarData(
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
            val newRoomEventDataList = calendarData.events.map { event ->
                RoomEventData(
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
                    if (!newRoomEventDataList.contains(event)) {
                        eventDao.deleteEvent(event)
                    }
                }
            }
        }
        // save to firebase
        firestoreCalendarRepository.saveAllCalendarsToFirestoreDB(context, AuthViewModel.loggedInUser.email)
    }

    suspend fun removeUserFromCalendar(context: Context, myUser: User, calendarData: CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val usersDao = roomDB.sharedUsersDao()
        val calendarsDao = roomDB.calendarItemDao()

        calendarData.sharedPeopleNumber--

        val newRoomCalendarData = RoomCalendarData(
            calendarData.name,
            calendarData.sharedPeopleNumber,
            UserData(calendarData.id, calendarData.owner.username, calendarData.owner.email),
            calendarData.lastUpdated
        )

        //update local calendar
        calendarsDao.updateCalendarItem(newRoomCalendarData)

        //delete local user
        usersDao.deleteUserByCalendarId(calendarData.id, myUser.email)

        //save to Firestore
        firestoreCalendarRepository.saveAllCalendarsToFirestoreDB(context, AuthViewModel.loggedInUser.email)
        firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(context)

        //remove user from shared calendar
        firestoreCalendarRepository.removeUserFromFirestoreSharedCalendar(myUser, calendarData)

        //remove user from realtime chat
        chatRepository.deleteUserFromRealtimeSharedChat(myUser, calendarData)
    }

    suspend fun addUserToCalendar(context: Context, myUser: User, calendarData: CalendarData) {
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val usersDao = roomDB.sharedUsersDao()
        val calendarsDao = roomDB.calendarItemDao()

        calendarData.sharedPeopleNumber += 1

        val newRoomCalendarData = RoomCalendarData(
            calendarData.name,
            calendarData.sharedPeopleNumber,
            UserData(calendarData.id, myUser.username, myUser.email),
            calendarData.lastUpdated
        )
        calendarsDao.updateCalendarItem(newRoomCalendarData)

        val newUserData = UserData(calendarData.id, myUser.username, myUser.email)

        usersDao.insertUser(newUserData)
        firestoreCalendarRepository.saveAllCalendarsToFirestoreDB(context, AuthViewModel.loggedInUser.email)
        firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(context)

        firestoreCalendarRepository.saveSharedUserToFirestoreDB(myUser, calendarData.owner, calendarData.id)

        chatRepository.addUserToRealtimeSharedChat(myUser, calendarData)
    }

    suspend fun addEventToCalendar(context: Context, event: EventData, calendarData: CalendarData) {
        // maybe update events here as well
        val roomDB = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "database-name"
        ).build()

        val eventDao = roomDB.eventItemDao()

        val newRoomEventData = RoomEventData(
            calendarData.id,
            event.title,
            event.description,
            event.startTime,
            event.endTime,
            event.location,
            event.wholeDayEvent
        )

        eventDao.insertEvent(newRoomEventData)
        val enetList = eventDao.getEventByCalendarId(calendarData.id)?.toMutableList()?.map { eventData ->
            EventData(
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
        firestoreCalendarRepository.saveAllCalendarsToFirestoreDB(context, AuthViewModel.loggedInUser.email)
    }

    suspend fun deleteCalendarLocal(context: Context, calendarData: CalendarData) {
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
}