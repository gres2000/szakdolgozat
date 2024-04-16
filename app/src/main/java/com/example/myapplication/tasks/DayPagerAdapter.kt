package com.example.myapplication.tasks

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DayPagerAdapter( fragmentPiece:Fragment) :
    FragmentStateAdapter(fragmentPiece) {

    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        val dayFragment = DayFragment()
        return dayFragment
    }


}