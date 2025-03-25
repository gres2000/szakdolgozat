package com.taskraze.myapplication.view.todo.daily

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskraze.myapplication.view.todo.tasks.CustomDayAdapter
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.databinding.DailyFragmentBinding
import com.taskraze.myapplication.view.todo.tasks.NewTaskActivity
import com.taskraze.myapplication.viewmodel.todo.TaskViewModel

class DailyFragment : Fragment() {

    companion object {
        private const val ARG_DAY = "day"

        fun newInstance(day: String, dayId: Int): DailyFragment {
            val fragment = DailyFragment()
            val args = Bundle()
            args.putString(ARG_DAY, day)
            fragment.arguments = args
            fragment.setDayId(dayId)
            return fragment
        }
    }
    private var dayId = -1
    private lateinit var addNewTaskLauncher: ActivityResultLauncher<Intent>
    private lateinit var updateTaskLauncher: ActivityResultLauncher<Intent>
    private var _binding: DailyFragmentBinding? = null
    private lateinit var viewModel: TaskViewModel

    enum class Mode {
        DAILY,
        WEEKLY
    }
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

        viewModel = ViewModelProvider(requireActivity())[TaskViewModel::class.java]

        val recyclerView = binding.dayRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // load tasks from local storage
        viewModel.loadTasks()

        // set dataList according to dayId
        var dataList: MutableList<TaskData>
        val adapter: Any?
        val mode: Mode
        if (dayId == -1) {
            dataList = viewModel.dailyTasksList.value?.toMutableList()!!
            adapter = CustomDayAdapter(this, requireActivity() as AppCompatActivity, dataList!!)
            mode = Mode.DAILY

            viewModel.dailyTasksList.observe(viewLifecycleOwner) { newList ->
                val ind = findNewElementIndex(dataList, newList)
                if (ind != null && dataList.size != newList.size) {
                    adapter.insertItem(newList[ind])
                    adapter.notifyItemInserted(ind)
                    dataList.add(newList[ind])
                }
                else if (ind != null) {
                    adapter.updateItem(newList[ind])
                    adapter.notifyItemChanged(ind)
                }
            }
        }
        else {
            dataList = viewModel.weeklyTasksList.value?.get(dayId)?.toMutableList()!!
            adapter = CustomDayAdapter(this, requireActivity() as AppCompatActivity, dataList)
            mode = Mode.WEEKLY

            viewModel.weeklyTasksList.observe(viewLifecycleOwner) { newList ->
                val ind = findNewElementIndex(dataList, newList[dayId])
                if (ind != null && dataList.size != newList[dayId].size) {
                    adapter.insertItem(newList[dayId][ind])
                    adapter.notifyItemInserted(ind)
                    dataList.add(newList[dayId][ind])
                }
                else if (ind != null) {
                    adapter.updateItem(newList[dayId][ind])
                    adapter.notifyItemChanged(ind)

                }
            }
        }

        recyclerView.adapter = adapter

        // register the ActivityResultLauncher
        setupAddNewTaskLauncher(dataList, mode)
        setupUpdateTaskLauncher(mode)

        setupAddButton()
    }

    private fun findNewElementIndex(oldList: List<TaskData>, newList: List<TaskData>): Int? {
        return newList.indexOfFirst { it !in oldList }.takeIf { it >= 0 }
    }


    private fun setupAddButton() {
        val addButton = binding.fabAdd
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), NewTaskActivity::class.java)
            intent.putExtra("update", false)
            addNewTaskLauncher.launch(intent)
        }
    }

    private fun setupAddNewTaskLauncher(
        dataList: List<TaskData>,
        mode: Mode
    ) {
        addNewTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val newTask = TaskData(
                        dataList.size,
                        data.getStringExtra("title") ?: "",
                        data.getStringExtra("description") ?: "",
                        data.getStringExtra("time") ?: "",
                        data.getBooleanExtra("isChecked", false) ?: false
                    )

                    if (mode == Mode.DAILY) {
                        viewModel.addDailyTask(newTask)
                    }
                    else {
                        viewModel.addWeeklyTask(newTask, dayId)
                    }
                }
            }
        }
    }

    private fun setupUpdateTaskLauncher(
        mode: Mode
    ) {
        updateTaskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val newTask = TaskData(
                        data.getIntExtra("taskId", 0),
                        data.getStringExtra("title") ?: "",
                        data.getStringExtra("description") ?: "",
                        data.getStringExtra("time") ?: "",
                        data.getBooleanExtra("isChecked", false) ?: false
                    )

                    if (mode == Mode.DAILY) {
                        viewModel.updateDailyTask(newTask)
                    }
                    else {
                        viewModel.updateWeeklyTask(newTask, dayId)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setDayId(id: Int) {
        dayId = id
    }

    fun getDayId(): Int  {
        return dayId
    }

    fun startUpdateTask(task: TaskData) {
        val intent = Intent(requireContext(), NewTaskActivity::class.java)
        intent.putExtra("update", true)
        intent.putExtra("taskId", task.taskId)
        intent.putExtra("title", task.title)
        intent.putExtra("description", task.description)
        intent.putExtra("time", task.time)
        updateTaskLauncher.launch(intent)
    }
}
