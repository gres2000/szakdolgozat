package com.taskraze.myapplication.model.calendar


import java.util.Date
import java.util.UUID

data class EventData(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var description: String? = null,
    var startTime: Date,
    var endTime: Date,
    var location: String? = null,
    var wholeDayEvent: Boolean
) {
    constructor() : this("", "", null, Date(), Date(), null, false)
}