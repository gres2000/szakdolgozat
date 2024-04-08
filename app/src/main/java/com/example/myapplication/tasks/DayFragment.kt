package com.example.myapplication.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.LeftFragment

class DayFragment(private val daysOfWeek: Array<String>) : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomDayAdapter
    private lateinit var dataList: MutableList<Task>
    private val dayString: String

    companion object {
        private var instantiationCount = 0
    }
    init {
        dayString = daysOfWeek[instantiationCount % daysOfWeek.size]
        instantiationCount++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val task1 = Task("első", "séta", "14:58")
        val task2 = Task("második", "munka", "14:58")
        val task3 = Task("harmadik", "eső", "14:58")

        dataList = mutableListOf(task1, task2, task3)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.day_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.dayRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)


        adapter = CustomDayAdapter(requireActivity() as AppCompatActivity, dataList)
        recyclerView.adapter = adapter

        view.findViewById<TextView>(R.id.textViewCurrentDay).text = dayString

    }
}