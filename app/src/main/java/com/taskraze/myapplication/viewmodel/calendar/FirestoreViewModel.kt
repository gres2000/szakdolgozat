package com.taskraze.myapplication.viewmodel.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FirestoreViewModel : ViewModel() {
    private val repository = FirestoreCalendarRepository()

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

//    fun loadAllEvents() {
//        viewModelScope.launch {
//            val own = repository.getOwnCalendars()
//            val shared = repository.getSharedCalendars()
//
//            val allCalendars = own + shared
//
//            val allEvents = mutableListOf<EventData>()
//            allCalendars.forEach { cal ->
//                cal.events.let { eventList ->
//                    allEvents.addAll(eventList)
//                }
//            }
//
//            _events.value = allEvents
//        }
//    }
fun loadAllEvents() {
    viewModelScope.launch {
        Log.d("NotificationMINE", "Loading all events...")

        val own = repository.getOwnCalendars()
        Log.d("NotificationMINE", "Own calendars loaded: ${own.size}")

        val shared = repository.getSharedCalendars()
        Log.d("NotificationMINE", "Shared calendars loaded: ${shared.size}")

        val allCalendars = own + shared
        val allEvents = allCalendars.flatMap { it.events }

        Log.d("NotificationMINE", "Total events found: ${allEvents.size}")

        _events.value = allEvents
        Log.d("NotificationMINE", "Events StateFlow updated")
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
