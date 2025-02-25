package com.example.myapplication.app.main_activity.todo_screen.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.app.view_model.MainViewModel

class DayDetailFragment(private val taskData: TaskData) : Fragment() {
    private lateinit var viewModel: MainViewModel

    companion object {
        fun newInstance(position: Int, taskData: TaskData): DayDetailFragment {
            val fragment = DayDetailFragment(taskData)
            val args = Bundle()
            args.putInt("position", position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.day_detail_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        view.findViewById<EditText>(R.id.editTextTitle).setText(taskData.title)
        view.findViewById<EditText>(R.id.editTextDescription).setText(taskData.description)

        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val task = TaskData(
                viewModel.taskId,
                view.findViewById<EditText>(R.id.editTextTitle).text.toString(),
                view.findViewById<EditText>(R.id.editTextDescription).text.toString(),
                "${view.findViewById<TimePicker>(R.id.timePicker).hour}:${view.findViewById<TimePicker>(R.id.timePicker).minute}",
                false
            )
            viewModel.taskDataStorage = task
            viewModel.toggleTaskReady()
            viewModel.dayId.value = viewModel.dayId.value
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setNewTaskFalse()
    }

}