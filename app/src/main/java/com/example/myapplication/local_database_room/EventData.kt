package com.example.myapplication.local_database_room

import androidx.room.Entity
import java.util.Date

@Entity(tableName = "event")
data class EventData(
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null
)