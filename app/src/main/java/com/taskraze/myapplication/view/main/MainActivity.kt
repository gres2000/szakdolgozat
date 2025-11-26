package com.taskraze.myapplication.view.main

import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.ActivityMainBinding
import com.taskraze.myapplication.view.todo.TodoFragment
import com.taskraze.myapplication.view.home.HomeFragment
import com.taskraze.myapplication.view.calendar.CalendarFragment
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.NotificationViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.calendar.CalendarViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentManager = supportFragmentManager
    private val fragmentMap = mutableMapOf<String, Fragment>()
    private lateinit var viewModel: MainViewModel
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        lifecycleScope.launch {
            authViewModel.awaitUserId()
            calendarViewModel.loadAllEvents()
            calendarViewModel.loadCalendars()
            calendarViewModel.loadSharedCalendars()

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                calendarViewModel.events.collect { eventList ->
                    if (eventList.isNotEmpty()) {
                        notificationViewModel.scheduleAllNotifications(this@MainActivity, eventList)

                        Log.d("NotificationMINE", "Scheduled all events")
                    }
                }
            }
        }


        setupFragments()
        setupBottomNavigationView()

        setupOnBackPressedHandler()

        binding.bottomNavigationView.isSaveEnabled = false

        binding.bottomNavigationView.selectedItemId = R.id.destination_home
        fragmentManager.beginTransaction()
            .replace(R.id.constraint_container, fragmentMap["home"]!!)
            .commit()
    }



    private fun setupBottomNavigationView() {

        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.destination_tasks -> {
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["tasks"]!!)
                        .commit()
                    true
                }

                R.id.destination_home -> {
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["home"]!!)
                        .commit()
                    true
                }

                R.id.destination_calendar -> {
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["calendar"]!!)
                        .commit()

                    true
                }

                else -> false
            }
        }
    }

    private fun setupOnBackPressedHandler() {
        val middleFragmentId = R.id.destination_home

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragmentId = binding.bottomNavigationView.selectedItemId
                if (currentFragmentId != middleFragmentId && supportFragmentManager.backStackEntryCount == 0) {
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["home"]!!)
                        .commit()
                    binding.bottomNavigationView.selectedItemId = R.id.destination_home
                } else if (currentFragmentId == middleFragmentId) {
                    this@MainActivity.finish()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    private fun setupFragments() {
        val homeFragment = HomeFragment()
        fragmentMap["home"] = homeFragment
        CoroutineScope(Dispatchers.Default).launch {
            val tasksFragmentDeferred = async { TodoFragment() }
            val calendarFragmentDeferred = async { CalendarFragment() }

            val tasksFragment = tasksFragmentDeferred.await()
            val calendarFragment = calendarFragmentDeferred.await()

            withContext(Dispatchers.Main) {
                fragmentMap["tasks"] = tasksFragment
                fragmentMap["calendar"] = calendarFragment
            }
        }
    }
}