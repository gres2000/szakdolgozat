package com.example.myapplication.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class DayFragment : Fragment() {
    /*
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
*/
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.day_fragment, container, false)
        recyclerView = view.findViewById(R.id.dayRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val taskFragment1 = TaskFragment()
        val taskFragment2 = TaskFragment()
        val taskFragment3 = TaskFragment()


        var dataList = listOf(taskFragment1, taskFragment2, taskFragment3)

        adapter = CustomDayAdapter(dataList)
        recyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fetch data for the specified day using arguments.getInt(ARG_DAY_INDEX)

//        val taskFragment1 = TaskFragment()
//        val taskFragment2 = TaskFragment()
//        val taskFragment3 = TaskFragment()
//
//        var dataList = listOf(taskFragment1, taskFragment2, taskFragment3)

        //adapter = CustomDayAdapter(dataList)

//        recyclerView.adapter = adapter

    }
}