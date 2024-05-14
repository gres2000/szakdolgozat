package com.example.myapplication.mainFragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.authentication.User
import com.example.myapplication.calendar.MyCalendar
import com.example.myapplication.calendar.CustomCalendarAdapter
import com.example.myapplication.calendar.Event
import com.example.myapplication.calendar.CalendarDialogFragment
import com.example.myapplication.databinding.RightFragmentBinding
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class RightFragment : Fragment(), CalendarDialogFragment.CalendarDialogListener {
    private var _binding: RightFragmentBinding? = null

    private lateinit var calendarsRecyclerView: RecyclerView
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: CustomCalendarAdapter
    private lateinit var addNewCalendar: FloatingActionButton
    private lateinit var saveCalendars: FloatingActionButton
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = RightFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]



        calendarsRecyclerView = view.findViewById(R.id.recyclerViewCalendars)
        calendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        addNewCalendar = view.findViewById(R.id.fab_add_calendar)
        saveCalendars = view.findViewById(R.id.fab_save_calendars)

        viewModel.viewModelScope.launch {
            if (this@RightFragment.isAdded) {
                adapter = CustomCalendarAdapter(
                    requireActivity() as AppCompatActivity,
                    viewModel.getAllCalendars(requireContext())
                )
                calendarsRecyclerView.adapter = adapter
            }
        }

        viewModel.viewModelScope.launch {
            viewModel.authenticateUser()
            viewModel.getAllCalendarsFromFirestoreDB(
                requireContext()
            )

            if (this@RightFragment.isAdded) {
                adapter = CustomCalendarAdapter(
                    requireActivity() as AppCompatActivity,
                    viewModel.getAllCalendars(requireContext())
                )

                calendarsRecyclerView.adapter = adapter
                calendarsRecyclerView.adapter?.notifyDataSetChanged()
            }
        }


        addNewCalendar.setOnClickListener{

            showNewCalendarDialog()

        }

        saveCalendars.setOnClickListener{
            viewModel.viewModelScope.launch {
                viewModel.authenticateUser()
                viewModel.getAllCalendarsFromFirestoreDB(
                    requireContext()
                )
                if (this@RightFragment.isAdded) {
                    adapter = CustomCalendarAdapter(
                        requireActivity() as AppCompatActivity,
                        viewModel.getAllCalendars(requireContext())
                    )
                    calendarsRecyclerView.adapter = adapter
                    calendarsRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }




    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNewCalendarDialog() {
        val dialog = CalendarDialogFragment()
        dialog.show(childFragmentManager, "NewCalendarDialogFragment")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewCalendarCreated(name: String) {
        val date = Date.from(LocalDate.now().atStartOfDay(
            ZoneId.systemDefault()).toInstant())
        val userList = mutableListOf<User>()
        val owner = User(viewModel.loggedInUser!!.username, viewModel.loggedInUser!!.email)
        val eventList: MutableList<Event> = mutableListOf()

        val cal = MyCalendar(name, 0, userList, owner, eventList, date)
        viewModel.viewModelScope.launch {

            viewModel.addCalendar(requireContext(), cal)

            adapter.updateData(viewModel.getAllCalendars(requireContext()))
        }
    }
}