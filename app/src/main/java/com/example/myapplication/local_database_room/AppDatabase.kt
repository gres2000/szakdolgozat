package com.example.myapplication.local_database_room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CalendarData::class, UserData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarItemDao(): CalendarItemDao
    abstract fun sharedUsersDao(): SharedUsersDao
}