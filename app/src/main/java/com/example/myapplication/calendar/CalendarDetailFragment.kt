package com.example.myapplication.calendar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.CalendarDialogFragmentBinding
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class CalendarDetailFragment : Fragment(), EventDetailFragment.EventDetailListener, CustomEventAdapter.OnItemRemovedListener {

    private var _binding: CalendarDialogFragmentBinding? = null
    private lateinit var titleTextView: TextView
    private lateinit var ownerTextView: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var viewModel: MainViewModel
    private lateinit var backButtonImageButton: ImageButton
    private lateinit var addNewEvent: FloatingActionButton
    private lateinit var adapter: CustomEventAdapter
    private lateinit var calendarOverlayGrid: GridLayout
    private var thisCalendar: MyCalendar? = null
    private lateinit var eventsMap: HashMap<Pair<Int, Int>, MutableList<Event>>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.calendar_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        thisCalendar = viewModel.getCalendarToFragment()
        titleTextView = view.findViewById(R.id.textViewDetailCalendarTitle)
        ownerTextView = view.findViewById(R.id.textViewDetailCalendarOwner)
        calendarView = view.findViewById(R.id.calendarViewCalendarDetail)
        eventsRecyclerView = view.findViewById(R.id.recyclerViewEvents)
        eventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        usersRecyclerView = view.findViewById(R.id.recyclerViewUsers)
        backButtonImageButton = view.findViewById(R.id.imageButtonLeftArrow)
        addNewEvent = view.findViewById(R.id.fab_add_event)
        eventsRecyclerView = view.findViewById(R.id.recyclerViewEvents)
        calendarOverlayGrid = view.findViewById(R.id.gridLayoutCalendarOverlay)
        titleTextView.text = thisCalendar?.name

        val ownerString = getString(R.string.owner_double_dots) + " " + thisCalendar?.owner?.username
        ownerTextView.text = ownerString

        viewModel.viewModelScope.launch {

            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.timeInMillis = calendarView.date

            if (viewModel.newEventStartingDay != null) {
                val cal = viewModel.newEventStartingDay
                fillEventsMap(cal!!)
            }
            else {
                fillEventsMap(selectedCalendar)
            }

            fillCalendarCircles()

            var dataList = eventsMap[getGridIndicesForDate(selectedCalendar.time)]
            if (dataList == null) {
                dataList = mutableListOf()
            }
            eventsMap.clear()
            adapter = CustomEventAdapter(requireActivity() as AppCompatActivity, dataList, thisCalendar!!.name)
            eventsRecyclerView.adapter = adapter
        }

        //create circle


        // OnClickListeners
        backButtonImageButton.setOnClickListener{
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.timeInMillis = calendarView.date
        viewModel.newEventStartingDay = selectedCalendar
        addNewEvent.setOnClickListener{
            viewModel.passCalendarToFragment(thisCalendar!!)

            val eventDetailFragment = EventDetailFragment()
            eventDetailFragment.listener = this
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.constraint_container, eventDetailFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }

        (eventsRecyclerView.adapter as CustomEventAdapter).setOnItemRemovedListener(this)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            val tempCalendar = Calendar.getInstance()
            tempCalendar.set(Calendar.YEAR, year)
            tempCalendar.set(Calendar.MONTH, month)
            tempCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            fillEventsMap(tempCalendar)

            val newDataList = if (eventsMap[getGridIndicesForDate(tempCalendar.time)] != null) eventsMap[getGridIndicesForDate(tempCalendar.time)]!!.toList() else listOf()
            adapter.updateData(newDataList)
            fillCalendarCircles()
            eventsMap.clear()
            //for new event creation
            viewModel.newEventStartingDay = tempCalendar
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.passCalendarToFragment(thisCalendar!!)
        _binding = null
    }

    override fun onNewEventCreated(event: Event) {
        viewModel.viewModelScope.launch {

            viewModel.addEventToCalendar(requireContext(), event, thisCalendar!!)

            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun getGridIndicesForDate(date: Date): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Get the day of the week (Sunday=1, Monday=2, ..., Saturday=7)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)


        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)


        val firstDayOfWeek = calendar.firstDayOfWeek
        val row: Int
        val column: Int
        if (calendar.firstDayOfWeek == Calendar.MONDAY) {
            row = (dayOfMonth + firstDayOfWeek) / 7
            column = (dayOfWeek + 5) % 7
        }
        else {
            row = (dayOfMonth + firstDayOfWeek + 1) / 7
            column = (dayOfWeek + 6) % 7
        }

        return Pair(row, column)
    }

    private fun createCircle(context: Context, color: Int): View {
        val circleView = View(context)
        val circleSize = 1 * 3 / 4 // Adjust the multiplier as needed
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.stroke_width)

        val layoutParams = ViewGroup.LayoutParams(circleSize, circleSize)
        circleView.layoutParams = layoutParams

        val shapeDrawable = GradientDrawable()
        shapeDrawable.shape = GradientDrawable.OVAL
        shapeDrawable.setStroke(strokeWidth, color)
        shapeDrawable.setColor(Color.TRANSPARENT)

        circleView.background = shapeDrawable

        return circleView
    }
    private fun fillEventsMap(calendar: Calendar){
        eventsMap = HashMap()
        val eventCalendar = Calendar.getInstance()
        val selectedCalendar = calendar
        for (event in thisCalendar!!.events) {
            eventCalendar.time = event.startTime
            if (eventCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)) {
                val startTime = event.startTime
                val key = getGridIndicesForDate(startTime)

                if (eventsMap.containsKey(key)) {
                    eventsMap[key]?.add(event)
                } else {
                    val eventList = mutableListOf(event)
                    eventsMap[key] = eventList
                }

            }
        }
    }

    private fun fillCalendarCircles() {
        calendarOverlayGrid.removeAllViews()

        //fill appropriate gridlayout cells with circles and empty views
        val cellPadding = resources.getDimensionPixelSize(R.dimen.one_dp)

        for (row in 0 until 5) {
            for (column in 0 until 7) {
                if (eventsMap.containsKey(Pair(row, column))) {
                    val orangeCircle = createCircle(requireContext(), Color.parseColor("#FFA500"))
                    val layoutParams = GridLayout.LayoutParams().apply {
                        rowSpec = GridLayout.spec(row, 1f)
                        columnSpec = GridLayout.spec(column, 1f)
                        setMargins(cellPadding*10, cellPadding*2, cellPadding*6, cellPadding*3)
                        width = 90
                        height = 90
                    }
                    calendarOverlayGrid.addView(orangeCircle, layoutParams)
                }
                else {
                    val cellView = View(context)
                    val layoutParamsCellView = GridLayout.LayoutParams()
                    layoutParamsCellView.rowSpec = GridLayout.spec(row, 1f)
                    layoutParamsCellView.columnSpec = GridLayout.spec(column, 1f)
                    layoutParamsCellView.width = 90
                    layoutParamsCellView.height = 90
                    layoutParamsCellView.setMargins(cellPadding*10, cellPadding*2, cellPadding*6, cellPadding*3)
                    cellView.layoutParams = layoutParamsCellView
                    cellView.background = ContextCompat.getDrawable(requireContext(), R.color.transparent)
                    calendarOverlayGrid.addView(cellView)

                }


            }
        }
    }

    override fun onItemRemoved(event: Event) {
        thisCalendar!!.events.remove(event)
        if (viewModel.newEventStartingDay != null) {
            val cal = viewModel.newEventStartingDay
            fillEventsMap(cal!!)
        }
        else {
            val tempCalendar = Calendar.getInstance()
            tempCalendar.timeInMillis = calendarView.date
            fillEventsMap(tempCalendar)
        }
        fillCalendarCircles()
        calendarOverlayGrid.requestLayout()
    }

}