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
import com.taskraze.myapplication.model.todo.TaskData
import com.taskraze.myapplication.view.todo.daily.DailyFragment
import com.taskraze.myapplication.viewmodel.todo.TaskViewModel
import kotlinx.coroutines.launch

class CustomTaskAdapter(
    private val fragment: DailyFragment,
    private val activity: AppCompatActivity,
    private val mode: DailyFragment.Mode,
    private val tasks: MutableList<TaskData>
) : RecyclerView.Adapter<CustomTaskAdapter.TaskViewHolder>() {

    private val taskViewModel = ViewModelProvider(activity)[TaskViewModel::class.java]

    init {
        setHasStableIds(true)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textViewTitle)
        val description: TextView = itemView.findViewById(R.id.textViewDescription)
        val time: TextView = itemView.findViewById(R.id.textViewTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.check_box)
        val deleteButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
    }

    override fun getItemId(position: Int): Long = tasks[position].taskId.hashCode().toLong()
    override fun getItemCount(): Int = tasks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item_view, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.title.text = task.title
        holder.description.text = task.description
        holder.time.text = task.time

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = task.isChecked
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            task.isChecked = isChecked
            taskViewModel.toggleChecked(task.taskId, isChecked, mode, fragment.getDayId())
        }

        holder.itemView.setOnClickListener {
            fragment.startUpdateTask(task.copy(
                title = holder.title.text.toString(),
                description = holder.description.text.toString(),
                time = holder.time.text.toString(),
                isChecked = holder.checkBox.isChecked
            ))
        }

        holder.deleteButton.setOnClickListener {
            showDeleteDialog(task, position)
        }
    }

    private fun showDeleteDialog(task: TaskData, position: Int) {
        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.delete_dialog, null)
        builder.setView(view)
        val dialog = builder.create()

        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)
        val buttonDelete = view.findViewById<Button>(R.id.button_delete)

        buttonCancel.setOnClickListener { dialog.dismiss() }

        buttonDelete.setOnClickListener {
            tasks.removeAt(position)
            notifyItemRemoved(position)

            taskViewModel.viewModelScope.launch {
                taskViewModel.removeTask(task.taskId, mode, fragment.getDayId())
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}
