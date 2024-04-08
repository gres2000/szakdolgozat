package com.example.myapplication.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.viewModel.MainViewModel

class CustomDayAdapter(private val activity: AppCompatActivity, private val dataList: List<Task>) : RecyclerView.Adapter<CustomDayAdapter.DayItemViewHolder>() {
    inner class DayItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val doneButton: Button = itemView.findViewById(R.id.checkBox)
        lateinit var viewModel: MainViewModel
    }
    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): DayItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.day_item_view, view, false)

        return DayItemViewHolder(itemView)
    }
    override fun onBindViewHolder(viewHolder: DayItemViewHolder, position: Int) {
        val currentItem = dataList[position]

        viewHolder.titleTextView.text = currentItem.title
        viewHolder.descriptionTextView.text = currentItem.description
        viewHolder.timeTextView.text = currentItem.time
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]

        viewHolder.itemView.setOnClickListener {
            var task = Task(viewHolder.titleTextView.text.toString(), viewHolder.descriptionTextView.text.toString(), viewHolder.timeTextView.text.toString())
            //new solution
            viewHolder.viewModel.updateEvent(task)
        }
    }
    override fun getItemCount() = dataList.size

}
