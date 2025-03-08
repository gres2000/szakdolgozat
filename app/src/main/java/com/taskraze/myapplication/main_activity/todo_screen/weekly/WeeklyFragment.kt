package com.taskraze.myapplication.main_activity.todo_screen.weekly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.taskraze.myapplication.databinding.WeeklyFragmentBinding

class WeeklyFragment : Fragment() {

    private var _binding: WeeklyFragmentBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: DayPagerAdapter
    private val daysOfWeek = arrayOf("M", "TU", "W", "TH", "F", "SA", "SU")

    //    private lateinit var pagerAdapter: DayPagerAdapter
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = WeeklyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        pagerAdapter = DayPagerAdapter(this)

        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 1

        // attach tablayout to viewpager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = daysOfWeek[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}