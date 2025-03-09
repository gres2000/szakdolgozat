package com.taskraze.myapplication.main.calendar.calendar.shared_calendars

import com.taskraze.myapplication.main.calendar.calendar.calendar_details.MyCalendar

data class SharedCalendar(
    val email: String = "",
    val userId: String = "",
    val calendars: List<MyCalendar> = emptyList()
)