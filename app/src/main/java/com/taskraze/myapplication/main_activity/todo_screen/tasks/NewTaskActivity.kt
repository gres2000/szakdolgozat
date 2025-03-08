package com.taskraze.myapplication.main_activity.todo_screen.tasks

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view_model.MainViewModel

class NewTaskActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.day_detail_fragment)

        // Get taskData from Intent

        val titleEditText = findViewById<EditText>(R.id.editTextTitle)
        val descriptionEditText = findViewById<EditText>(R.id.editTextDescription)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val cancelButton = findViewById<Button>(R.id.cancelButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

//        titleEditText.setText(taskData.title)
//        descriptionEditText.setText(taskData.description)

        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val task = TaskData(
                0,
                titleEditText.text.toString(),
                descriptionEditText.text.toString(),
                "${timePicker.hour}:${timePicker.minute}",
                false
            )
            val resultIntent = Intent()
            resultIntent.putExtra("title", task.title)
            resultIntent.putExtra("description", task.description)
            resultIntent.putExtra("time", task.time)
            resultIntent.putExtra("isChecked", task.isChecked)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
