package com.taskraze.myapplication.viewmodel.calendar

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskraze.myapplication.model.calendar.EventData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalendarExportViewModel : ViewModel() {

    private val _exportState = MutableLiveData<String>()
    val exportState: LiveData<String> get() = _exportState

    private val client = OkHttpClient()
    private val dtFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun exportEventsToGoogleCalendar(
        events: List<EventData>,
        accessToken: String,
        calendarId: String = "primary"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                events.forEach { event ->
                    val startZoned = event.startTime.toInstant().atZone(ZoneId.systemDefault())
                    val endZoned = event.endTime.toInstant().atZone(ZoneId.systemDefault())

                    // Ensure endTime > startTime
                    val adjustedEnd = if (!endZoned.isAfter(startZoned)) startZoned.plusMinutes(1) else endZoned

                    val startTime = dtFormatter.format(startZoned)
                    val endTime = dtFormatter.format(adjustedEnd)

                    val json = JSONObject().apply {
                        put("summary", event.title ?: "No title")
                        put("description", event.description ?: "")
                        put("start", JSONObject().apply { put("dateTime", startTime) })
                        put("end", JSONObject().apply { put("dateTime", endTime) })
                        if (!event.location.isNullOrEmpty()) put("location", event.location)
                    }

                    val request = Request.Builder()
                        .url("https://www.googleapis.com/calendar/v3/calendars/$calendarId/events")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .addHeader("Content-Type", "application/json")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string()
                        if (!response.isSuccessful) {
                            throw Exception("Failed to insert event: ${response.code} - $body")
                        } else {
                            Log.d("CalendarExport", "Event inserted successfully: ${event.title}")
                        }
                    }
                }

                _exportState.postValue("success")
            } catch (e: Exception) {
                e.printStackTrace()
                _exportState.postValue("failure")
            }
        }
    }

    fun exportEventsToOutlookGraph(events: List<EventData>, accessToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                events.forEach { event ->

                    val json = JSONObject().apply {
                        put("subject", event.title.takeIf { it.isNotBlank() } ?: "No title")
                        put("body", JSONObject().apply {
                            put("contentType", "HTML")
                            put("content", event.description?.takeIf { it.isNotBlank() } ?: " ")
                        })

                        if (event.wholeDayEvent) {
                            put("isAllDay", true)
                            put("start", JSONObject().apply {
                                put("dateTime", event.startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString())
                                put("timeZone", ZoneId.systemDefault().id)
                            })
                            put("end", JSONObject().apply {
                                put("dateTime", event.endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).toString())
                                put("timeZone", ZoneId.systemDefault().id)
                            })
                        } else {
                            put("start", JSONObject().apply {
                                put("dateTime", event.startTime.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                put("timeZone", ZoneId.systemDefault().id)
                            })
                            put("end", JSONObject().apply {
                                put("dateTime", event.endTime.toInstant().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                put("timeZone", ZoneId.systemDefault().id)
                            })
                        }

                        event.location?.takeIf { it.isNotBlank() }?.let { loc ->
                            put("location", JSONObject().apply { put("displayName", loc) })
                        }
                    }

                    val request = Request.Builder()
                        .url("https://graph.microsoft.com/v1.0/me/events")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .addHeader("Accept", "application/json")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).execute().use { resp ->
                        val body = resp.body?.string()
                        if (resp.isSuccessful) {
                            Log.d("CalendarExport", "Inserted event: ${event.title}, response: $body")
                        } else {
                            Log.e("CalendarExport", "Failed event: ${event.title}, response: $body")
                            // Only throw if you want the whole batch to fail
                            throw Exception("Outlook API failed for event: ${event.title}")
                        }
                    }
                }

                // All events succeeded
                _exportState.postValue("success")
            } catch (e: Exception) {
                e.printStackTrace()
                _exportState.postValue("failure")
            }
        }
    }

}
