package com.taskraze.myapplication.view.todo.weekly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.WeeklyFragmentBinding

class WeeklyFragment : Fragment() {

    private var _binding: WeeklyFragmentBinding? = null
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pagerAdapter: DayPagerAdapter
    private val daysOfWeek: MutableList<String> = mutableListOf()
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
        val list = listOf(
            getString(R.string.capitalMonday),
            getString(R.string.capitalTuesday),
            getString(R.string.capitalWednesday),
            getString(R.string.capitalThursday),
            getString(R.string.capitalFriday),
            getString(R.string.capitalSaturday),
            getString(R.string.capitalSunday)
        )
        daysOfWeek.addAll(list)

        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        pagerAdapter = DayPagerAdapter(this)

        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 1

        val currentDayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)

        val adjustedDayOfWeek = if (currentDayOfWeek == java.util.Calendar.SUNDAY) 6 else currentDayOfWeek - java.util.Calendar.MONDAY

        viewPager.setCurrentItem(adjustedDayOfWeek, false)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = daysOfWeek[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}