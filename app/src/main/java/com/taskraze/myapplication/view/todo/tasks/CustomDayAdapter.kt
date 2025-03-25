package com.taskraze.myapplication.view.todo.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.todo.daily.DailyFragment
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.viewmodel.MainViewModel

class CustomDayAdapter(private val containingFragment: DailyFragment, val activity: AppCompatActivity, data: List<TaskData>) : RecyclerView.Adapter<CustomDayAdapter.DayItemViewHolder>() {
    inner class DayItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }

    private val dataList = data.toMutableList()
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
            val taskData = TaskData(viewHolder.viewHolderId, viewHolder.titleTextView.text.toString(), viewHolder.descriptionTextView.text.toString(), viewHolder.timeTextView.text.toString(), viewHolder.checkBox.isChecked)
            containingFragment.startUpdateTask(taskData)
        }
        viewHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isChecked = isChecked
        }
    }
    override fun getItemCount() = dataList.size

    fun insertItem(newTask: TaskData) {
        dataList.add(newTask)
    }

    fun updateItem(task: TaskData) {
        val index = dataList.indexOfFirst { it.taskId == task.taskId }
        if (index >= 0) {
            dataList[index] = task
        }
    }

    fun removeItem(taskId: Int) {
        val index = dataList.indexOfFirst { it.taskId == taskId }
        if (index >= 0) {
            dataList.removeAt(index)
        }
    }

}
