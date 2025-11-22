package com.taskraze.myapplication.model.calendar

data class UserCalendarsData(
    val userId: String,
    val calendars: MutableList<CalendarData>
) {
    // Default constructor
    constructor() : this("", mutableListOf())
}
