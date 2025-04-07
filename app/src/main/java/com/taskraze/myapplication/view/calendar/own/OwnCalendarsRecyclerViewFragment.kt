package com.taskraze.myapplication.view.calendar.own

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
import com.taskraze.myapplication.model.room_database.data_classes.User
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.databinding.OwnCalendarsRecyclerViewBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import com.taskraze.myapplication.model.calendar.LocalCalendarRepository
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class OwnCalendarsRecyclerViewFragment : Fragment(), CalendarDialogFragment.CalendarDialogListener {
    private var _binding: OwnCalendarsRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var addNewCalendar: FloatingActionButton
    private lateinit var saveCalendars: FloatingActionButton
    private val authRepository = AuthRepository()
    private val localCalendarRepository = LocalCalendarRepository()
    private val firestoreCalendarRepository = FirestoreCalendarRepository(localCalendarRepository)


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
                    localCalendarRepository.getAllCalendarsLocal(requireContext())
                )
                binding.ownCalendarsRecyclerView.adapter = adapter
                binding.ownCalendarsRecyclerView.adapter?.notifyDataSetChanged()
                //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
            }
        }

        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
            firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(requireContext())

            if (this@OwnCalendarsRecyclerViewFragment.isAdded) {
                val adapter = CustomOwnCalendarAdapter(
                    requireActivity() as AppCompatActivity,
                    localCalendarRepository.getAllCalendarsLocal(requireContext())
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
                // authRepository.fetchUserDetails()
                firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(
                    requireContext()
                )
                if (this@OwnCalendarsRecyclerViewFragment.isAdded) {
                    val adapter = CustomOwnCalendarAdapter(
                        requireActivity() as AppCompatActivity,
                        localCalendarRepository.getAllCalendarsLocal(requireContext())
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
        val owner = User(AuthViewModel.loggedInUser!!.username, AuthViewModel.loggedInUser!!.email)
        val eventList: MutableList<EventData> = mutableListOf()

        val cal = CalendarData(-1, name, 0, userList, owner, eventList, date)
        MainViewModel.viewModelScope.launch {

            localCalendarRepository.addOrUpdateCalendarLocal(requireContext(), cal)

            (binding.ownCalendarsRecyclerView.adapter as CustomOwnCalendarAdapter).updateData(localCalendarRepository.getAllCalendarsLocal(requireContext()))
        }
    }
}