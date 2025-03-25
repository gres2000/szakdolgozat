package com.taskraze.myapplication.model.room_database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.taskraze.myapplication.model.room_database.data_classes.UserData

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