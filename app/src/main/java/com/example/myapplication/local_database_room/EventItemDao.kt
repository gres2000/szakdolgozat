package com.example.myapplication.local_database_room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.calendar.Event
import java.util.Date

@Dao
interface EventItemDao {
    @Query("SELECT * FROM event WHERE calendarId = :calendarId")
    suspend fun getEventByCalendarName(calendarId: String): List<EventData>?

    @Query("SELECT * FROM event WHERE calendarId = :calendarId AND title = :title AND startTime = :startTime AND endtime = :endTime")
    suspend fun getSpecificEvent(calendarId: String, title: String, startTime: Date?, endTime: Date?): EventData?
    @Insert
    suspend fun insertEvent(event: EventData): Long
    @Delete
    suspend fun deleteEvent(event: EventData)
    @Update
    suspend fun updateEvent(event: EventData)
}