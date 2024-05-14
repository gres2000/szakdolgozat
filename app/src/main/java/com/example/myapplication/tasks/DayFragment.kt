package com.example.myapplication.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.DayDetailFragmentBinding
import com.example.myapplication.databinding.DayFragmentBinding
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DayFragment : Fragment() {
    private var _binding: DayFragmentBinding? = null
    var dayFragmentId: Int
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomDayAdapter
    private lateinit var dataList: MutableList<Task>
    private lateinit var addButton: FloatingActionButton
    private lateinit var viewModel: MainViewModel
    private val dayString: String
    private val daysOfWeak: Array<String> =
        arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val binding get() = _binding!!
    companion object {
        private var instantiationCount = 0
    }
    init {
        dayString = daysOfWeak[instantiationCount % daysOfWeak.size]
        dayFragmentId = instantiationCount % 7
        instantiationCount++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DayFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        dataList = viewModel.weeklyTasksList[dayFragmentId]

        recyclerView = view.findViewById(R.id.dayRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = CustomDayAdapter(this, requireActivity() as AppCompatActivity, dataList)
        recyclerView.adapter = adapter

        view.findViewById<TextView>(R.id.textViewCurrentDay).text = dayString

        addButton = view.findViewById(R.id.fab_add)
        addButton.setOnClickListener {
            val task = Task(adapter.itemCount, "", "", "", false)
            viewModel.updateEvent(task)
            viewModel.taskId = adapter.itemCount
            viewModel.dayId.value = dayFragmentId
            viewModel.toggleNewTask()
        }
        viewModel.dayId.observe(viewLifecycleOwner) { eventData ->
            if (viewModel.taskReady && eventData == dayFragmentId && !viewModel.isNewTask) {
                dataList[viewModel.taskId] = viewModel.taskStorage
                viewModel.toggleTaskReady()
            } else if (viewModel.taskReady && eventData == dayFragmentId) {
                dataList.add(viewModel.taskStorage)
                viewModel.toggleTaskReady()
                viewModel.toggleNewTask()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}