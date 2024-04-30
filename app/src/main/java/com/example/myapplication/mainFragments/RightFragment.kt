package com.example.myapplication.mainFragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.myapplication.R
import com.example.myapplication.authentication.User
import com.example.myapplication.calendar.Calendar
import com.example.myapplication.calendar.CustomCalendarAdapter
import com.example.myapplication.calendar.Event
import com.example.myapplication.databinding.RightFragmentBinding
import com.example.myapplication.local_database_room.AppDatabase
import com.example.myapplication.local_database_room.CalendarData
import com.example.myapplication.local_database_room.EventData
import com.example.myapplication.local_database_room.UserData
import com.example.myapplication.tasks.CustomDayAdapter
import com.example.myapplication.tasks.Task
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class RightFragment : Fragment() {
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
        return inflater.inflate(R.layout.right_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        calendarsRecyclerView = view.findViewById(R.id.recyclerViewCalendars)
        calendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        addNewCalendar = view.findViewById(R.id.fab_add_calendar)
        saveCalendars = view.findViewById(R.id.fab_save_calendars)

        viewModel.viewModelScope.launch {
//            val calendarList = viewModel.getAllCalendars(requireContext())
            adapter = CustomCalendarAdapter(requireActivity() as AppCompatActivity, viewModel.getAllCalendars(requireContext()) )
            calendarsRecyclerView.adapter = adapter
        }

        addNewCalendar.setOnClickListener{
            viewModel.viewModelScope.launch {
                val date = Date.from(LocalDate.now().atStartOfDay(
                    ZoneId.systemDefault()).toInstant())
                val userList = mutableListOf<User>()
                val owner = User(viewModel.loggedInUser!!.username, viewModel.loggedInUser!!.emailAddress)
                val eventList = mutableListOf(Event("elsőevent", "nincs description", date, date, "még nincs"))

                val cal = Calendar("first", 0, userList, owner, eventList, date)
                viewModel.addCalendar(requireContext(), cal)

                adapter.updateData(viewModel.getAllCalendars(requireContext()))
            }
        }

        saveCalendars.setOnClickListener{
            viewModel.viewModelScope.launch {
                viewModel.saveAllCalendarsToFirestoreDB(requireContext(), viewModel.auth.currentUser!!.email.toString())
            }
        }





        /*val roomDB = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "database-name"
        ).build()
        val datenow = Date.from(LocalDate.now().atStartOfDay(
            ZoneId.systemDefault()).toInstant())

        val calendarsDao = roomDB.calendarItemDao()
        runBlocking {
            calendarsDao.insertCalendarItem(CalendarData(
                0, "csop", 4,  mutableListOf(UserData("a@a.com", "a")), "a@a.com",
                mutableListOf(EventData("futás", "nincs", datenow, datenow, "itt")), datenow))
        }

        runBlocking {
            val calendars = withContext(Dispatchers.IO) {
                calendarsDao.getAll()
            }
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}