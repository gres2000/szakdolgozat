package com.taskraze.myapplication.view.todo

import AuthViewModelFactory
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.main.MainActivity
import com.taskraze.myapplication.view.todo.daily.DailyFragment
import com.taskraze.myapplication.view.todo.weekly.WeeklyFragment
import com.taskraze.myapplication.databinding.TodoFragmentBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel

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
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]


        switchButton = binding.taskSwitch
        val dailyEditText = binding.dailyLabel
        val weeklyEditText = binding.weeklyLabel

        val sharedPref = (requireActivity() as MainActivity).getSharedPreferences(authViewModel.getUserId(), MODE_PRIVATE)
        switchButton.isChecked = sharedPref.getBoolean("switch_state", false)

        switchFragment(switchButton.isChecked)
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
            .replace(R.id.todo_fragment_container, fragment)
            .commit()
    }
}
