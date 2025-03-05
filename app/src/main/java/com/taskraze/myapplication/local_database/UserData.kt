package com.taskraze.myapplication.local_database

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