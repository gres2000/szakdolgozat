package com.taskraze.myapplication.view.calendar.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.R
import com.taskraze.myapplication.calendar.own_calendars.CustomSharedCalendarAdapter
import com.taskraze.myapplication.databinding.SharedCalendarsRecyclerViewBinding
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.taskraze.myapplication.model.auth.AuthRepository
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import com.taskraze.myapplication.model.calendar.LocalCalendarRepository
import kotlinx.coroutines.launch

class SharedCalendarsRecyclerViewFragment : Fragment() {
    private var _binding: SharedCalendarsRecyclerViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var saveCalendars: FloatingActionButton
    private val authRepository = AuthRepository()
    private val localCalendarRepository = LocalCalendarRepository()
    private val firestoreCalendarRepository = FirestoreCalendarRepository(localCalendarRepository)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SharedCalendarsRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saveCalendars = view.findViewById(R.id.fab_save_calendars)

        binding.sharedCalendarsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
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

        lifecycleScope.launch {
            // authRepository.fetchUserDetails()
            firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(requireContext())

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

        saveCalendars.setOnClickListener{
            MainViewModel.viewModelScope.launch {
                // authRepository.fetchUserDetails()
                firestoreCalendarRepository.getAllCalendarsFromFirestoreDB(
                    requireContext()
                )
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