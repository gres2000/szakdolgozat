package com.example.myapplication.calendar

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import android.view.LayoutInflater as LayoutInflater1

class CalendarDialogFragment : DialogFragment() {

    interface CalendarDialogListener {
        fun onNewCalendarCreated(name: String)
    }

    private lateinit var listener: CalendarDialogListener
    private lateinit var nameEditText: EditText

    override fun onCreateView(inflater: LayoutInflater1, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.calendar_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nameEditText = view.findViewById(R.id.editTextNewCalendarTitle)
        val confirmButton = view.findViewById<Button>(R.id.saveButtonCalendar)
        val cancelButton = view.findViewById<Button>(R.id.cancelButtonCalendar)

        confirmButton.setOnClickListener {
            val name = nameEditText.text.toString()
            if (name.isNotEmpty()) {
                listener.onNewCalendarCreated(name)
                dismiss()
            } else {
                nameEditText.error = "Name cannot be empty"
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as CalendarDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$parentFragment must implement CalendarDialogListener")
        }
    }
}