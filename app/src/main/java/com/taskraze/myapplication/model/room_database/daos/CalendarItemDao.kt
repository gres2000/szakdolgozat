package com.taskraze.myapplication.model.room_database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.taskraze.myapplication.model.room_database.data_classes.RoomCalendarData
import com.taskraze.myapplication.model.room_database.data_classes.RoomEventData
import com.taskraze.myapplication.model.room_database.data_classes.UserData

@Dao
interface CalendarItemDao {
    @Insert
    suspend fun insertCalendarItem(roomCalendarData: RoomCalendarData): Long

    @Update
    suspend fun updateCalendarItem(roomCalendarData: RoomCalendarData)

    @Delete
    suspend fun deleteCalendarItem(roomCalendarData: RoomCalendarData)

    @Query("SELECT * FROM calendar_items")
    suspend fun getAllCalendars(): List<RoomCalendarData>
//    OR name IN(SELECT calendarId FROM user WHERE emailAddress = :AuthViewModel.loggedInUserEmail)
    @Query("SELECT * FROM calendar_items WHERE owner = :loggedInUser ")
    suspend fun getAllCalendarsForUser(loggedInUser: String): MutableList<RoomCalendarData>

    @Query("SELECT * FROM user WHERE calendarId = :calendarId")
    suspend fun getSharedPeopleForCalendar(calendarId: String): List<UserData>

    @Query("SELECT * FROM event WHERE calendarId = :calendarId")
    suspend fun getEventsForCalendar(calendarId: Long): List<RoomEventData>

    @Query("SELECT * FROM calendar_items WHERE id = :calendarId")
    suspend fun getCalendarById(calendarId: Long): RoomCalendarData?
}
