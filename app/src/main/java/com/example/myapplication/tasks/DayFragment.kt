package com.example.myapplication.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R

class DayFragment : Fragment() {
    companion object {
        private const val ARG_DAY_INDEX = "dayIndex"

        fun newInstance(dayIndex: Int): DayFragment {
            val fragment = DayFragment()
            val args = Bundle()
            args.putInt(ARG_DAY_INDEX, dayIndex)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.day_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fetch data for the specified day using arguments.getInt(ARG_DAY_INDEX)
    }
}