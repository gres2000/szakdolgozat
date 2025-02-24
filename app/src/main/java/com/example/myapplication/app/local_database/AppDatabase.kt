package com.example.myapplication.app.local_database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CalendarData::class, UserData::class, EventData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarItemDao(): CalendarItemDao
    abstract fun sharedUsersDao(): SharedUsersDao
    abstract fun eventItemDao(): EventItemDao
}