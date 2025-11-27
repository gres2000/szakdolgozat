package com.taskraze.myapplication.view.calendar.own

import AuthViewModelFactory
import CalendarViewModelFactory
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
import com.taskraze.myapplication.viewmodel.calendar.CalendarViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CustomOwnCalendarAdapter(private val activity: AppCompatActivity, private val dataList: MutableList<CalendarData>) : RecyclerView.Adapter<CustomOwnCalendarAdapter.CalendarItemViewHolder>() {
    inner class CalendarItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitleCalendarItem)
        val numberTextView: TextView = itemView.findViewById(R.id.textViewPeopleNumber)
        val lastUpdatedTextView: TextView = itemView.findViewById(R.id.textViewLastUpdated)
        val deleteImageButton: ImageButton = itemView.findViewById(R.id.imageButtonDelete)
        var viewHolderId: Int = -1
        lateinit var viewModel: MainViewModel
    }

    private lateinit var calendarViewModel: CalendarViewModel

    override fun onCreateViewHolder(view: ViewGroup, viewType: Int): CalendarItemViewHolder {
        val itemView = LayoutInflater.from(view.context).inflate(R.layout.calendar_item_view, view, false)
        val authViewModel = ViewModelProvider(
            activity,
            AuthViewModelFactory(activity)
        )[com.taskraze.myapplication.viewmodel.auth.AuthViewModel::class.java]
        calendarViewModel = ViewModelProvider(
            activity,
            CalendarViewModelFactory(authViewModel)
        )[CalendarViewModel::class.java]
        return CalendarItemViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(viewHolder: CalendarItemViewHolder, position: Int) {
        viewHolder.viewModel = ViewModelProvider(activity)[MainViewModel::class.java]
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
                .replace(R.id.constraint_container, calendarDetailFragment)
                .addToBackStack(null)
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
                calendarViewModel.deleteSharedUsersFromCalendar(dataList[position].sharedPeople, dataList[position].id)
                calendarViewModel.deleteCalendar(dataList[position])
                dataList.removeAt(position)
                notifyItemRemoved(position)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

}