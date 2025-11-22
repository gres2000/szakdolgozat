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

class CustomEventAdapter(
    private val activity: AppCompatActivity,
    private val dataList: MutableList<EventData>,
    private val calendar: CalendarData,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<CustomEventAdapter.EventItemViewHolder>() {
    inner class EventItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val startingTimeTextView: TextView = itemView.findViewById(R.id.textViewStartingTime)
        val eventTitleTextView: TextView = itemView.findViewById(R.id.textViewEventTitle)
        val intervalTextView: TextView = itemView.findViewById(R.id.textViewInterval)
        val deleteEventImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDeleteEvent)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }
    private var onEventActionListener: OnEventActionListener? = null

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

    override fun onBindViewHolder(viewHolder: EventItemViewHolder, position: Int) {
        val currentItem = dataList[position]

        val dateFormat = SimpleDateFormat("HH:mm")

        viewHolder.startingTimeTextView.text = if (!currentItem.wholeDayEvent) dateFormat.format(currentItem.startTime).toString() else "--:--"
        viewHolder.eventTitleTextView.text = currentItem.title

        val tmpString = if (!currentItem.wholeDayEvent) dateFormat.format(currentItem.startTime).toString() + "-" + dateFormat.format(currentItem.endTime).toString() else "Whole day event"
        viewHolder.intervalTextView.text = tmpString

        viewHolder.deleteEventImageButton.setOnClickListener {
            val pos = viewHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                showDeleteDialog(pos)
            }
        }

        viewHolder.itemView.setOnClickListener {
            val pos = viewHolder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onEventActionListener?.onItemClicked(dataList[pos])
            }
        }
    }

    override fun getItemCount() = dataList.size

    fun updateData(newData: List<EventData>) {
        val originSize = dataList.size
        dataList.clear()
        dataList.addAll(newData)
        if (newData.isNotEmpty()) {
            notifyDataSetChanged()
        }
        else {
            notifyItemRangeRemoved(0, originSize)
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