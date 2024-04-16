package com.example.myapplication.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.tasks.Task
import com.example.myapplication.viewModel.MainViewModel
import org.w3c.dom.Text

class CustomCalendarAdapter(val activity: AppCompatActivity, private val dataList: List<Task>) : RecyclerView.Adapter<CustomCalendarAdapter.CalendarItemViewHolder>() {
    inner class CalendarItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewCalendarItemNumber)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): CalendarItemViewHolder {

        val itemView = LayoutInflater.from(view.context).inflate(R.layout.calendar_item_view, view, false)

        return CalendarItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: CalendarItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]

    }

    override fun getItemCount() = dataList.size
}