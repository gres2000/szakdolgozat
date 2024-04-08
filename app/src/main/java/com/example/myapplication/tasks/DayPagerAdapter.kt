package com.example.myapplication.tasks

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myapplication.fragments.LeftFragment

class DayPagerAdapter( fragmentPiece:Fragment) :
    FragmentStateAdapter(fragmentPiece) {
        private val daysOfWeak: Array<String> = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    override fun getItemCount(): Int = 7

    override fun createFragment(position: Int): Fragment {
        val dayFragment = DayFragment(daysOfWeak)
        return dayFragment
    }


}