package com.taskraze.myapplication.view.main

import AuthViewModelFactory
import CalendarViewModelFactory
import android.content.Intent
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
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
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


        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(this)
        )[AuthViewModel::class.java]
        calendarViewModel = ViewModelProvider(
            this,
            CalendarViewModelFactory(authViewModel)
        )[CalendarViewModel::class.java]

        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setupFragments()
        setupBottomNavigationView()

        setupOnBackPressedHandler()

        binding.bottomNavigationView.isSaveEnabled = false

        binding.bottomNavigationView.selectedItemId = R.id.destination_home
        fragmentManager.beginTransaction()
            .replace(R.id.constraint_container, fragmentMap["home"]!!)
            .commit()


        handleOverlayIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        loadAllDataAndScheduleNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadAllDataAndScheduleNotifications()
    }

    private fun loadAllDataAndScheduleNotifications() {
        lifecycleScope.launch {
            authViewModel.awaitUserId()
            calendarViewModel.loadCalendars()
            calendarViewModel.loadSharedCalendars()
            calendarViewModel.loadAllEvents()

            calendarViewModel.events.collect { events ->
                if (events.isNotEmpty()) {
                    notificationViewModel.scheduleAllNotifications(this@MainActivity, events)
                }
            }
        }
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
        fragmentMap["home"] = HomeFragment()
        fragmentMap["tasks"] = TodoFragment()
        fragmentMap["calendar"] = CalendarFragment()
    }

    private fun handleOverlayIntent(intent: Intent?) {
        intent?.getStringExtra("navigateTo")?.let { destination ->
            when(destination) {
                "tasks" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.destination_tasks
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["tasks"]!!)
                        .commit()
                }
                "calendar" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.destination_calendar
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["calendar"]!!)
                        .commit()
                }
                "home" -> {
                    binding.bottomNavigationView.selectedItemId = R.id.destination_home
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["home"]!!)
                        .commit()
                }

                else -> {}
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleOverlayIntent(intent)
    }
}