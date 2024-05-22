package com.example.myapplication.local_database_room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CalendarItemDao {
    @Insert
    suspend fun insertCalendarItem(calendarData: CalendarData): Long

    @Update
    suspend fun updateCalendarItem(calendarData: CalendarData)

    @Delete
    suspend fun deleteCalendarItem(calendarData: CalendarData)

    @Query("SELECT * FROM calendar_items")
    suspend fun getAllCalendars(): List<CalendarData>
//    OR name IN(SELECT calendarId FROM user WHERE emailAddress = :loggedInUserEmail)
    @Query("SELECT * FROM calendar_items WHERE owner = :loggedInUser ")
    suspend fun getAllCalendarsForUser(loggedInUser: String): MutableList<CalendarData>

    @Query("SELECT * FROM user WHERE calendarId = :calendarId")
    suspend fun getSharedPeopleForCalendar(calendarId: String): List<UserData>

    @Query("SELECT * FROM event WHERE calendarId = :calendarId")
    suspend fun getEventsForCalendar(calendarId: Long): List<EventData>

    @Query("SELECT * FROM calendar_items WHERE id = :calendarId")
    suspend fun getCalendarById(calendarId: Long): CalendarData?
}
