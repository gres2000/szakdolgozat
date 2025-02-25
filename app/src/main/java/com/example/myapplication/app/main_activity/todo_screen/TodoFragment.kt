package com.example.myapplication.app.main_activity.todo_screen

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.R
import com.example.myapplication.app.main_activity.MainActivity
import com.example.myapplication.app.main_activity.todo_screen.daily.DailyFragment
import com.example.myapplication.app.main_activity.todo_screen.tasks.DayPagerAdapter
import com.example.myapplication.app.main_activity.todo_screen.weekly.WeeklyFragment
import com.example.myapplication.databinding.TodoFragmentBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout

class TodoFragment : Fragment() {

    private var _binding: TodoFragmentBinding? = null
    private val binding get() = _binding!!
    lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pagerAdapter: DayPagerAdapter
    private lateinit var tabLayout: TabLayout
    private lateinit var switchButton: SwitchMaterial
    private val daysOfWeak: Array<String> = arrayOf("M", "TU", "W", "TH", "F", "SA", "SU")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TodoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwitch()

//        viewPager = view.findViewById(R.id.viewPagerId)
//        bottomNavigationView = view.findViewById(R.id.tasks_navbar)
//        tabLayout= view.findViewById(R.id.tabLayout)
//        viewPager.isSaveEnabled = false;
//
//
//        pagerAdapter = DayPagerAdapter(this)
//        viewPager.adapter = pagerAdapter
//        viewPager.offscreenPageLimit = pagerAdapter.itemCount - 1 // might need to delete later
//
//        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
//            tab.text = daysOfWeak[position]
//        }.attach()
//
//        bottomNavigationView.setOnItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.action_previous -> {
//                    if (viewPager.currentItem > 0) {
//                        viewPager.currentItem -= 1
//                    }
//                    true
//                }
//                R.id.action_next -> {
//                    if (viewPager.currentItem < pagerAdapter.itemCount - 1) {
//                        viewPager.currentItem += 1
//                    }
//                    true
//                }
//                else -> false
//            }
//        }
    }

    private fun setupSwitch() {

        switchButton = binding.taskSwitch
        val dailyEditText = binding.dailyLabel
        val weeklyEditText = binding.weeklyLabel

        // get saved switch state
        val sharedPref = (requireActivity() as MainActivity).getSharedPreferences("MyPrefs", MODE_PRIVATE)
        switchButton.isChecked = sharedPref.getBoolean("switch_state", false)

        switchFragment(switchButton.isChecked)
        // set highlight
        if (switchButton.isChecked) {
            weeklyEditText.setBackgroundResource(R.drawable.highlight_text_background)
            dailyEditText.background = null
        } else {
            weeklyEditText.background = null
            dailyEditText.setBackgroundResource(R.drawable.highlight_text_background)
        }

        switchButton.setOnCheckedChangeListener { _, isChecked ->
            switchFragment(isChecked)
            if (isChecked) {
                weeklyEditText.setBackgroundResource(R.drawable.highlight_text_background)
                dailyEditText.background = null
            } else {
                weeklyEditText.background = null
                dailyEditText.setBackgroundResource(R.drawable.highlight_text_background)
            }
            sharedPref.edit().putBoolean("switch_state", switchButton.isChecked).apply()
        }
    }

    private fun switchFragment(showWeekly: Boolean) {
        val fragment = if (showWeekly) WeeklyFragment() else DailyFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.todoFragmentContainer, fragment)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        viewPager.adapter = null
    }
}
