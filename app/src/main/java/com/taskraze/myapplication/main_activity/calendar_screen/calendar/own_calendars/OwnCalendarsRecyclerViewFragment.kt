package com.taskraze.myapplication.main_activity.calendar_screen.calendar.own_calendars

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.R
import com.taskraze.myapplication.authentication_activity.User
import com.taskraze.myapplication.main_activity.calendar_screen.calendar.calendar_details.MyCalendar
import com.taskraze.myapplication.main_activity.calendar_screen.calendar.calendar_details.MyEvent
import com.taskraze.myapplication.databinding.OwnCalendarsRecyclerViewBinding
import com.taskraze.myapplication.view_model.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class OwnCalendarsRecyclerViewFragment : Fragment(), CalendarDialogFragment.CalendarDialogListener {
    private var _binding: OwnCalendarsRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var addNewCalendar: FloatingActionButton
    private lateinit var saveCalendars: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = OwnCalendarsRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addNewCalendar = view.findViewById(R.id.fab_add_calendar)
        saveCalendars = view.findViewById(R.id.fab_save_calendars)

        binding.ownCalendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            if (this@OwnCalendarsRecyclerViewFragment.isAdded) {
                val adapter = CustomOwnCalendarAdapter(
                    requireActivity() as AppCompatActivity,
                    MainViewModel.getAllCalendars(requireContext())
                )
                binding.ownCalendarsRecyclerView.adapter = adapter
                binding.ownCalendarsRecyclerView.adapter?.notifyDataSetChanged()
                //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
            }
        }

        lifecycleScope.launch {
            MainViewModel.authenticateUser()
            MainViewModel.getAllCalendarsFromFirestoreDB(requireContext())

            if (this@OwnCalendarsRecyclerViewFragment.isAdded) {
                val adapter = CustomOwnCalendarAdapter(
                    requireActivity() as AppCompatActivity,
                    MainViewModel.getAllCalendars(requireContext())
                )

                binding.ownCalendarsRecyclerView.adapter = adapter
                binding.ownCalendarsRecyclerView.adapter?.notifyDataSetChanged()
                //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
            }
        }

        addNewCalendar.setOnClickListener{

            showNewCalendarDialog()

        }

        saveCalendars.setOnClickListener{
            MainViewModel.viewModelScope.launch {
                MainViewModel.authenticateUser()
                MainViewModel.getAllCalendarsFromFirestoreDB(
                    requireContext()
                )
                if (this@OwnCalendarsRecyclerViewFragment.isAdded) {
                    val adapter = CustomOwnCalendarAdapter(
                        requireActivity() as AppCompatActivity,
                        MainViewModel.getAllCalendars(requireContext())
                    )
                    binding.ownCalendarsRecyclerView.adapter = adapter
                    binding.ownCalendarsRecyclerView.adapter?.notifyDataSetChanged()
                    //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
                }
            }
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
        val userList = mutableListOf<User>()
        val owner = User(MainViewModel.loggedInUser!!.username, MainViewModel.loggedInUser!!.email)
        val eventList: MutableList<MyEvent> = mutableListOf()

        val cal = MyCalendar(-1, name, 0, userList, owner, eventList, date)
        MainViewModel.viewModelScope.launch {

            MainViewModel.addCalendar(requireContext(), cal)

            (binding.ownCalendarsRecyclerView.adapter as CustomOwnCalendarAdapter).updateData(MainViewModel.getAllCalendars(requireContext()))
        }
    }
}