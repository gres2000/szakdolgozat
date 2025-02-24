package com.example.myapplication.app.main_activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.app.main_activity.todo_screen.TodoFragment
import com.example.myapplication.app.main_activity.home_screen.HomeFragment
import com.example.myapplication.app.main_activity.calendar_screen.CalendarFragment
import com.example.myapplication.app.main_activity.todo_screen.details.DayDetailFragment
import com.example.myapplication.app.view_model.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private val fragmentManager = supportFragmentManager
    private val fragmentMap = mutableMapOf<String, Fragment>()
    private lateinit var viewModel: MainViewModel
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]


        lifecycleScope.launch {
            viewModel.authenticateUser()
        }
        setupFragments()
        setupBottomNavigationView()

        setupOnBackPressedHandler()

        MainViewModel.someEvent.observe(this) { eventData ->
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.constraint_container, DayDetailFragment(eventData))
            transaction.commit()
        }

        MainViewModel.dayId.observe(this) { eventData ->
            if (viewModel.taskReady) {
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.constraint_container, fragmentMap["tasks"]!!)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }
        binding.bottomNavigationView.isSaveEnabled = false

        binding.bottomNavigationView.selectedItemId = R.id.destination_home
        fragmentManager.beginTransaction()
            .replace(R.id.constraint_container, fragmentMap["home"]!!)
            .commit()

        //overlay widget
//        overlayPermissionLauncher = registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) { result ->
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (Settings.canDrawOverlays(this)) {
//                    // Permission granted
//                    createOverlayWidget()
//                }
//            }
//        }
//
//        checkOverlayPermission()

    }

//    private fun checkOverlayPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                val intent = Intent(
//                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:$packageName")
//                )
//                overlayPermissionLauncher.launch(intent)
//            } else {
//                // Permission is already granted
//                createOverlayWidget()
//            }
//        } else {
//            // System is less than Marshmallow
//            createOverlayWidget()
//        }
//    }

//    private fun createOverlayWidget() {
//        val intent = Intent(this, OverlayService::class.java)
//        startForegroundService(intent)
//    }



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
        val middleFragmentId = R.id.destination_home // Replace with your actual middle fragment ID

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragmentId = binding.bottomNavigationView.selectedItemId
                if (currentFragmentId != middleFragmentId && supportFragmentManager.backStackEntryCount == 0) {
                    fragmentManager.beginTransaction()
                        .replace(R.id.constraint_container, fragmentMap["home"]!!)
                        .commit()
                    binding.bottomNavigationView.selectedItemId = R.id.destination_home
                }
                else if (currentFragmentId == middleFragmentId) {
                    this@MainActivity.finish()
                }
                else {
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