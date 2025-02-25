package com.example.myapplication.app.main_activity.calendar_screen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.app.main_activity.calendar_screen.calendar.CalendarsViewPagerAdapter
import com.example.myapplication.app.main_activity.calendar_screen.calendar.own_calendars.OwnCalendarsRecyclerViewFragment
import com.example.myapplication.app.main_activity.calendar_screen.calendar.shared_calendars.SharedCalendarsRecyclerViewFragment
import com.example.myapplication.app.view_model.MainViewModel
import com.example.myapplication.databinding.CalendarFragmentBinding
import com.google.android.material.tabs.TabLayoutMediator

class CalendarFragment : Fragment() {
    private var _binding: CalendarFragmentBinding? = null

    private lateinit var viewModel: MainViewModel
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CalendarFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val fragmentList = listOf(OwnCalendarsRecyclerViewFragment(), SharedCalendarsRecyclerViewFragment())
        val viewPager: ViewPager2 = binding.calendarsViewPager
        val viewPagerAdapter = CalendarsViewPagerAdapter(requireActivity(), fragmentList)
        viewPager.adapter = viewPagerAdapter

        val tabLayout = binding.tabLayout

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Own"
                1 -> "Shared"
                else -> "Page $position"
            }
        }.attach()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}