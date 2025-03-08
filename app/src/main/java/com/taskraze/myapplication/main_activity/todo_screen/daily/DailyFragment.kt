package com.taskraze.myapplication.main_activity.todo_screen.daily

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
import com.taskraze.myapplication.main_activity.todo_screen.tasks.CustomDayAdapter
import com.taskraze.myapplication.main_activity.todo_screen.tasks.TaskData
import com.taskraze.myapplication.view_model.MainViewModel
import com.taskraze.myapplication.databinding.DailyFragmentBinding
import com.taskraze.myapplication.main_activity.todo_screen.tasks.NewTaskActivity

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
    private lateinit var taskLauncher: ActivityResultLauncher<Intent>
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

        // set dataList according to dayId
        val dataList: Any?
        if (dayId == -1) {
            dataList = viewModel.dailyTasksList
        }
        else {
            dataList = viewModel.weeklyTasksList[dayId]
        }

        val adapter = CustomDayAdapter(this, requireActivity() as AppCompatActivity, dataList)
        recyclerView.adapter = adapter

        // register the ActivityResultLauncher
        registerActivityResultLauncher(dataList, adapter)

        setupAddButton()
    }

    private fun setupAddButton() {
        val addButton = binding.fabAdd
        addButton.setOnClickListener {
            val intent = Intent(requireContext(), NewTaskActivity::class.java)
            taskLauncher.launch(intent)
        }
    }

    private fun registerActivityResultLauncher(
        dataList: MutableList<TaskData>,
        adapter: CustomDayAdapter
    ) {
        taskLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val newTask = TaskData(
                        0,
                        data.getStringExtra("title") ?: "",
                        data.getStringExtra("description") ?: "",
                        data.getStringExtra("time") ?: "",
                        data.getBooleanExtra("isChecked", false) ?: false
                    )

                    dataList.add(newTask)
                    adapter.notifyItemInserted(dataList.size - 1)
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
}
