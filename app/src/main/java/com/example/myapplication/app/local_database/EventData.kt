package com.example.myapplication.app.local_database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "event")
data class EventData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calendarId: Long,
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null,
    var wholeDayEvent: Boolean
) {
    // Secondary constructor to allow creation without specifying the ID
    constructor(
        calendarId: Long,
        title: String,
        description: String?,
        startTime: Date,
        endTime: Date,
        location: String?,
        wholeDayEvent: Boolean
    ) : this(
        id = 0,
        calendarId = calendarId,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        location = location,
        wholeDayEvent = wholeDayEvent
    )
}