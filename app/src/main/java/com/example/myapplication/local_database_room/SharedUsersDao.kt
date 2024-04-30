package com.example.myapplication.local_database_room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SharedUsersDao {
    @Query("SELECT * FROM user WHERE emailAddress = :email")
    suspend fun getUserByEmail(email: String): UserData?

    @Insert
    suspend fun insertUser(user: UserData): Long
}