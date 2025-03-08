package com.taskraze.myapplication.main_activity.todo_screen.weekly

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.taskraze.myapplication.main_activity.todo_screen.daily.DailyFragment

class DayPagerAdapter(fragmentPiece: Fragment) :
    FragmentStateAdapter(fragmentPiece) {

    private val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun getItemCount(): Int = daysOfWeek.size

    override fun createFragment(position: Int): Fragment {
        return DailyFragment.newInstance(daysOfWeek[position], position)
    }
}