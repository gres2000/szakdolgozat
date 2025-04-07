package com.taskraze.myapplication.model.room_database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taskraze.myapplication.model.room_database.daos.CalendarItemDao
import com.taskraze.myapplication.model.room_database.daos.EventItemDao
import com.taskraze.myapplication.model.room_database.daos.SharedUsersDao
import com.taskraze.myapplication.model.room_database.data_classes.RoomCalendarData
import com.taskraze.myapplication.model.room_database.data_classes.RoomEventData
import com.taskraze.myapplication.model.room_database.data_classes.UserData

@Database(entities = [RoomCalendarData::class, UserData::class, RoomEventData::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calendarItemDao(): CalendarItemDao
    abstract fun sharedUsersDao(): SharedUsersDao
    abstract fun eventItemDao(): EventItemDao
}