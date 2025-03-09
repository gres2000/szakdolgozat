package com.taskraze.myapplication.room_database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taskraze.myapplication.room_database.daos.CalendarItemDao
import com.taskraze.myapplication.room_database.daos.EventItemDao
import com.taskraze.myapplication.room_database.daos.SharedUsersDao
import com.taskraze.myapplication.room_database.data_classes.CalendarData
import com.taskraze.myapplication.room_database.data_classes.EventData
import com.taskraze.myapplication.room_database.data_classes.UserData

@Database(entities = [CalendarData::class, UserData::class, EventData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarItemDao(): CalendarItemDao
    abstract fun sharedUsersDao(): SharedUsersDao
    abstract fun eventItemDao(): EventItemDao
}