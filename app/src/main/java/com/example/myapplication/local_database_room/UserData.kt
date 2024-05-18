package com.example.myapplication.local_database_room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user")
data class UserData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calendarId: Long,
    val username: String,
    val emailAddress: String
){
    // Secondary constructor to allow creation without specifying the ID
    constructor(
        calendarId: Long,
        username: String,
        emailAddress: String
    ) : this(
        id = 0,
        calendarId = calendarId,
        username = username,
        emailAddress = emailAddress
    )
}