package com.example.myapplication.app.local_database

import androidx.room.TypeConverter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.Date


class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromJsonUser(json: String): UserData {
        val type = object : TypeToken<UserData>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun toJsonUser(user: UserData): String {
        return Gson().toJson(user)
    }



}