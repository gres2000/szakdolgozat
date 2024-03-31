package com.example.myapplication.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R
import com.example.myapplication.databinding.LeftFragmentBinding
import com.example.myapplication.tasks.DayFragment
import com.example.myapplication.tasks.DayPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

class LeftFragment : Fragment() {
    private var _binding: LeftFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pagerAdapter: DayPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LeftFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPagerId)
        bottomNavigationView = view.findViewById(R.id.tasks_navbar)
        viewPager.isSaveEnabled = false;


        pagerAdapter = DayPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = pagerAdapter.itemCount - 1

        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_previous -> {
                    if (viewPager.currentItem > 0) {
                        viewPager.currentItem -= 1
                    }
                    true
                }
                R.id.action_next -> {
                    if (viewPager.currentItem < pagerAdapter.itemCount - 1) {
                        viewPager.currentItem += 1
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
    }


    }
