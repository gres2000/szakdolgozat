package com.example.myapplication.calendar

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.calendar.EventDetailFragment
import com.example.myapplication.databinding.CalendarDialogFragmentBinding
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CalendarDetailFragment : Fragment() {

    private var _binding: CalendarDialogFragmentBinding? = null
    private lateinit var titleTextView: TextView
    private lateinit var ownerTextView: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var viewModel: MainViewModel
    private lateinit var backButtonImageButton: ImageButton
    private lateinit var addNewEvent: FloatingActionButton
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.calendar_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val newCalendar = viewModel.getCalendarToFragment()
        titleTextView = view.findViewById(R.id.textViewDetailCalendarTitle)
        ownerTextView = view.findViewById(R.id.textViewDetailCalendarOwner)
        calendarView = view.findViewById(R.id.calendarViewCalendarDetail)
        eventsRecyclerView = view.findViewById(R.id.recyclerViewEvents)
        usersRecyclerView = view.findViewById(R.id.recyclerViewUsers)
        backButtonImageButton = view.findViewById(R.id.imageButtonLeftArrow)
        addNewEvent = view.findViewById(R.id.fab_add_event)
        titleTextView.text = newCalendar?.name

        val ownerString = getString(R.string.owner_double_dots) + " " + newCalendar?.owner?.username
        ownerTextView.text = ownerString

        backButtonImageButton.setOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        addNewEvent.setOnClickListener{

            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.constraint_container, EventDetailFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}