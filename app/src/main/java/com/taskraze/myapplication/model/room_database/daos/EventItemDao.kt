package com.taskraze.myapplication.model.room_database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.taskraze.myapplication.model.room_database.data_classes.RoomEventData
import java.util.Date

@Dao
interface EventItemDao {

    @Query("SELECT * FROM event WHERE calendarId = :calendarId")
    suspend fun getEventByCalendarId(calendarId: Long): List<RoomEventData>?

    @Query("SELECT * FROM event WHERE calendarId = :calendarName AND title = :title AND startTime = :startTime AND endtime = :endTime")
    suspend fun getSpecificEvent(calendarName: String, title: String, startTime: Date?, endTime: Date?): RoomEventData?
    @Insert
    suspend fun insertEvent(event: RoomEventData): Long
    @Delete
    suspend fun deleteEvent(event: RoomEventData)
    @Update
    suspend fun updateEvent(event: RoomEventData)
}