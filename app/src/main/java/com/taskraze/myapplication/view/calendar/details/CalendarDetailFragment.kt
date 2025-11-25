package com.taskraze.myapplication.view.calendar.details

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.taskraze.myapplication.R
import com.taskraze.myapplication.common.CustomUsersAdapter
import com.taskraze.myapplication.databinding.CalendarDetailFragmentBinding
import com.taskraze.myapplication.model.calendar.CalendarData
import com.taskraze.myapplication.model.calendar.EventData
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import com.taskraze.myapplication.viewmodel.calendar.CalendarExportViewModel
import com.taskraze.myapplication.viewmodel.calendar.FirestoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Calendar
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import java.time.ZoneId
import androidx.core.graphics.toColorInt

class CalendarDetailFragment : Fragment(), EventDetailFragment.EventDetailListener,
    CustomEventAdapter.OnEventActionListener, CustomUsersAdapter.ChatActionListener, CustomUsersAdapter.DeleteActionListener {

    class DayViewContainer(
        view: View,
        val onClick: (CalendarDay) -> Unit
    ) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.dayText)
        val eventIndicator: View = view.findViewById(R.id.eventIndicator)
        val eventIndicatorContainer: FrameLayout = view.findViewById(R.id.eventIndicatorContainer)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    onClick(day)
                }
            }
        }
    }
    private lateinit var firestoreViewModel: FirestoreViewModel
    private var selectedDate: LocalDate? = null
    private lateinit var toolbar: Toolbar
    private var _binding: CalendarDetailFragmentBinding? = null
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: CustomEventAdapter
    private var thisCalendar: CalendarData? = null
    private val binding get() = _binding!!
    private lateinit var exportViewModel: CalendarExportViewModel
    private lateinit var msalApp: ISingleAccountPublicClientApplication
    private var msalAccount = null as com.microsoft.identity.client.IAccount?

    private val eventColors = listOf(
        "#F44336".toColorInt(), "#E91E63".toColorInt(), "#9C27B0".toColorInt(),
        "#673AB7".toColorInt(), "#3F51B5".toColorInt(), "#2196F3".toColorInt(),
        "#03A9F4".toColorInt(), "#00BCD4".toColorInt(), "#009688".toColorInt(),
        "#4CAF50".toColorInt(), "#8BC34A".toColorInt(), "#CDDC39".toColorInt(),
        "#FFEB3B".toColorInt(), "#FFC107".toColorInt(), "#FF9800".toColorInt(),
        "#FF5722".toColorInt(), "#795548".toColorInt(), "#9E9E9E".toColorInt(),
        "#607D8B".toColorInt(), "#F06292".toColorInt(), "#BA68C8".toColorInt(),
        "#9575CD".toColorInt(), "#64B5F6".toColorInt(), "#4DD0E1".toColorInt(),
        "#4DB6AC".toColorInt(), "#81C784".toColorInt(), "#AED581".toColorInt(),
        "#DCE775".toColorInt(), "#FFD54F".toColorInt(), "#FF8A65".toColorInt()
    )

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            if (account != null && account.account != null) {
                // Launch coroutine to get token and export
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val scope = "oauth2:https://www.googleapis.com/auth/calendar"
                        val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(requireContext(), account.account!!, scope)

                        launch(Dispatchers.Main) {
                            // Export events using ViewModel
                            exportViewModel.exportEventsToGoogleCalendar(
                                events = thisCalendar!!.events,
                                accessToken = token
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to get Google token", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CalendarDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun updateEventsForSelectedDay(date: LocalDate) {
        val filteredEvents = thisCalendar!!.events.filter { event ->
            val startDate = event.startTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            val endDate = event.endTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            !date.isBefore(startDate) && !date.isAfter(endDate)
        }

        adapter.updateData(filteredEvents)
    }

    private fun onDayClicked(day: CalendarDay) {
        val oldDate = selectedDate
        selectedDate = day.date

        oldDate?.let { binding.calendarViewCalendarDetail.notifyDateChanged(it) }
        binding.calendarViewCalendarDetail.notifyDateChanged(day.date)

        updateEventsForSelectedDay(day.date)

        binding.textViewSelectedDate.text = "${day.date.year}-${day.date.monthValue}-${day.date.dayOfMonth}"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        firestoreViewModel = ViewModelProvider(requireActivity())[FirestoreViewModel::class.java]
        exportViewModel = ViewModelProvider(requireActivity())[CalendarExportViewModel::class.java]

        initMSAL()

        toolbar = binding.calendarDetailToolbar
        thisCalendar = viewModel.getCalendarToFragment()

        exportViewModel.exportState.observe(viewLifecycleOwner) { state ->
            when (state) {
                "success" -> Toast.makeText(requireContext(), "Export completed!", Toast.LENGTH_SHORT).show()
                "failure" -> Toast.makeText(requireContext(), "Export failed!", Toast.LENGTH_SHORT).show()
            }
        }

        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        createMenuToolbar()

        binding.recyclerViewEvents.layoutManager = LinearLayoutManager(requireContext())

        val itemDecoration = UsersRecyclerItemDecoration()
        binding.recyclerViewUsers.addItemDecoration(itemDecoration)
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.textViewDetailCalendarTitle.text = thisCalendar?.name
        binding.textViewDetailCalendarOwner.text = "${getString(R.string.owner_double_dots)} ${thisCalendar?.owner?.username}"

        viewModel.viewModelScope.launch {
            adapter = CustomEventAdapter(
                requireActivity() as AppCompatActivity,
                thisCalendar!!.events.toMutableList(),
            )
            binding.recyclerViewEvents.adapter = adapter

            selectedDate = LocalDate.now()
            binding.textViewSelectedDate.text = "${selectedDate!!.year}-${selectedDate!!.monthValue}-${selectedDate!!.dayOfMonth}"

            // Update events for today
            updateEventsForSelectedDay(selectedDate!!)

            val usersAdapter = CustomUsersAdapter(
                requireContext() as AppCompatActivity,
                thisCalendar!!.sharedPeople,
                null,
                this@CalendarDetailFragment,
                AuthViewModel.getUserId() == thisCalendar!!.owner.email // TODO this should be owner.userId
            )
            binding.recyclerViewUsers.adapter = usersAdapter
        }

        // calendarView setup
        val calendarView = binding.calendarViewCalendarDetail
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

            override fun create(view: View) = DayViewContainer(view) { day -> onDayClicked(day) }

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                container.textView.text = data.date.dayOfMonth.toString()

                // grey out days outside month
                if (data.position == DayPosition.MonthDate) {
                    container.textView.alpha = 1f
                    container.textView.setTextColor(Color.WHITE)
                } else {
                    container.textView.alpha = 0.3f
                    container.textView.setTextColor(Color.GRAY)
                }

                // highlight selected day
                if (data.date == selectedDate) {
                    container.textView.setBackgroundResource(R.drawable.selected_day_bg)
                    container.textView.setTextColor(Color.BLACK)
                } else {
                    container.textView.background = null
                }

                val eventsForDay = thisCalendar?.events?.filter { event ->
                    val start = event.startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    val end = event.endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    !data.date.isBefore(start) && !data.date.isAfter(end)
                } ?: emptyList()

                container.eventIndicatorContainer.removeAllViews()

                if (eventsForDay.isNotEmpty()) {
                    var sizeDp = 40
                    val sizeDecrement = 5
                    val minSizeDp = 20

                    eventsForDay.forEach { event ->
                        val start = event.startTime.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        val end = event.endTime.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()

                        val drawableRes = when {
                            data.date == start && data.date == end -> R.drawable.event_circle
                            data.date == start -> R.drawable.event_start_circle
                            data.date == end -> R.drawable.event_end_circle
                            data.date.isAfter(start) && data.date.isBefore(end) -> R.drawable.event_middle_circle
                            else -> R.drawable.event_circle
                        }

                        val circleView = View(requireContext()).apply {
                            val layoutSize = sizeDp.dpToPx()
                            layoutParams = FrameLayout.LayoutParams(layoutSize, layoutSize).apply {
                                gravity = Gravity.CENTER
                            }

                            when (val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)?.mutate()) {
                                is LayerDrawable -> {
                                    for (i in 0 until drawable.numberOfLayers - 1) {
                                        val layer = drawable.getDrawable(i)
                                        if (layer is GradientDrawable) {
                                            layer.setStroke(2.dpToPx(), getEventColor(event.id))
                                        }
                                    }
                                    background = drawable
                                }

                                is GradientDrawable -> {
                                    drawable.setStroke(2.dpToPx(), getEventColor(event.id))
                                    background = drawable
                                }

                                else -> {
                                    setBackgroundColor(getEventColor(event.id))
                                }
                            }
                        }

                        container.eventIndicatorContainer.addView(circleView)

                        sizeDp = (sizeDp - sizeDecrement).coerceAtLeast(minSizeDp)
                    }
                }

                container.eventIndicator.visibility = if (eventsForDay.isEmpty()) View.GONE else View.INVISIBLE
            }
        }


        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        val titlesContainer = binding.titlesContainer
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                textView.text = dayOfWeek
            }


        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        calendarView.setup(startMonth, endMonth, DayOfWeek.MONDAY)
        calendarView.scrollToMonth(currentMonth)

        calendarView.monthScrollListener = { month ->
            val ym = month.yearMonth

            val formatted = ym.month.name.lowercase()
                .replaceFirstChar { it.uppercase() }

            binding.textViewCurrentMonth.text = "$formatted ${ym.year}"
        }

        // click listeners
        binding.imageButtonLeftArrow.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.fabAddUser.setOnClickListener { lifecycleScope.launch { showChooseFriendDialog() } }

        val selectedCalendar = Calendar.getInstance()
        viewModel.newEventStartingDay = selectedCalendar
        binding.fabAddEvent.setOnClickListener {
            viewModel.passCalendarToFragment(thisCalendar!!)

            viewModel.newEventStartingDay = selectedDate?.let { date ->
                Calendar.getInstance().apply {
                    set(date.year, date.monthValue - 1, date.dayOfMonth)
                }
            }

            val fragment = EventDetailFragment().apply { listener = this@CalendarDetailFragment }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.constraint_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        (binding.recyclerViewEvents.adapter as CustomEventAdapter).setOnEventActionListener(this)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.passCalendarToFragment(thisCalendar!!)
        _binding = null
    }

    override fun onNewEventCreated(event: EventData) {
        viewModel.viewModelScope.launch {

            if (AuthViewModel.getUserId() == thisCalendar!!.owner.email) {
                MainViewModel.addEventToSharedCalendar(event, thisCalendar!!)
            }
            else {
                MainViewModel.addEventToSharedCalendar(event, thisCalendar!!)

            }

            requireActivity().onBackPressedDispatcher.onBackPressed()

        }
    }

    override fun onEditEvent(event: EventData) {
        thisCalendar?.let { calendar ->
            val index = calendar.events.indexOfFirst { it.id == event.id }
            if (index != -1) {
                calendar.events[index] = event
                adapter.updateData(calendar.events.filter { it.startTime.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate() == selectedDate })
                firestoreViewModel.updateCalendar(calendar)
            }

            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onItemRemoved(event: EventData) {
        thisCalendar?.let { calendar ->
            calendar.events.remove(event)
            firestoreViewModel.updateCalendar(calendar)

            val startDate = event.startTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            val endDate = event.endTime.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            var date = startDate
            while (!date.isAfter(endDate)) {
                binding.calendarViewCalendarDetail.notifyDateChanged(date)
                date = date.plusDays(1)
            }
        }
    }

    override fun onItemClicked(event: EventData) {
        val fragment = EventDetailFragment().apply {
            listener = this@CalendarDetailFragment
            this.eventToEdit = event
            this.calendar = thisCalendar!!
        }
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.constraint_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showChooseFriendDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.choose_friend_dialog)
        dialog.findViewById<TextView>(R.id.dialog_message).text = getString(R.string.choose_a_friend_to_add_to_this_calendar)

        val chooseFriendRecyclerView = dialog.findViewById<RecyclerView>(R.id.chooseFriendRecyclerView)
        chooseFriendRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        MainViewModel.getFriends { friendList: MutableList<UserData> ->

            for (user in thisCalendar!!.sharedPeople){
                friendList.remove(user)
            }
            val adapter = CustomUsersAdapter(requireContext() as AppCompatActivity, friendList, this, null,false)
            adapter.setItemClickedPrompt(getString(R.string.add_selected_user_to_shared_calendar))
            chooseFriendRecyclerView.adapter = adapter
            (chooseFriendRecyclerView.adapter as CustomUsersAdapter).notifyDataSetChanged()

            dialog.setCancelable(true)

            dialog.show()
        }
    }

    override fun onUserClickConfirmed(receiverUser: UserData) {
        MainViewModel.viewModelScope.launch {
            firestoreViewModel.addUserToCalendar(receiverUser, thisCalendar!!.id)
            (binding.recyclerViewUsers.adapter as CustomUsersAdapter).addItem(receiverUser)
            binding.recyclerViewUsers.adapter!!.notifyItemInserted(thisCalendar!!.sharedPeopleNumber)
        }
    }


    override fun onDeleteConfirmed(deletedUser: UserData, position: Int) {
        MainViewModel.viewModelScope.launch {
            firestoreViewModel.removeUserFromCalendar(deletedUser.userId, thisCalendar!!.id)
            (binding.recyclerViewUsers.adapter as CustomUsersAdapter).removeItem(deletedUser)
            binding.recyclerViewUsers.adapter!!.notifyItemRemoved(position)
        }
    }
    private fun createMenuToolbar() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.calendar_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.startGroupChatAction -> {
                        Log.d("CalendarDetailFragment", "startGroupChatAction selected")
                        viewModel.viewModelScope.launch {
                            MainViewModel.startGroupChat(thisCalendar!!)
                        }
                        Log.d("CalendarDetailFragment", "startGroupChatAction selected")
                        true
                    }
                    R.id.exportEventsAction -> {
                        showExportDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.CREATED)
    }

    private fun showExportDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.choose_export_dialog)

        dialog.findViewById<ImageButton>(R.id.googleCalendarImageButton).setOnClickListener {
            exportAllEventsToGoogleCalendar()
        }

        dialog.findViewById<ImageButton>(R.id.outlookCalendarImageButton).setOnClickListener {
            acquireOutlookTokenAndExport()
        }

        dialog.show()

    }

    private fun exportAllEventsToGoogleCalendar() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/calendar"))
            .build()

        val client = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun initMSAL() {

        Log.d("MSAL", "MSAL creation started!")
        PublicClientApplication.createSingleAccountPublicClientApplication(
            requireContext(),
            R.raw.msal_config,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    Log.d("MSAL", "MSAL created successfully!")
                    msalApp = application
                    // load current signed-in account (if any)
                    msalApp.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                        override fun onAccountLoaded(activeAccount: com.microsoft.identity.client.IAccount?) {
                            msalAccount = activeAccount
                        }

                        override fun onAccountChanged(priorAccount: com.microsoft.identity.client.IAccount?, currentAccount: com.microsoft.identity.client.IAccount?) {
                            msalAccount = currentAccount
                        }

                        override fun onError(exception: MsalException) {
                            Log.e("MSAL", "Account loading error: ${exception.message}")
                        }
                    })
                }

                override fun onError(exception: MsalException) {
                    Log.e("MSAL", "MSAL init failed", exception)
                }
            }
        )
    }

    private fun acquireOutlookTokenAndExport() {
        val scopes = arrayOf("Calendars.ReadWrite", "User.Read")

        if (!::msalApp.isInitialized) {
            Toast.makeText(requireContext(), "MSAL not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        if (msalAccount == null) {
            Log.d("MSAL", "No account, starting interactive login via signIn()")
            msalApp.signIn(
                requireActivity(),
                null, // optional login hint
                scopes,
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        msalAccount = authenticationResult.account
                        val token = authenticationResult.accessToken
                        lifecycleScope.launch(Dispatchers.IO) {
                            exportViewModel.exportEventsToOutlookGraph(thisCalendar!!.events, token)
                        }
                    }

                    override fun onError(exception: MsalException) {
                        Log.e("MSAL", "SignIn failed: ${exception.message}")
                        Toast.makeText(requireContext(), "Outlook sign-in failed", Toast.LENGTH_SHORT).show()
                    }

                    override fun onCancel() {
                        Log.d("MSAL", "User cancelled login")
                    }
                }
            )
            return
        }

        // Already signed in → acquire token silently
        val authority = msalApp.configuration.defaultAuthority.authorityURL.toString()
        msalApp.acquireTokenSilentAsync(scopes, authority, object : SilentAuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) {
                val token = result.accessToken
                lifecycleScope.launch(Dispatchers.IO) {
                    exportViewModel.exportEventsToOutlookGraph(thisCalendar!!.events, token)
                }
            }

            override fun onError(e: MsalException?) {
                Log.d("MSAL", "Silent failed → interactive: ${e?.message}")
                // fallback to interactive acquireToken
                acquireTokenInteractively(scopes)
            }
        })
    }


    private fun acquireTokenInteractively(scopes: Array<String>) {
        msalApp.acquireToken(
            requireActivity(),
            scopes,
            object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    msalAccount = authenticationResult.account

                    val token = authenticationResult.accessToken
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        exportViewModel.exportEventsToOutlookGraph(thisCalendar!!.events, token)
                    }
                }

                override fun onError(exception: MsalException) {
                    Log.e("MSAL", "Interactive token acquisition failed: ${exception.message}")
                    // Optional: show a Toast
                }

                override fun onCancel() {
                    Log.d("MSAL", "User cancelled interactive login")
                }
            }
        )
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    fun getEventColor(eventId: String): Int {
        val index = (eventId.hashCode() and Int.MAX_VALUE) % eventColors.size
        return eventColors[index]
    }
}