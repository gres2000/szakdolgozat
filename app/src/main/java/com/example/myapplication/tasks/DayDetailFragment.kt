package com.example.myapplication.tasks

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
import com.example.myapplication.viewModel.MainViewModel

class DayDetailFragment(private val task: Task) : Fragment() {
    private lateinit var viewModel: MainViewModel

    companion object {
        fun newInstance(position: Int, task: Task): DayDetailFragment {
            val fragment = DayDetailFragment(task)
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

        view.findViewById<EditText>(R.id.editTextTitle).setText(task.title)
        view.findViewById<EditText>(R.id.editTextDescription).setText(task.description)

        view.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        view.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val task = Task(
                viewModel.taskId,
                view.findViewById<EditText>(R.id.editTextTitle).text.toString(),
                view.findViewById<EditText>(R.id.editTextDescription).text.toString(),
                "${view.findViewById<TimePicker>(R.id.timePicker).hour}:${view.findViewById<TimePicker>(R.id.timePicker).minute}",
                false
            )
            viewModel.taskStorage = task
            viewModel.toggleTaskReady()
            viewModel.dayId.value = viewModel.dayId.value
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.setNewTaskFalse()
    }

}