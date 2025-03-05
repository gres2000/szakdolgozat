package com.taskraze.myapplication.main_activity.todo_screen.tasks

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.taskraze.myapplication.main_activity.todo_screen.daily.DailyFragment

class DayPagerAdapter( fragmentPiece:Fragment) :
    FragmentStateAdapter(fragmentPiece) {

    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        val dayFragment = DailyFragment()
        return dayFragment
    }


}