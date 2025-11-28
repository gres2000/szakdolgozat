package com.taskraze.myapplication.view.todo.tasks

import AuthViewModelFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.taskraze.myapplication.R
import com.taskraze.myapplication.common.UserPreferences
import com.taskraze.myapplication.databinding.TaskDetailFragmentBinding
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.viewmodel.NotificationViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModel
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModelFactory
import kotlinx.coroutines.launch
import java.util.UUID

class TaskDetailFragment : Fragment() {

    interface TaskDetailListener {
        fun onNewTaskCreated(task: TaskData)
        fun onEditTask(task: TaskData)
    }

    private lateinit var binding: TaskDetailFragmentBinding
    private lateinit var notificationSwitch: SwitchCompat
    private lateinit var notificationSpinner: Spinner
    private lateinit var tagLayout: LinearLayout
    private lateinit var tagSpinner: Spinner
    lateinit var listener: TaskDetailListener
    lateinit var taskToEdit: TaskData
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var recommendationsViewModel: RecommendationsViewModel
    private lateinit var authViewModel: AuthViewModel

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
        super.onViewCreated(view, savedInstanceState)
        binding.timePicker.setIs24HourView(true)

        notificationViewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]

        recommendationsViewModel = ViewModelProvider(
            this,
            RecommendationsViewModelFactory(authViewModel.getUserId())
        )[RecommendationsViewModel::class.java]

        notificationSwitch = binding.switchNotification
        notificationSpinner = binding.spinnerNotificationTime

        val times = listOf("5 min", "10 min", "15 min", "30 min", "1 hour")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, times)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notificationSpinner.adapter = adapter

        tagLayout = binding.tagLayout
        tagSpinner = binding.tagSpinner

        val tags = listOf(
            "Work", "Personal", "Health", "Fitness", "Entertainment",
            "Education", "Chores", "Shopping", "Finance", "Social"
        )

        val tagAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tags)
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tagSpinner.adapter = tagAdapter

        val tagEnabled = UserPreferences.isRecommendationsEnabled(requireContext())
        tagLayout.visibility = if (tagEnabled) View.VISIBLE else View.GONE

        prefillTaskIfEditing()

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            notificationSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            if (binding.editTextTitle.text.toString().trim().isEmpty()) {
                binding.editTextTitle.error = "Title cannot be empty"
                binding.editTextTitle.requestFocus()
                return@setOnClickListener
            }

            val hour = binding.timePicker.hour
            val minute = binding.timePicker.minute
            val timeString = String.format("%02d:%02d", hour, minute)

            val notificationMinutes = if (notificationSwitch.isChecked) {
                when (notificationSpinner.selectedItem.toString()) {
                    "5 min" -> 5
                    "10 min" -> 10
                    "15 min" -> 15
                    "30 min" -> 30
                    "1 hour" -> 60
                    else -> null
                }
            } else {
                if (::taskToEdit.isInitialized) {
                    notificationViewModel.cancelTaskNotification(requireContext(), taskToEdit)
                    taskToEdit.notificationMinutesBefore = null
                }
                null
            }

            if (::taskToEdit.isInitialized) {
                taskToEdit.title = binding.editTextTitle.text.toString()
                taskToEdit.description = binding.editTextDescription.text.toString()
                taskToEdit.time = timeString
                taskToEdit.notificationMinutesBefore = notificationMinutes

                // save tag
                if (tagEnabled) {
                    val selectedTag = tagSpinner.selectedItem.toString()
                    recommendationsViewModel.saveTag(taskToEdit.taskId, selectedTag,  1)
                }

                listener.onEditTask(taskToEdit)

                if (notificationMinutes != null) {
                    notificationViewModel.scheduleTaskNotification(requireContext(), taskToEdit)
                }

            } else {
                val newTask = TaskData(
                    taskId = UUID.randomUUID().toString(),
                    title = binding.editTextTitle.text.toString(),
                    description = binding.editTextDescription.text.toString(),
                    time = timeString,
                    isChecked = false,
                    notificationMinutesBefore = notificationMinutes
                )
                listener.onNewTaskCreated(newTask)

                // save tag
                if (tagEnabled) {
                    val selectedTag = tagSpinner.selectedItem.toString()
                    recommendationsViewModel.saveTag(newTask.taskId, selectedTag,  1)
                }

                if (notificationMinutes != null) {
                    notificationViewModel.scheduleTaskNotification(requireContext(), newTask)
                }
            }
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun prefillTaskIfEditing() {
        if (!::taskToEdit.isInitialized) {
            return
        }
        Log.d("NotificationERROR", "Prefilling task for editing: $taskToEdit")

        binding.editTextTitle.setText(taskToEdit.title)
        binding.editTextDescription.setText(taskToEdit.description)

        val timeParts = taskToEdit.time?.split(":")
        binding.timePicker.hour = timeParts?.getOrNull(0)?.toInt() ?: 0
        binding.timePicker.minute = timeParts?.getOrNull(1)?.toInt() ?: 0

        if (taskToEdit.notificationMinutesBefore != null) {
            notificationSwitch.isChecked = true
            notificationSpinner.visibility = View.VISIBLE
            val index = when (taskToEdit.notificationMinutesBefore) {
                5 -> 0
                10 -> 1
                15 -> 2
                30 -> 3
                60 -> 4
                else -> 0
            }
            notificationSpinner.setSelection(index)
        } else {
            notificationSwitch.isChecked = false
            notificationSpinner.visibility = View.GONE
        }

        if (UserPreferences.isRecommendationsEnabled(requireContext())) {
            viewLifecycleOwner.lifecycleScope.launch {
                val tagName = recommendationsViewModel.getTagForId(taskToEdit.taskId)
                tagName?.let {
                    val index = (tagSpinner.adapter as ArrayAdapter<String>).getPosition(it)
                    if (index >= 0) tagSpinner.setSelection(index)
                }
            }
        }
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        val animId = if (enter) R.anim.fold_down else R.anim.fold_up
        return AnimationUtils.loadAnimation(requireContext(), animId)
    }
}
