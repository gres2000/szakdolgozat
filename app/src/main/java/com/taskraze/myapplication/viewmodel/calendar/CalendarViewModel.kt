package com.taskraze.myapplication.viewmodel.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.calendar.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CalendarViewModel : ViewModel() {
    private val repository = CalendarRepository()
    private val _calendars = MutableStateFlow<List<CalendarData>>(emptyList())
    private val _sharedCalendars = MutableStateFlow<List<CalendarData>>(emptyList())
    private val _events = MutableStateFlow<List<EventData>>(emptyList())
    val calendars: StateFlow<List<CalendarData>> = _calendars
    val sharedCalendars: StateFlow<List<CalendarData>> = _sharedCalendars
    val events: StateFlow<List<EventData>> = _events

    fun loadCalendars() {
        viewModelScope.launch {
            val list = repository.getOwnCalendars()
            _calendars.value = list
        }
    }

    fun loadSharedCalendars() {
        viewModelScope.launch {
            val list = repository.getSharedCalendars()
            _sharedCalendars.value = list
        }
    }

    fun loadAllEvents() {
        viewModelScope.launch {
            val own = repository.getOwnCalendars()
            val shared = repository.getSharedCalendars()

            val allCalendars = own + shared
            val allEvents = allCalendars.flatMap { it.events }

            _events.value = allEvents
        }
    }

    fun addCalendar(calendar: CalendarData) {
        viewModelScope.launch {
            repository.addCalendar(calendar)
            loadCalendars()
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
            loadSharedCalendars()
        }
    }

    fun removeUserFromCalendar(userId: String, calendarId: Long) {
        viewModelScope.launch {
            repository.removeSharedUserFromCalendar(userId, calendarId)
            loadCalendars()
        }
    }
}
