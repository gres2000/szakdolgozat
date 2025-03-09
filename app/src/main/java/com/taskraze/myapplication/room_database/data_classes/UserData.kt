package com.taskraze.myapplication.room_database.data_classes

import androidx.room.Entity

@Entity(
    tableName = "user",
    primaryKeys = ["calendarId", "emailAddress"]
)
data class UserData(
    val calendarId: Long,
    val username: String,
    val emailAddress: String
)