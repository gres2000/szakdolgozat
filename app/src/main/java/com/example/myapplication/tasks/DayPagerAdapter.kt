package com.example.myapplication.tasks

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class DayPagerAdapter( fragmentPiece:Fragment) :
    FragmentStateAdapter(fragmentPiece) {

    override fun getItemCount(): Int = 7 // Number of days

    override fun createFragment(position: Int): Fragment = DayFragment()

}