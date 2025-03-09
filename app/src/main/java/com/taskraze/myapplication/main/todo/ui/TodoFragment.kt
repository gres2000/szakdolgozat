package com.taskraze.myapplication.main.todo.ui

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.taskraze.myapplication.R
import com.taskraze.myapplication.main.MainActivity
import com.taskraze.myapplication.main.todo.ui.daily.DailyFragment
import com.taskraze.myapplication.main.todo.ui.weekly.WeeklyFragment
import com.taskraze.myapplication.databinding.TodoFragmentBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.taskraze.myapplication.view_model.MainViewModel

class TodoFragment : Fragment() {

    private var _binding: TodoFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var switchButton: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TodoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSwitch()
    }

    private fun setupSwitch() {

        switchButton = binding.taskSwitch
        val dailyEditText = binding.dailyLabel
        val weeklyEditText = binding.weeklyLabel

        // get saved switch state
        val sharedPref = (requireActivity() as MainActivity).getSharedPreferences(MainViewModel.loggedInUser.email, MODE_PRIVATE)
        switchButton.isChecked = sharedPref.getBoolean("switch_state", false)

        switchFragment(switchButton.isChecked)
        // set highlight
        if (switchButton.isChecked) {
            weeklyEditText.setBackgroundResource(R.drawable.highlight_text_background)
            dailyEditText.background = null
        } else {
            weeklyEditText.background = null
            dailyEditText.setBackgroundResource(R.drawable.highlight_text_background)
        }

        switchButton.setOnCheckedChangeListener { _, isChecked ->
            switchFragment(isChecked)
            if (isChecked) {
                weeklyEditText.setBackgroundResource(R.drawable.highlight_text_background)
                dailyEditText.background = null
            } else {
                weeklyEditText.background = null
                dailyEditText.setBackgroundResource(R.drawable.highlight_text_background)
            }
            sharedPref.edit().putBoolean("switch_state", switchButton.isChecked).apply()
        }
    }

    private fun switchFragment(showWeekly: Boolean) {
        val fragment = if (showWeekly) WeeklyFragment() else DailyFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.todoFragmentContainer, fragment)
            .commit()
    }

}
