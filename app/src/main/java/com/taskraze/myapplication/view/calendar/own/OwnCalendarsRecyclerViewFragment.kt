package com.taskraze.myapplication.view.calendar.own

import AuthViewModelFactory
import CalendarViewModelFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.databinding.OwnCalendarsRecyclerViewBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.NotificationViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.calendar.CalendarViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID

class OwnCalendarsRecyclerViewFragment : Fragment(), CalendarDialogFragment.CalendarDialogListener {
    private var _binding: OwnCalendarsRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var addNewCalendar: FloatingActionButton
    private lateinit var saveCalendars: FloatingActionButton
    private lateinit var viewModel: MainViewModel
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OwnCalendarsRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        notificationViewModel = ViewModelProvider(requireActivity())[NotificationViewModel::class.java]
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]
        calendarViewModel = ViewModelProvider(
            this,
            CalendarViewModelFactory(authViewModel)
        )[CalendarViewModel::class.java]


        addNewCalendar = view.findViewById(R.id.fab_add_calendar)

        binding.ownCalendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = CustomOwnCalendarAdapter(requireActivity() as AppCompatActivity, mutableListOf())
        binding.ownCalendarsRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                calendarViewModel.loadCalendars()
                calendarViewModel.calendars.collect { calendarList ->
                    Log.d("SCHEDULEDasd", "Events loaded: ${calendarViewModel.events.value}")

                    adapter.updateData(calendarList)
                }
            }
        }

        addNewCalendar.setOnClickListener{
            showNewCalendarDialog()
        }

    }

    private fun showNewCalendarDialog() {
        val dialog = CalendarDialogFragment()
        dialog.show(childFragmentManager, "NewCalendarDialogFragment")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewCalendarCreated(name: String) {
        val date = Date.from(
            LocalDate.now().atStartOfDay(
            ZoneId.systemDefault()).toInstant())
        val userList = mutableListOf<UserData>()
        val owner = UserData("", authViewModel.getUserId(), authViewModel.getUserId())
        val eventList: MutableList<EventData> = mutableListOf()
        val uniqueId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE

        val cal = CalendarData(uniqueId, name, 0, userList, owner, eventList, date)
        viewModel.viewModelScope.launch {
            calendarViewModel.addCalendar(cal)
        }
    }
}