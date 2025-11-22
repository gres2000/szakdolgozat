package com.taskraze.myapplication.viewmodel.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirestoreViewModel : ViewModel() {
    private val repository = FirestoreCalendarRepository()

    // Observable calendars list from Firestore
    private val _calendars = MutableStateFlow<List<CalendarData>>(emptyList())
    val calendars: StateFlow<List<CalendarData>> = _calendars

    fun loadCalendars() {
        viewModelScope.launch {
            val list = repository.getAllCalendars()
            _calendars.value = list
        }
    }

    fun addCalendar(calendar: CalendarData) {
        viewModelScope.launch {
            repository.addCalendar(calendar)
            loadCalendars() // refresh
        }
    }

    fun updateCalendar(calendar: CalendarData) {
        viewModelScope.launch {
            repository.updateCalendar(calendar)
            loadCalendars()
        }
    }

    fun deleteCalendar(calendar: CalendarData) {
        viewModelScope.launch {
            repository.removeCalendar(calendar.id)
            loadCalendars()
        }
    }

    fun addUserToCalendar(user: UserData, calendarId: Long) {
        viewModelScope.launch {
            repository.addSharedUserToCalendar(user, calendarId)
            loadCalendars()
        }
    }

    fun removeUserFromCalendar(userId: String, calendarId: Long) {
        viewModelScope.launch {
            repository.removeSharedUserFromCalendar(userId, calendarId)
            loadCalendars()
        }
    }
}
