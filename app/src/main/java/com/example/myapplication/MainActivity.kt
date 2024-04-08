package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.fragments.LeftFragment
import com.example.myapplication.fragments.MidFragment
import com.example.myapplication.fragments.RightFragment
import com.example.myapplication.tasks.DetailFragment
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView


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

        viewModel.someEvent.observe(this, Observer { eventData ->
            // React to changes in the LiveData
            // eventData is the value emitted by the LiveData
            // Update UI or perform any necessary action
            val transaction = fragmentManager.beginTransaction()
            transaction.replace(R.id.constraint_container, DetailFragment(eventData))
            transaction.addToBackStack(null)
            transaction.commit()
        })

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
        val tasksFragment = LeftFragment()
        val homeFragment = MidFragment()
        val calendarFragment = RightFragment()

        fragmentMap["tasks"] = tasksFragment
        fragmentMap["home"] = homeFragment
        fragmentMap["calendar"] = calendarFragment
    }
}