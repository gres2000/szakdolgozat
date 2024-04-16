package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.mainFragments.LeftFragment
import com.example.myapplication.mainFragments.MidFragment
import com.example.myapplication.mainFragments.RightFragment
import com.example.myapplication.tasks.DetailFragment
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        setupFragments()
        setupBottomNavigationView()

        viewModel.someEvent.observe(this) { eventData ->
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.constraint_container, DetailFragment(eventData))
            transaction.addToBackStack(null)
            transaction.commit()
        }

        viewModel.dayId.observe(this) { eventData ->
            if (viewModel.taskReady) {
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.constraint_container, fragmentMap["tasks"]!!)
                transaction.commit()
            }
        }

    }

    private fun setupBottomNavigationView() {
        val bottomNavView  = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavView.menu.findItem(R.id.destination_home).isChecked = true


        bottomNavView.setOnItemSelectedListener { menuItem ->
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

    private fun setupFragments() {
        CoroutineScope(Dispatchers.Default).launch {
            val tasksFragmentDeferred = async { LeftFragment() }
            val calendarFragmentDeferred = async { RightFragment() }
            val homeFragmentDeferred = async { MidFragment() }

            val tasksFragment = tasksFragmentDeferred.await()
            val calendarFragment = calendarFragmentDeferred.await()
            val homeFragment = homeFragmentDeferred.await()

            withContext(Dispatchers.Main) {
                fragmentMap["tasks"] = tasksFragment
                fragmentMap["calendar"] = calendarFragment
                fragmentMap["home"] = homeFragment
            }
        }
    }

}