package com.taskraze.myapplication.view.calendar.details

import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class EventDetailFragment : Fragment() {
    interface EventDetailListener {
        fun onNewEventCreated(event: EventData)
        fun onEditEvent(event: EventData)
    }
    private lateinit var viewModel: MainViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var dateUntilTextView: TextView
    private lateinit var dateFromTextView: TextView
    private lateinit var hourPickerFrom: NumberPicker
    private lateinit var minutePickerFrom: NumberPicker
    private lateinit var hourPickerUntil: NumberPicker
    private lateinit var minutePickerUntil: NumberPicker
    private lateinit var wholeDaySwitch: SwitchCompat
    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var notificationSpinner: Spinner
    lateinit var listener: EventDetailListener
    private lateinit var eventTitle: TextView
    private lateinit var eventDescription: TextView
    lateinit var eventToEdit: EventData
    lateinit var calendar: CalendarData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.event_detail_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        notificationViewModel = ViewModelProvider(requireActivity())[NotificationViewModel::class.java]

        wholeDaySwitch = view.findViewById(R.id.switchButtonEventDetail)
        eventTitle = view.findViewById(R.id.editTextTitleEventDetail)
        eventDescription = view.findViewById(R.id.editTextDescriptionEventDetail)

        dateUntilTextView = view.findViewById(R.id.dateUntilTextView)
        dateFromTextView = view.findViewById(R.id.dateFromTextView)
        //timer from

        hourPickerFrom = view.findViewById(R.id.hourPickerFrom)
        minutePickerFrom = view.findViewById(R.id.minutePickerFrom)
        hourPickerFrom.minValue = 0
        hourPickerFrom.maxValue = 23

        minutePickerFrom.minValue = 0
        minutePickerFrom.maxValue = 59

        minutePickerFrom.setFormatter { String.format("%02d", it) }

        //timer until
        hourPickerUntil = view.findViewById(R.id.hourPickerUntil)
        minutePickerUntil = view.findViewById(R.id.minutePickerUntil)

        hourPickerUntil.minValue = 0
        hourPickerUntil.maxValue = 23

        minutePickerUntil.minValue = 0
        minutePickerUntil.maxValue = 59

        minutePickerUntil.setFormatter { String.format("%02d", it) }
        updateDateInView(viewModel.newEventStartingDay!!, dateUntilTextView)
        updateDateInView(viewModel.newEventStartingDay!!, dateFromTextView)
        hourPickerFrom.value = LocalDateTime.now().hour
        hourPickerUntil.value = LocalDateTime.now().hour + 1

        hourPickerFrom.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE
        minutePickerFrom.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE

        hourPickerUntil.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE
        minutePickerUntil.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE

        // notification components
        notificationSwitch = view.findViewById(R.id.switchNotification)
        notificationSpinner = view.findViewById(R.id.spinnerNotificationTime)

        val times = listOf("5 min", "10 min", "15 min", "30 min", "1 hour")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, times)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notificationSpinner.adapter = adapter

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        prefillEventIfEditing()

        val foldUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fold_up)
        val foldDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fold_down)

        wholeDaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                wholeDaySwitch.isEnabled = false
                hourPickerFrom.startAnimation(foldUpAnimation)
                minutePickerFrom.startAnimation(foldUpAnimation)
                hourPickerUntil.startAnimation(foldUpAnimation)
                minutePickerUntil.startAnimation(foldUpAnimation)

                wholeDaySwitch.postDelayed({
                    hourPickerFrom.visibility = View.GONE
                    minutePickerFrom.visibility = View.GONE
                    hourPickerUntil.visibility = View.GONE
                    minutePickerUntil.visibility = View.GONE
                    wholeDaySwitch.isEnabled = true
                }, foldUpAnimation.duration)
            } else {
                wholeDaySwitch.isEnabled = false
                hourPickerFrom.visibility = View.VISIBLE
                minutePickerFrom.visibility = View.VISIBLE
                hourPickerUntil.visibility = View.VISIBLE
                minutePickerUntil.visibility = View.VISIBLE
                hourPickerFrom.startAnimation(foldDownAnimation)
                minutePickerFrom.startAnimation(foldDownAnimation)
                hourPickerUntil.startAnimation(foldDownAnimation)
                minutePickerUntil.startAnimation(foldDownAnimation)
                wholeDaySwitch.postDelayed({
                    wholeDaySwitch.isEnabled = true
                }, foldDownAnimation.duration)
            }
        }

        dateUntilTextView.setOnClickListener {
            showDatePickerDialog(dateUntilTextView)
        }

        dateFromTextView.setOnClickListener {
            showDatePickerDialog(dateFromTextView)
        }

        view.findViewById<Button>(R.id.cancelButtonEventDetail).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        view.findViewById<Button>(R.id.saveButtonEventDetail).setOnClickListener {
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val dateFrom = dateFormat.parse("${dateFromTextView.text} ${hourPickerFrom.value}:${minutePickerFrom.value}")!!
            val dateUntil = dateFormat.parse("${dateUntilTextView.text} ${hourPickerUntil.value}:${minutePickerUntil.value}")!!

            val notificationMinutes = if (notificationSwitch.isChecked) {
                when (notificationSpinner.selectedItem.toString()) {
                    "5 min" -> 5
                    "10 min" -> 10
                    "15 min" -> 15
                    "30 min" -> 30
                    "1 hour" -> 60
                    else -> null
                }
            } else {
                if (::eventToEdit.isInitialized) {
                    notificationViewModel.cancelEventNotification(requireContext(), eventToEdit)
                    eventToEdit.notificationMinutesBefore = null
                }
                null
            }

            if (::eventToEdit.isInitialized) {
                eventToEdit.title = eventTitle.text.toString()
                eventToEdit.description = eventDescription.text.toString()
                eventToEdit.startTime = dateFrom
                eventToEdit.endTime = dateUntil
                eventToEdit.wholeDayEvent = wholeDaySwitch.isChecked
                eventToEdit.notificationMinutesBefore = notificationMinutes

                listener.onEditEvent(eventToEdit)

                if (notificationMinutes != null) {
                    notificationViewModel.scheduleEventNotification(requireContext(), eventToEdit)
                }
            } else {
                val newEvent = EventData(
                    title = eventTitle.text.toString(),
                    description = eventDescription.text.toString(),
                    startTime = dateFrom,
                    endTime = dateUntil,
                    location = null,
                    wholeDayEvent = wholeDaySwitch.isChecked,
                    notificationMinutesBefore = notificationMinutes
                )
                listener.onNewEventCreated(newEvent)

                if (notificationMinutes != null) {
                    notificationViewModel.scheduleEventNotification(requireContext(), newEvent)
                }
            }
        }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val animId = if (enter) R.anim.fold_down else R.anim.fold_up

        return AnimationUtils.loadAnimation(requireContext(), animId)
    }

    private fun showDatePickerDialog(dateTextView: TextView) {
        val dateStr = dateTextView.text.toString()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val selectedDate = dateFormat.parse(dateStr)
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate!!

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView(calendar, dateTextView)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateInView(calendar: Calendar, dateTextView: TextView) {
        val dateFormat = "yyyy/MM/dd"
        val simpleDateFormat = SimpleDateFormat(dateFormat, Locale.getDefault())
        dateTextView.text = simpleDateFormat.format(calendar.time)
    }

    private fun prefillEventIfEditing() {
        if (!::eventToEdit.isInitialized) return

        eventTitle.text = eventToEdit.title
        eventDescription.text = eventToEdit.description

        val calStart = Calendar.getInstance().apply { time = eventToEdit.startTime }
        val calEnd = Calendar.getInstance().apply { time = eventToEdit.endTime }

        updateDateInView(calStart, dateFromTextView)
        updateDateInView(calEnd, dateUntilTextView)

        hourPickerFrom.value = calStart.get(Calendar.HOUR_OF_DAY)
        minutePickerFrom.value = calStart.get(Calendar.MINUTE)
        hourPickerUntil.value = calEnd.get(Calendar.HOUR_OF_DAY)
        minutePickerUntil.value = calEnd.get(Calendar.MINUTE)

        wholeDaySwitch.isChecked = eventToEdit.wholeDayEvent

        val visibility = if (eventToEdit.wholeDayEvent) View.GONE else View.VISIBLE
        hourPickerFrom.visibility = visibility
        minutePickerFrom.visibility = visibility
        hourPickerUntil.visibility = visibility
        minutePickerUntil.visibility = visibility

        if (eventToEdit.notificationMinutesBefore != null) {
            notificationSwitch.isChecked = true
            notificationSpinner.visibility = View.VISIBLE
            val minutes = eventToEdit.notificationMinutesBefore!!
            val selectedIndex = when (minutes) {
                5 -> 0
                10 -> 1
                15 -> 2
                30 -> 3
                60 -> 4
                else -> 0
            }
            notificationSpinner.setSelection(selectedIndex)
        } else {
            notificationSwitch.isChecked = false
            notificationSpinner.visibility = View.GONE
        }
    }

}