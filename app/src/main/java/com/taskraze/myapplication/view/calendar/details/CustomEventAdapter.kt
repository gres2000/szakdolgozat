package com.taskraze.myapplication.view.calendar.details

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.FirestoreCalendarRepository
import com.taskraze.myapplication.model.calendar.LocalCalendarRepository
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CustomEventAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<EventData>, private val calendar: CalendarData) : RecyclerView.Adapter<CustomEventAdapter.EventItemViewHolder>() {
    inner class EventItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val startingTimeTextView: TextView = itemView.findViewById(R.id.textViewStartingTime)
        val eventTitleTextView: TextView = itemView.findViewById(R.id.textViewEventTitle)
        val intervalTextView: TextView = itemView.findViewById(R.id.textViewInterval)
        val deleteEventImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDeleteEvent)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }
    private var onItemRemovedListener: OnItemRemovedListener? = null
    private val localCalendarRepository = LocalCalendarRepository()
    private val firestoreCalendarRepository = FirestoreCalendarRepository(localCalendarRepository)

    interface OnItemRemovedListener {
        fun onItemRemoved(event: EventData)
    }
    fun setOnItemRemovedListener(listener: OnItemRemovedListener) {
        onItemRemovedListener = listener
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): EventItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.event_item_view, view, false)
        return EventItemViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: EventItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]

        val dateFormat = SimpleDateFormat("HH:mm")

        viewHolder.startingTimeTextView.text = if (!currentItem.wholeDayEvent) dateFormat.format(currentItem.startTime).toString() else "--:--"
        viewHolder.eventTitleTextView.text = currentItem.title

        val tmpString = if (!currentItem.wholeDayEvent) dateFormat.format(currentItem.startTime).toString() + "-" + dateFormat.format(currentItem.endTime).toString() else "Whole day event"
        viewHolder.intervalTextView.text = tmpString

        viewHolder.deleteEventImageButton.setOnClickListener{
            showDeleteDialog(viewHolder.layoutPosition)
        }

        viewHolder.itemView.setOnClickListener{
            //open event for editing needs implementation
//            viewHolder.viewModel.toggleExistingEvent()
//            val clickedEvent = dataList[position]
//            openEventDetailFragmentForEditing(clickedEvent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun instantToLocalDate(instant: Instant): LocalDate {
        // Convert Instant to Epoch Milliseconds
        val epochMillis = instant.toEpochMilli()

        // Convert Epoch Milliseconds to LocalDate
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
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
            val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
            viewModel.viewModelScope.launch {
                if (calendar.owner.email == AuthViewModel.loggedInUser!!.email) {
                    viewModel.deleteEventFromRoom(activity, dataList[position], calendar.name)
                    onItemRemovedListener?.onItemRemoved(dataList[position])
                    dataList.removeAt(position)
                    notifyItemRemoved(position)
                    firestoreCalendarRepository.saveAllCalendarsToFirestoreDB(activity, AuthViewModel.loggedInUser!!.email)
                }
                else {
                    MainViewModel.deleteEventFromSharedCalendar(dataList[position], calendar.owner.email, calendar.id)
                    onItemRemovedListener?.onItemRemoved(dataList[position])
                    dataList.removeAt(position)
                    notifyItemRemoved(position)
                }


            }
            dialog.dismiss()
        }

        dialog.show()
    }
    fun getDataList():List<EventData> {
        return dataList
    }
}