package com.taskraze.myapplication.view.calendar

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import androidx.viewpager2.adapter.FragmentStateAdapter

class CalendarsViewPagerAdapter(fragmentActivity: FragmentActivity, private val fragmentList: List<Fragment>) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getItemCount(): Int {
        return 2
    }
}