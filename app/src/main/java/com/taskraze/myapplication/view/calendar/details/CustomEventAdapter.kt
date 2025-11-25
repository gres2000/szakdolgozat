package com.taskraze.myapplication.view.calendar.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId

class CustomEventAdapter(
    private val activity: AppCompatActivity,
    private val dataList: MutableList<EventData>,
) : RecyclerView.Adapter<CustomEventAdapter.EventItemViewHolder>() {
    inner class EventItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val startingTimeTextView: TextView = itemView.findViewById(R.id.textViewStartingTime)
        val eventTitleTextView: TextView = itemView.findViewById(R.id.textViewEventTitle)
        val intervalTextView: TextView = itemView.findViewById(R.id.textViewInterval)
        val deleteEventImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDeleteEvent)
        lateinit var viewModel: MainViewModel
    }
    private var onEventActionListener: OnEventActionListener? = null
    private val dateFormat = SimpleDateFormat("HH:mm")

    interface OnEventActionListener {
        fun onItemRemoved(event: EventData)
        fun onItemClicked(event: EventData)
    }
    fun setOnEventActionListener(listener: OnEventActionListener) {
        onEventActionListener = listener
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): EventItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.event_item_view, view, false)
        return EventItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EventItemViewHolder, position: Int) {
        val event = dataList[position]

        holder.startingTimeTextView.text =
            if (!event.wholeDayEvent) dateFormat.format(event.startTime)
            else "--:--"

        holder.eventTitleTextView.text = event.title

        val start = event.startTime
        val end = event.endTime

        val timeFormat = SimpleDateFormat("HH:mm")
        val monthDayFormat = SimpleDateFormat("MM/dd")
        val fullDateFormat = SimpleDateFormat("yyyy/MM/dd")

        val startDT = LocalDateTime.ofInstant(event.startTime.toInstant(), ZoneId.systemDefault())
        val endDT = LocalDateTime.ofInstant(event.endTime.toInstant(), ZoneId.systemDefault())

        val sameDay = startDT.toLocalDate() == endDT.toLocalDate()
        val sameYear = startDT.year == endDT.year

        holder.intervalTextView.text =
            if (event.wholeDayEvent) {
                "Whole day event"
            } else {
                when {
                    sameDay -> {
                        "${timeFormat.format(start)} - ${timeFormat.format(end)}"
                    }
                    sameYear -> {
                        "${monthDayFormat.format(start)} ${timeFormat.format(start)} - " +
                                "${monthDayFormat.format(end)} ${timeFormat.format(end)}"
                    }
                    else -> {
                        "${fullDateFormat.format(start)} ${timeFormat.format(start)} - " +
                                "${fullDateFormat.format(end)} ${timeFormat.format(end)}"
                    }
                }
            }

        // Clicks
        holder.deleteEventImageButton.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) showDeleteDialog(pos)
        }

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onEventActionListener?.onItemClicked(dataList[pos])
            }
        }
    }

    override fun getItemCount() = dataList.size

    fun updateData(newData: List<EventData>) {
        val oldSize = dataList.size
        dataList.clear()
        dataList.addAll(newData)
        notifyItemRangeChanged(0, newData.size)

        if (newData.size < oldSize) {
            notifyItemRangeRemoved(newData.size, oldSize - newData.size)
        }
    }

    private fun showDeleteDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        builder.setView(dialogView)

        val buttonCancel = dialogView.findViewById<Button>(R.id.button_cancel)
        val buttonDelete = dialogView.findViewById<Button>(R.id.button_delete)

        val dialog = builder.create()

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonDelete.setOnClickListener {
            if (position != RecyclerView.NO_POSITION) {
                // Notify fragment or listener
                onEventActionListener?.onItemRemoved(dataList[position])

                // Remove from adapter list
                dataList.removeAt(position)

                // Update RecyclerView
                notifyItemRemoved(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}