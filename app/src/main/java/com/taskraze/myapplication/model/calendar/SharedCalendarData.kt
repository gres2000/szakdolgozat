package com.taskraze.myapplication.model.calendar

import com.taskraze.myapplication.model.calendar.CalendarData

data class SharedCalendarData(
    val email: String = "",
    val userId: String = "",
    val calendars: List<CalendarData> = emptyList()
)