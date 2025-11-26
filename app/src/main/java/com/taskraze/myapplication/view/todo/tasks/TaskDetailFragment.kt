package com.taskraze.myapplication.view.todo.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.taskraze.myapplication.R
import com.taskraze.myapplication.databinding.TaskDetailFragmentBinding
import com.taskraze.myapplication.model.todo.TaskData
import java.util.UUID

class TaskDetailFragment : Fragment() {

    interface TaskDetailListener {
        fun onNewTaskCreated(task: TaskData)
        fun onEditTask(task: TaskData)
    }

    private lateinit var binding: TaskDetailFragmentBinding
    lateinit var listener: TaskDetailListener
    lateinit var taskToEdit: TaskData
    private var isEditing = false

    companion object {
        fun newCreateTask() = TaskDetailFragment()

        fun newUpdateTask(task: TaskData) = TaskDetailFragment().apply {
            this.taskToEdit = task
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TaskDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.timePicker.setIs24HourView(true)

        prefillTaskIfEditing()

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val timeString = String.format("%02d:%02d", hour, minute)

            if (isEditing) {
                taskToEdit.title = binding.editTextTitle.text.toString()
                taskToEdit.description = binding.editTextDescription.text.toString()
                taskToEdit.time = timeString
                listener.onEditTask(taskToEdit)
            } else {
                val newTask = TaskData(
                    taskId = UUID.randomUUID().toString(),
                    title = binding.editTextTitle.text.toString(),
                    description = binding.editTextDescription.text.toString(),
                    time = timeString,
                    isChecked = false
                )
                listener.onNewTaskCreated(newTask)
            }

            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun prefillTaskIfEditing() {
        if (!::taskToEdit.isInitialized) {
            isEditing = false
            return
        }

        isEditing = true
        binding.editTextTitle.setText(taskToEdit.title)
        binding.editTextDescription.setText(taskToEdit.description)

        val timeParts = taskToEdit.time?.split(":")
        binding.timePicker.hour = timeParts?.getOrNull(0)?.toInt() ?: 0
        binding.timePicker.minute = timeParts?.getOrNull(1)?.toInt() ?: 0
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val animId = if (enter) R.anim.fold_down else R.anim.fold_up
        return AnimationUtils.loadAnimation(requireContext(), animId)
    }
}
