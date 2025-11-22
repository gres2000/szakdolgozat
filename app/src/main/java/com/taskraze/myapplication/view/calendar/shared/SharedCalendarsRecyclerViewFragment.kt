package com.taskraze.myapplication.view.calendar.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.SharedCalendarsRecyclerViewBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import com.taskraze.myapplication.view.calendar.own.CustomOwnCalendarAdapter
import com.taskraze.myapplication.viewmodel.calendar.FirestoreViewModel
import kotlinx.coroutines.launch

class SharedCalendarsRecyclerViewFragment : Fragment() {
    private var _binding: SharedCalendarsRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var saveCalendars: FloatingActionButton
    private lateinit var firestoreViewModel: FirestoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SharedCalendarsRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestoreViewModel = ViewModelProvider(requireActivity())[FirestoreViewModel::class.java]

        saveCalendars = view.findViewById(R.id.fab_save_calendars)

        binding.sharedCalendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = CustomSharedCalendarAdapter(requireActivity() as AppCompatActivity, mutableListOf())
        binding.sharedCalendarsRecyclerView.adapter = adapter

        firestoreViewModel.loadSharedCalendars()

        lifecycleScope.launchWhenStarted {
            firestoreViewModel.sharedCalendars.collect { calendarList ->
                Toast.makeText(requireContext(), "Loaded: ${calendarList.size} SHARED calendars", Toast.LENGTH_SHORT).show()
                adapter.updateData(calendarList)
            }
        }
//
//        lifecycleScope.launch {
//            if (this@SharedCalendarsRecyclerViewFragment.isAdded) {
//                binding.sharedCalendarsRecyclerView.adapter?.notifyDataSetChanged()
//                //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
//            }
//        }

//        lifecycleScope.launch {
//
//            if (this@SharedCalendarsRecyclerViewFragment.isAdded) {
//                val adapter = CustomSharedCalendarAdapter(
//                    requireActivity() as AppCompatActivity,
//                    MainViewModel.getSharedCalendars(requireContext())
//                )
//
//                binding.sharedCalendarsRecyclerView.adapter = adapter
//                binding.sharedCalendarsRecyclerView.adapter?.notifyDataSetChanged()
//                //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
//            }
//        }

        saveCalendars.setOnClickListener{
            MainViewModel.viewModelScope.launch {
                if (this@SharedCalendarsRecyclerViewFragment.isAdded) {
                    val adapter = CustomSharedCalendarAdapter(
                        requireActivity() as AppCompatActivity,
                        MainViewModel.getSharedCalendars(requireContext())
                    )
                    binding.sharedCalendarsRecyclerView.adapter = adapter
                    binding.sharedCalendarsRecyclerView.adapter?.notifyDataSetChanged()
                    //MyItemTouchHelperCallback.attachDragAndDrop(adapter, calendarsRecyclerView)
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}