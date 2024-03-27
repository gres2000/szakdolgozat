package com.example.myapplication

import android.os.Bundle
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    //private var bottomNavView  = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
    private var navController: NavController? = null
    private val fragmentManager = supportFragmentManager
    private val fragmentMap = mutableMapOf<String, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //navController = findNavController(R.id.nav_graph)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupFragments()
        setupBottomNavigationView()

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