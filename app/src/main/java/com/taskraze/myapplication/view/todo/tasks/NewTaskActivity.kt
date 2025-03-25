package com.taskraze.myapplication.view.todo.tasks

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.taskraze.myapplication.databinding.NewTaskActivityBinding
import com.taskraze.myapplication.model.todo.TaskData

class NewTaskActivity : AppCompatActivity() {
    private var taskId = 0
    private lateinit var binding: NewTaskActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = NewTaskActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleEditText = binding.editTextTitle
        val descriptionEditText = binding.editTextDescription
        val timePicker = binding.timePicker
        val cancelButton = binding.cancelButton
        val saveButton = binding.saveButton

        //check if initiated for an update, otherwise it's a new task
        if (intent.getBooleanExtra("update", false)) {
            taskId = intent.getIntExtra("taskId", 0)
            titleEditText.setText(intent.getStringExtra("title"))
            descriptionEditText.setText(intent.getStringExtra("description"))

            //split time
            val timeString = intent.getStringExtra("time")
            val timeParts = timeString?.split(":")
            timePicker.hour = timeParts?.get(0)?.toInt() ?: 0
            timePicker.minute = timeParts?.get(1)?.toInt() ?: 0
        }

        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val task = TaskData(
                taskId,
                titleEditText.text.toString(),
                descriptionEditText.text.toString(),
                "${timePicker.hour}:${timePicker.minute}",
                false
            )
            val resultIntent = Intent()
            resultIntent.putExtra("title", task.title)
            resultIntent.putExtra("taskId", task.taskId)
            resultIntent.putExtra("description", task.description)
            resultIntent.putExtra("time", task.time)
            resultIntent.putExtra("isChecked", task.isChecked)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
