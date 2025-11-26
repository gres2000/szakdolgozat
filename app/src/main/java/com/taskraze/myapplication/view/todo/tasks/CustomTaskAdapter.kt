package com.taskraze.myapplication.view.todo.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.todo.daily.DailyFragment
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.viewmodel.todo.TaskViewModel
import kotlinx.coroutines.launch

class CustomTaskAdapter(
    private val containingFragment: DailyFragment,
    val activity: AppCompatActivity,
    data: List<TaskData>,
    private val mode: DailyFragment.Mode,
    private val dayId: Int? = null
) : RecyclerView.Adapter<CustomTaskAdapter.TaskItemViewHolder>() {
    inner class TaskItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.check_box)
        val deleteButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        var viewHolderId: String = ""
        lateinit var taskViewModel: TaskViewModel
    }

    private val dataList = data.toMutableList()
    private val viewModel = ViewModelProvider(activity)[TaskViewModel::class.java]
    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): TaskItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.task_item_view, view, false)

        return TaskItemViewHolder(itemView)
    }
    override fun onBindViewHolder(viewHolder: TaskItemViewHolder, position: Int) {
        val currentItem = dataList[position]
        viewHolder.viewHolderId = currentItem.taskId
        viewHolder.titleTextView.text = currentItem.title
        viewHolder.descriptionTextView.text = currentItem.description
        viewHolder.timeTextView.text = currentItem.time
        viewHolder.checkBox.isChecked = currentItem.isChecked
        viewHolder.taskViewModel = ViewModelProvider(activity)[TaskViewModel::class.java]

        viewHolder.itemView.setOnClickListener {
            val taskData = TaskData(viewHolder.viewHolderId, viewHolder.titleTextView.text.toString(), viewHolder.descriptionTextView.text.toString(), viewHolder.timeTextView.text.toString(), viewHolder.checkBox.isChecked, currentItem.notificationMinutesBefore)
            containingFragment.startUpdateTask(taskData)
        }
        viewHolder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            currentItem.isChecked = isChecked
            viewModel.toggleChecked(currentItem.taskId, isChecked, mode, containingFragment.getDayId())
        }

        viewHolder.deleteButton.setOnClickListener {
            showDeleteDialog(currentItem.taskId)
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

    private fun showDeleteDialog(taskId: String) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        builder.setView(dialogView)

        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val buttonDelete = dialogView.findViewById<Button>(R.id.button_delete)

        val dialog = builder.create()

        buttonCancel.setOnClickListener { dialog.dismiss() }

        buttonDelete.setOnClickListener {
            val index = dataList.indexOfFirst { it.taskId == taskId }
            if (index < 0) {
                dialog.dismiss()
                return@setOnClickListener
            }

            dataList.removeAt(index)
            notifyItemRemoved(index)

            viewModel.viewModelScope.launch {
                viewModel.removeTask(taskId, mode, containingFragment.getDayId())
            }

            dialog.dismiss()
        }

        dialog.show()
    }


}
