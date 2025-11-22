package com.taskraze.myapplication.view.calendar.shared

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
import com.taskraze.myapplication.view.calendar.details.CalendarDetailFragment
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.calendar.FirestoreViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CustomSharedCalendarAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<CalendarData>) : RecyclerView.Adapter<CustomSharedCalendarAdapter.SharedCalendarItemViewHolder>() {
    inner class SharedCalendarItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitleCalendarItem)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewPeopleNumber)
        val lastUpdatedTextView: TextView = itemView.findViewById(R.id.textViewLastUpdated)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }
    private val firestoreViewModel: FirestoreViewModel =
        ViewModelProvider(activity)[FirestoreViewModel::class.java]

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): SharedCalendarItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.calendar_item_view, view, false)
        return SharedCalendarItemViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(viewHolder: SharedCalendarItemViewHolder, position: Int) {

        val currentItem = dataList[position]
        viewHolder.titleTextView.text = currentItem.name
        val tempString = "People: " + currentItem.sharedPeopleNumber.toString()
        viewHolder.numberTextView.text = tempString
        //convert instant to date
        val lastUpdatedDate =  Instant.now()
        val localDate = instantToLocalDate(lastUpdatedDate)
        val formattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-d"))

        viewHolder.lastUpdatedTextView.text = formattedDate
        viewHolder.deleteImageButton.setOnClickListener{
            showDeleteDialog(viewHolder.layoutPosition)
        }

        viewHolder.itemView.setOnClickListener{
            viewHolder.viewModel.newEventStartingDay = null
            val calendarDetailFragment = CalendarDetailFragment()

            viewHolder.viewModel.passCalendarToFragment(dataList[position])

            val fragmentManager = activity.supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.constraint_container, calendarDetailFragment) // Replace R.id.fragment_container with your actual container id
                .addToBackStack(null) // Add to back stack to allow navigating back
                .commit()
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

    fun updateData(newData: List<CalendarData>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyItemInserted(dataList.size)
    }

    private fun showDeleteDialog(position: Int) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.delete_dialog, null)
        dialogView.findViewById<TextView>(R.id.dialog_message).text =
            activity.getString(R.string.are_you_sure_you_want_to_quit_this_group)
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
                firestoreViewModel.removeUserFromCalendar(AuthViewModel.getUserId(), dataList[position].id)
                dataList.removeAt(position)
                notifyItemRemoved(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

}