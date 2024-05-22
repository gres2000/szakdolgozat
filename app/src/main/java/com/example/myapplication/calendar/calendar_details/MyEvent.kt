package com.example.myapplication.calendar.calendar_details


import java.util.Date

data class MyEvent(
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null,
    var wholeDayEvent: Boolean
) {
    constructor() : this("", null, Date(), Date(), null, false)
}