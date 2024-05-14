package com.example.myapplication.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.FrameLayout
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
import com.example.myapplication.databinding.CalendarFragmentBinding
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class CalendarDetailFragment : Fragment(), EventDetailFragment.EventDetailListener, CustomEventAdapter.OnItemRemovedListener {

    private var _binding: CalendarFragmentBinding? = null
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
    private lateinit var eventsMap: HashMap<Pair<Int, Int>, Pair<MutableList<Event>, Int>>
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CalendarFragmentBinding.inflate(inflater, container, false)
        return binding.root
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
                dataList = Pair(mutableListOf(),-1)
            }
            eventsMap.clear()
            adapter = CustomEventAdapter(requireActivity() as AppCompatActivity, dataList.first, thisCalendar!!.name)
            eventsRecyclerView.adapter = adapter
        }


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

            val newDataList = if (eventsMap[getGridIndicesForDate(tempCalendar.time)] != null) eventsMap[getGridIndicesForDate(tempCalendar.time)]!!.first.toList() else listOf()
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
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.stroke_width)

        val shapeDrawable = GradientDrawable()
        shapeDrawable.shape = GradientDrawable.OVAL
        shapeDrawable.setStroke(strokeWidth, color)
        shapeDrawable.setColor(Color.TRANSPARENT)

        circleView.background = shapeDrawable

        return circleView
    }

    private fun createRectangle(context: Context, color: Int): View {
        val rectangleView = View(context)
        val rectangleWidth = 90 // Adjust the width as needed
        val rectangleHeight = 90 // Adjust the height as needed
        val strokeWidth = context.resources.getDimensionPixelSize(R.dimen.stroke_width)

        val layoutParams = ViewGroup.LayoutParams(rectangleWidth, rectangleHeight)
        rectangleView.layoutParams = layoutParams

        val shapeDrawable = object : ShapeDrawable(object : RectShape() {
            override fun draw(canvas: Canvas, paint: Paint) {
                // Override draw method to draw only top and bottom edges
                val rect = rect()
                canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint) // Top edge
                canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint) // Bottom edge
            }
        }) {
            override fun onDraw(shape: Shape, canvas: Canvas, paint: Paint) {
                paint.color = color
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = strokeWidth.toFloat()
                shape.draw(canvas, paint)
            }
        }

        rectangleView.background = shapeDrawable

        return rectangleView
    }
    private fun fillEventsMap(calendar: Calendar){
        eventsMap = HashMap()
        val eventCalendar = Calendar.getInstance()
        val selectedCalendar = calendar
        for (event in thisCalendar!!.events) {
            eventCalendar.time = event.startTime
            if (eventCalendar.get(Calendar.MONTH) == selectedCalendar.get(Calendar.MONTH)) {
                //starting day
                val startTime = event.startTime
                val key = getGridIndicesForDate(startTime)

                if (eventsMap.containsKey(key)) {
                    if (eventsMap[key]!!.second == 0) {
                        val tempMutableList = eventsMap[key]?.first
                        tempMutableList!!.add(event)
                        eventsMap[key] = Pair(tempMutableList, 0)
                    }
                    else {
                        val tempMutableList = eventsMap[key]?.first
                        tempMutableList!!.add(event)
                        eventsMap[key] = Pair(tempMutableList, 2)
                    }
                } else {
                    val eventList = mutableListOf(event)
                    eventsMap[key] = Pair(eventList,0)
                }

                //between days

                val tempStartingCalendar = Calendar.getInstance()
                tempStartingCalendar.time = event.startTime

                // Extract year, month, and day from the original date
                var year = tempStartingCalendar.get(Calendar.YEAR)
                var month = tempStartingCalendar.get(Calendar.MONTH)
                var day = tempStartingCalendar.get(Calendar.DAY_OF_MONTH)

                // Create a new Calendar instance and set it to the extracted year, month, and day
                val newStartingCalendar = Calendar.getInstance()
                newStartingCalendar.set(year, month, day, 0, 0, 0)
                newStartingCalendar.set(Calendar.MILLISECOND, 0)

                // Get the new date with hours and minutes set to 0
                var newStartingTime = newStartingCalendar.time

                val tempEndCalendar = Calendar.getInstance()
                tempEndCalendar.time = event.endTime

                // Extract year, month, and day from the original date
                year = tempEndCalendar.get(Calendar.YEAR)
                month = tempEndCalendar.get(Calendar.MONTH)
                day = tempEndCalendar.get(Calendar.DAY_OF_MONTH)

                // Create a new Calendar instance and set it to the extracted year, month, and day
                val newEndCalendar = Calendar.getInstance()
                newEndCalendar.set(year, month, day, 0, 0, 0)
                newEndCalendar.set(Calendar.MILLISECOND, 0)
                var currentDate = Calendar.getInstance()
                currentDate.time = newStartingTime
                currentDate.add(Calendar.DAY_OF_MONTH, 1)
                var newEndTime = newEndCalendar.time
                while (currentDate.time.before(newEndTime)) {
                    val throughKey = getGridIndicesForDate(currentDate.time)
                    if (eventsMap.containsKey(throughKey)) {
                        if (eventsMap[throughKey]!!.second == 1) {
                            val tempMutableList = eventsMap[throughKey]?.first
                            tempMutableList!!.add(event)
                            eventsMap[throughKey] = Pair(tempMutableList, 1)
                        }
                        else {
                            val tempMutableList = eventsMap[throughKey]?.first
                            tempMutableList!!.add(event)
                            eventsMap[throughKey] = Pair(tempMutableList, 2)
                        }
                    } else {
                        val eventList = mutableListOf(event)
                        eventsMap[throughKey] = Pair(eventList,1)
                    }
                    currentDate.add(Calendar.DAY_OF_MONTH, 1)
                }

                //ending day
                tempStartingCalendar.time = event.startTime

                // Extract year, month, and day from the original date
                year = tempStartingCalendar.get(Calendar.YEAR)
                month = tempStartingCalendar.get(Calendar.MONTH)
                day = tempStartingCalendar.get(Calendar.DAY_OF_MONTH)

                // Create a new Calendar instance and set it to the extracted year, month, and day
                newStartingCalendar.set(year, month, day, 0, 0, 0)
                newStartingCalendar.set(Calendar.MILLISECOND, 0)

                // Get the new date with hours and minutes set to 0
                newStartingTime = newStartingCalendar.time

                tempEndCalendar.time = event.endTime

                // Extract year, month, and day from the original date
                year = tempEndCalendar.get(Calendar.YEAR)
                month = tempEndCalendar.get(Calendar.MONTH)
                day = tempEndCalendar.get(Calendar.DAY_OF_MONTH)

                // Create a new Calendar instance and set it to the extracted year, month, and day
                newEndCalendar.set(year, month, day, 0, 0, 0)
                newEndCalendar.set(Calendar.MILLISECOND, 0)

                // Get the new date with hours and minutes set to 0
                newEndTime = newEndCalendar.time
                if (newStartingTime.before(newEndTime)) {
                    val endKey = getGridIndicesForDate(event.endTime)
                    if (eventsMap.containsKey(endKey)) {
                        if (eventsMap[endKey]!!.second == 0) {
                            val tempMutableList = eventsMap[endKey]?.first
                            tempMutableList!!.add(event)
                            eventsMap[endKey] = Pair(tempMutableList, 0)
                        }
                        else {
                            val tempMutableList = eventsMap[endKey]?.first
                            tempMutableList!!.add(event)
                            eventsMap[endKey] = Pair(tempMutableList, 2)
                        }
                    } else {
                        val eventList = mutableListOf(event)
                        eventsMap[endKey] = Pair(eventList,0)
                    }
                }

            }
        }
    }

    private fun fillCalendarCircles() {
        calendarOverlayGrid.removeAllViews()

        //fill appropriate gridlayout cells with circles and empty views
        val cellPadding = resources.getDimensionPixelSize(R.dimen.one_dp)

        for (row in 0 until 6) {
            for (column in 0 until 7) {
                if (eventsMap.containsKey(Pair(row, column))) {
                    val shapeId = eventsMap[Pair(row, column)]?.second
                    when(shapeId) {
                        0 -> {
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
                        1 -> {
                            val orangeRectangle = createRectangle(requireContext(), Color.parseColor("#FFA500"))
                            val frameLayout = FrameLayout(requireContext())
                            frameLayout.addView(orangeRectangle)

                            val layoutParams = GridLayout.LayoutParams().apply {
                                rowSpec = GridLayout.spec(row, 1f)
                                columnSpec = GridLayout.spec(column, 1f)
                                setMargins(cellPadding*10, cellPadding*2, cellPadding*6, cellPadding*3)
                                width = 90
                                height = 90
                            }

                            frameLayout.layoutParams = layoutParams

                            calendarOverlayGrid.addView(frameLayout)
                        }
                        2 -> {
                            val orangeCircle = createCircle(requireContext(), Color.parseColor("#FFA500"))
                            val orangeRectangle = createRectangle(requireContext(), Color.parseColor("#FFA500"))
                            val layoutParams = GridLayout.LayoutParams().apply {
                                rowSpec = GridLayout.spec(row, 1f)
                                columnSpec = GridLayout.spec(column, 1f)
                                setMargins(cellPadding*10, cellPadding*2, cellPadding*6, cellPadding*3)
                                width = 90
                                height = 90
                            }
                            calendarOverlayGrid.addView(orangeCircle, layoutParams)
                            calendarOverlayGrid.addView(orangeRectangle, layoutParams)
                        }
                    }

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