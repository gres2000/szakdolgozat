package com.taskraze.myapplication.main_activity.calendar_screen.calendar.calendar_details

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view_model.MainViewModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Locale

class EventDetailFragment : Fragment() {
    interface EventDetailListener {
        fun onNewEventCreated(event: MyEvent)
    }
    private lateinit var viewModel: MainViewModel
    private lateinit var dateUntilTextView: TextView
    private lateinit var dateFromTextView: TextView
    private lateinit var hourPickerFrom: NumberPicker
    private lateinit var minutePickerFrom: NumberPicker
    private lateinit var hourPickerUntil: NumberPicker
    private lateinit var minutePickerUntil: NumberPicker
    private lateinit var wholeDaySwitch: SwitchCompat
    lateinit var listener: EventDetailListener
    private lateinit var eventTitle: TextView
    private lateinit var eventDescrption: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.event_detail_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        wholeDaySwitch = view.findViewById(R.id.switchButtonEventDetail)
        eventTitle = view.findViewById(R.id.editTextTitleEventDetail)
        eventDescrption = view.findViewById(R.id.editTextDescriptionEventDetail)

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

        val foldUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fold_up)
        val foldDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.fold_down)

        wholeDaySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
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
            val newEvent = MyEvent(
                eventTitle.text.toString(),
                eventDescrption.text.toString(),
                dateFrom,
                dateUntil,
                null,
                wholeDaySwitch.isChecked

            )
            listener.onNewEventCreated(newEvent)
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
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

}