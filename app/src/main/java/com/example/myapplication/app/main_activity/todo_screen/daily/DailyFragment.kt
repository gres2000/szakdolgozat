package com.example.myapplication.app.main_activity.todo_screen.daily

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.app.main_activity.todo_screen.tasks.CustomDayAdapter
import com.example.myapplication.app.main_activity.todo_screen.tasks.TaskData
import com.example.myapplication.app.view_model.MainViewModel
import com.example.myapplication.databinding.DailyFragmentBinding
import com.example.myapplication.databinding.HomeFragmentBinding

class DailyFragment : Fragment() {

    private var _binding: DailyFragmentBinding? = null

    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DailyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val recyclerView = binding.dayRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        val dataList = viewModel.weeklyTasksList[0]

        val adapter = CustomDayAdapter(this, requireActivity() as AppCompatActivity, dataList)
        recyclerView.adapter = adapter

        val addButton = binding.fabAdd
        addButton.setOnClickListener {
            val taskData = TaskData(adapter.itemCount, "", "", "", false)
            viewModel.updateEvent(taskData)
            viewModel.taskId = adapter.itemCount
            viewModel.toggleNewTask()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
