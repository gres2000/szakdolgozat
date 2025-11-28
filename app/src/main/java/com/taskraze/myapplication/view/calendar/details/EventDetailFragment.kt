package com.taskraze.myapplication.view.calendar.details

import AuthViewModelFactory
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.taskraze.myapplication.R
import com.taskraze.myapplication.common.UserPreferences
import com.taskraze.myapplication.databinding.EventDetailFragmentBinding
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.NotificationViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModel
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class EventDetailFragment : Fragment() {
    interface EventDetailListener {
        fun onNewEventCreated(event: EventData)
        fun onEditEvent(event: EventData)
    }
    private lateinit var binding: EventDetailFragmentBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var recommendationsViewModel: RecommendationsViewModel
    private lateinit var authViewModel: AuthViewModel
    private lateinit var dateUntilTextView: TextView
    private lateinit var dateFromTextView: TextView
    private lateinit var hourPickerFrom: NumberPicker
    private lateinit var minutePickerFrom: NumberPicker
    private lateinit var hourPickerUntil: NumberPicker
    private lateinit var minutePickerUntil: NumberPicker
    private lateinit var wholeDaySwitch: SwitchCompat
    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var notificationSpinner: Spinner
    private lateinit var tagLayout: LinearLayout
    private lateinit var tagSpinner: Spinner
    lateinit var listener: EventDetailListener
    private lateinit var eventTitle: TextView
    private lateinit var eventDescription: TextView
    lateinit var eventToEdit: EventData
    lateinit var calendar: CalendarData
    var startingDay: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EventDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]

        recommendationsViewModel = ViewModelProvider(
            this,
            RecommendationsViewModelFactory(authViewModel.getUserId())
        )[RecommendationsViewModel::class.java]

        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        notificationViewModel = ViewModelProvider(requireActivity())[NotificationViewModel::class.java]

        wholeDaySwitch = binding.switchButtonEventDetail
        eventTitle = binding.editTextTitleEventDetail
        eventDescription = binding.editTextDescriptionEventDetail

        dateUntilTextView = binding.dateUntilTextView
        dateFromTextView = binding.dateFromTextView
        //timer from

        hourPickerFrom = binding.hourPickerFrom
        minutePickerFrom = binding.minutePickerFrom
        hourPickerFrom.minValue = 0
        hourPickerFrom.maxValue = 23

        minutePickerFrom.minValue = 0
        minutePickerFrom.maxValue = 59

        minutePickerFrom.setFormatter { String.format("%02d", it) }

        //timer until
        hourPickerUntil = binding.hourPickerUntil
        minutePickerUntil = binding.minutePickerUntil

        hourPickerUntil.minValue = 0
        hourPickerUntil.maxValue = 23

        minutePickerUntil.minValue = 0
        minutePickerUntil.maxValue = 59

        minutePickerUntil.setFormatter { String.format("%02d", it) }
        startingDay?.let { day ->
            updateDateInView(day, dateFromTextView)
            updateDateInView(day, dateUntilTextView)
        }
        hourPickerFrom.value = LocalDateTime.now().hour
        hourPickerUntil.value = LocalDateTime.now().hour + 1

        hourPickerFrom.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE
        minutePickerFrom.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE

        hourPickerUntil.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE
        minutePickerUntil.visibility = if (wholeDaySwitch.isChecked) View.GONE else View.VISIBLE

        // notification components
        notificationSwitch = binding.switchNotification
        notificationSpinner = binding.spinnerNotificationTime

        val times = listOf("5 min", "10 min", "15 min", "30 min", "1 hour")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, times)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notificationSpinner.adapter = adapter

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        tagLayout = binding.tagLayout
        tagSpinner = binding.tagSpinner

        val tags = listOf(
            "Work", "Personal", "Health", "Fitness", "Entertainment",
            "Education", "Chores", "Shopping", "Finance", "Social"
        )

        val tagAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tags)
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tagSpinner.adapter = tagAdapter

        val tagEnabled = UserPreferences.isRecommendationsEnabled(requireContext())
        tagLayout.visibility = if (tagEnabled) View.VISIBLE else View.GONE

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
            val minCalendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            minCalendar.time = dateFormat.parse(dateFromTextView.text.toString())!!

            showDatePickerDialog(dateUntilTextView, minCalendar)
        }

        dateFromTextView.setOnClickListener {
            showDatePickerDialog(dateFromTextView)
        }

        binding.cancelButtonEventDetail.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.saveButtonEventDetail.setOnClickListener {

            if (eventTitle.text.toString().trim().isEmpty()) {
                eventTitle.error = "Title cannot be empty"
                eventTitle.requestFocus()
                return@setOnClickListener
            }


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

                // save tag
                if (tagEnabled) {
                    val selectedTag = tagSpinner.selectedItem.toString()
                    recommendationsViewModel.saveTag(eventToEdit.eventId, selectedTag,  1)
                }

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

                // save tag
                if (tagEnabled) {
                    val selectedTag = tagSpinner.selectedItem.toString()
                    recommendationsViewModel.saveTag(newEvent.eventId, selectedTag,  1)
                }

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

    private fun showDatePickerDialog(dateTextView: TextView, minDate: Calendar? = null) {
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

        minDate?.let {
            datePickerDialog.datePicker.minDate = it.timeInMillis
        }

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

        if (UserPreferences.isRecommendationsEnabled(requireContext())) {
            viewLifecycleOwner.lifecycleScope.launch {
                val tagName = recommendationsViewModel.getTagForId(eventToEdit.eventId)
                tagName?.let {
                    val index = (tagSpinner.adapter as ArrayAdapter<String>).getPosition(it)
                    if (index >= 0) tagSpinner.setSelection(index)
                }
            }
        }
    }

}