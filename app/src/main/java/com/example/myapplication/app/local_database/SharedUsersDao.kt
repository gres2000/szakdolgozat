package com.example.myapplication.app.local_database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SharedUsersDao {
    @Query("SELECT * FROM user WHERE emailAddress = :email")
    suspend fun getUserByEmail(email: String): UserData?

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<UserData>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserData): Long
    @Delete
    suspend fun deleteUser(user: UserData)

    @Query("DELETE FROM user WHERE calendarId = :calendarId AND emailAddress = :emailAddress")
    suspend fun deleteUserByCalendarId(calendarId: Long, emailAddress: String)
}