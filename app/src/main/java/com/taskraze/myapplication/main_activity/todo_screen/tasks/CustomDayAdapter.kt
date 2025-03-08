package com.taskraze.myapplication.main_activity.todo_screen.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.main_activity.todo_screen.daily.DailyFragment
import com.taskraze.myapplication.view_model.MainViewModel

class CustomDayAdapter(private val containingFragment: DailyFragment, val activity: AppCompatActivity, private val dataList: List<TaskData>) : RecyclerView.Adapter<CustomDayAdapter.DayItemViewHolder>() {
    inner class DayItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }
    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): DayItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.day_item_view, view, false)

        return DayItemViewHolder(itemView)
    }
    override fun onBindViewHolder(viewHolder: DayItemViewHolder, position: Int) {
        val currentItem = dataList[position]
        viewHolder.viewHolderId = currentItem.taskId
        viewHolder.titleTextView.text = currentItem.title
        viewHolder.descriptionTextView.text = currentItem.description
        viewHolder.timeTextView.text = currentItem.time
        viewHolder.checkBox.isChecked = currentItem.isChecked
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]

        viewHolder.itemView.setOnClickListener {
            var taskData = TaskData(viewHolder.viewHolderId, viewHolder.titleTextView.text.toString(), viewHolder.descriptionTextView.text.toString(), viewHolder.timeTextView.text.toString(), false)
            //new solution
//            viewHolder.viewModel.updateEvent(taskData)
        }
        viewHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isChecked = isChecked
        }
    }
    override fun getItemCount() = dataList.size

}
