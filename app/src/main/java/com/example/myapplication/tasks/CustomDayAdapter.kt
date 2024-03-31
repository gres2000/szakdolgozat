package com.example.myapplication.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class CustomDayAdapter(private val dataList: List<Task>) : RecyclerView.Adapter<CustomDayAdapter.DayItemViewHolder>() {
    inner class DayItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val doneButton: Button = itemView.findViewById(R.id.doneButton)
    }
    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): DayItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.day_item_view, view, false)
        return DayItemViewHolder(itemView)
    }
    override fun onBindViewHolder(viewHolder: DayItemViewHolder, position: Int) {
        val currentItem = dataList[position]

        viewHolder.titleTextView.text = currentItem.title
        viewHolder.descriptionTextView.text = currentItem.description

        viewHolder.doneButton.setOnClickListener {
            dataList[position].title = "ajjajj"
            viewHolder.titleTextView.text = dataList[position].title
        }
    }
    override fun getItemCount() = dataList.size

}
