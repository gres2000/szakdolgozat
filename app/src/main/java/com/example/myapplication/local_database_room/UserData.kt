package com.example.myapplication.local_database_room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "user",
    primaryKeys = ["calendarId", "emailAddress"]
)
data class UserData(
    val calendarId: Long,
    val username: String,
    val emailAddress: String
)