package com.example.myapplication.calendar


import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Event(
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null
)  : Parcelable