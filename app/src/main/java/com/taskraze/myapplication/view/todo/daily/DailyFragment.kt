package com.taskraze.myapplication.view.todo.daily

import AuthViewModelFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.todo.tasks.CustomTaskAdapter
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.databinding.DailyFragmentBinding
import com.taskraze.myapplication.view.todo.tasks.TaskDetailFragment
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.todo.TaskViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class DailyFragment : Fragment() {

    companion object {
        private const val ARG_DAY = "day"

        fun newDailyFragment(day: String, dayId: Int): DailyFragment {
            val fragment = DailyFragment()
            val args = Bundle()
            args.putString(ARG_DAY, day)
            fragment.arguments = args
            fragment.setDayId(dayId)
            return fragment
        }
    }

    private var dayId = -1
    private var _binding: DailyFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var authViewModel: AuthViewModel

    enum class Mode { DAILY, WEEKLY }

    private lateinit var adapter: CustomTaskAdapter
    private val tasks = mutableListOf<TaskData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DailyFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskViewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]
        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]

        taskViewModel.userId = authViewModel.getUserId()
        taskViewModel.loadTasks()

        val recyclerView = binding.dayRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        val mode = if (dayId == -1) Mode.DAILY else Mode.WEEKLY
        adapter = CustomTaskAdapter(this, requireActivity() as AppCompatActivity, mode, tasks)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            if (dayId == -1) {
                taskViewModel.dailyTasksList.collect { newList ->
                    tasks.clear()
                    tasks.addAll(newList)
                    adapter.notifyDataSetChanged()
                }
            } else {
                taskViewModel.weeklyTasksList.collect { newWeekly ->
                    val newList = newWeekly[dayId]
                    tasks.clear()
                    tasks.addAll(newList)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        setupAddButton()
    }

    private fun setupAddButton() {
        binding.fabAdd.setOnClickListener {
            val fragment = TaskDetailFragment.newCreateTask()
            fragment.listener = object : TaskDetailFragment.TaskDetailListener {
                override fun onNewTaskCreated(task: TaskData) {
                    if (dayId == -1) taskViewModel.addDailyTask(task)
                    else taskViewModel.addWeeklyTask(task, dayId)
                }

                override fun onEditTask(task: TaskData) {
                    if (dayId == -1) taskViewModel.updateDailyTask(task)
                    else taskViewModel.updateWeeklyTask(task, dayId)
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.constraint_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    fun setDayId(id: Int) { dayId = id }
    fun getDayId(): Int = dayId

    fun startUpdateTask(task: TaskData) {
        val fragment = TaskDetailFragment.newUpdateTask(task)
        fragment.listener = object : TaskDetailFragment.TaskDetailListener {
            override fun onNewTaskCreated(task: TaskData) {
                if (dayId == -1) taskViewModel.addDailyTask(task)
                else taskViewModel.addWeeklyTask(task, dayId)
            }

            override fun onEditTask(task: TaskData) {
                if (dayId == -1) taskViewModel.updateDailyTask(task)
                else taskViewModel.updateWeeklyTask(task, dayId)
            }
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.constraint_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
