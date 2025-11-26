package com.taskraze.myapplication.view.todo.daily

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
import com.taskraze.myapplication.viewmodel.todo.TaskViewModel
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

    enum class Mode { DAILY, WEEKLY }

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

        val recyclerView = binding.dayRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        taskViewModel.loadTasks()

        val dataList: MutableList<TaskData>
        val adapter: CustomTaskAdapter
        if (dayId == -1) {
            dataList = taskViewModel.dailyTasksList.value.toMutableList()
            adapter = CustomTaskAdapter(this, requireActivity() as AppCompatActivity, dataList, Mode.DAILY)

            viewLifecycleOwner.lifecycleScope.launch {
                taskViewModel.dailyTasksList.collect { newList ->
                    val ind = findNewElementIndex(dataList, newList)
                    if (ind != null && dataList.size != newList.size) {
                        adapter.insertItem(newList[ind])
                        adapter.notifyItemInserted(ind)
                        dataList.add(newList[ind])
                    } else if (ind != null) {
                        adapter.updateItem(newList[ind])
                        adapter.notifyItemChanged(ind)
                    }
                }
            }
        } else {
            dataList = taskViewModel.weeklyTasksList.value[dayId].toMutableList()
            adapter = CustomTaskAdapter(this, requireActivity() as AppCompatActivity, dataList, Mode.WEEKLY, dayId)

            viewLifecycleOwner.lifecycleScope.launch {
                taskViewModel.weeklyTasksList.collect { newList ->
                    val ind = findNewElementIndex(dataList, newList[dayId])
                    if (ind != null && dataList.size != newList[dayId].size) {
                        adapter.insertItem(newList[dayId][ind])
                        adapter.notifyItemInserted(ind)
                        dataList.add(newList[dayId][ind])
                    } else if (ind != null) {
                        adapter.updateItem(newList[dayId][ind])
                        adapter.notifyItemChanged(ind)
                    }
                }
            }
        }

        recyclerView.adapter = adapter

        setupAddButton()
    }

    private fun findNewElementIndex(oldList: List<TaskData>, newList: List<TaskData>): Int? {
        return newList.indexOfFirst { it !in oldList }.takeIf { it >= 0 }
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
