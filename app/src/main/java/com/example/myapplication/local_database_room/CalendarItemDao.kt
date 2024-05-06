package com.example.myapplication.local_database_room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.google.gson.JsonArray

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

    @Query("SELECT * FROM calendar_items WHERE owner = :loggedInUser OR name IN(SELECT calendarId FROM user WHERE emailAddress = :loggedInUserEmail)")
    suspend fun getAllCalendarsForUser(loggedInUser: String, loggedInUserEmail: String): List<CalendarData>

    @Query("SELECT * FROM user WHERE calendarId = :calendarId")
    suspend fun getSharedPeopleForCalendar(calendarId: String): List<UserData>

    @Query("SELECT * FROM event WHERE calendarId = :calendarId")
    suspend fun getEventsForCalendar(calendarId: String): List<EventData>

    @Query("SELECT * FROM calendar_items WHERE name = :calendarName")
    suspend fun getCalendarByName(calendarName: String): CalendarData?
}
