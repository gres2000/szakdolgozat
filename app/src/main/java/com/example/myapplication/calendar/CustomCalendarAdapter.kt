package com.example.myapplication.calendar

import android.os.Build
import android.util.MutableLong
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.tasks.Task
import com.example.myapplication.viewModel.MainViewModel
import kotlinx.coroutines.launch
import org.w3c.dom.Text
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CustomCalendarAdapter(val activity: AppCompatActivity, private val dataList: MutableList<Calendar>) : RecyclerView.Adapter<CustomCalendarAdapter.CalendarItemViewHolder>() {
    inner class CalendarItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitleCalendarItem)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewPeopleNumber)
        val lastUpdatedTextView: TextView = itemView.findViewById(R.id.textViewLastUpdated)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): CalendarItemViewHolder {

        val itemView = LayoutInflater.from(view.context).inflate(R.layout.calendar_item_view, view, false)

        return CalendarItemViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(viewHolder: CalendarItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        val currentItem = dataList[position]
        viewHolder.titleTextView.text = currentItem.name
        viewHolder.numberTextView.text = currentItem.sharedPeopleNumber.toString()
        //convert instant to date
        val lastUpdatedDate =  Instant.now()
        val localDate = instantToLocalDate(lastUpdatedDate)
        val formattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-d"))

        viewHolder.lastUpdatedTextView.text = formattedDate
        viewHolder.deleteImageButton.setOnClickListener{
            showDeleteDialog(position)
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

    fun updateData(newData: List<Calendar>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyItemInserted(dataList.size)
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
            // Perform the delete action here
            // For example, call a method to delete the item from your data source
            val viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
            viewModel.viewModelScope.launch { // Launch a coroutine
                viewModel.deleteCalendarFromRoom(activity, dataList[position])
                dataList.removeAt(position)
                notifyItemRemoved(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}