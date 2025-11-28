package com.taskraze.myapplication.view.home

import AuthViewModelFactory
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.taskraze.myapplication.R
import com.taskraze.myapplication.view.auth.LoginActivity
import com.taskraze.myapplication.common.UserPreferences
import com.taskraze.myapplication.view.chat.StartChatActivity
import com.taskraze.myapplication.view.friends.FriendsActivity
import com.taskraze.myapplication.viewmodel.MainViewModel
import com.taskraze.myapplication.databinding.HomeFragmentBinding
import com.taskraze.myapplication.model.calendar.UserData
import com.taskraze.myapplication.model.home.InternetConnectivityCallback
import com.taskraze.myapplication.viewmodel.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.taskraze.myapplication.view.overlay_widget.OverlayService
import com.taskraze.myapplication.viewmodel.MainViewModelFactory
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModel
import com.taskraze.myapplication.viewmodel.recommendation.RecommendationsViewModelFactory

class HomeFragment : Fragment() {
    private var _binding: HomeFragmentBinding? = null
    private lateinit var logoutButton: Button
    private lateinit var currentUserTextView: TextView
    private lateinit var openChatButton: Button
    private lateinit var openFriendsButton: Button
    private lateinit var overlayMenuSwitch: SwitchCompat
    private lateinit var recommendationsSwitch: SwitchCompat
    private lateinit var suggestedEventsRecyclerView: RecyclerView
    private lateinit var connectivityCallback: InternetConnectivityCallback
    private lateinit var authViewModel: AuthViewModel
    private lateinit var viewModel: MainViewModel
    private lateinit var recommendationsViewModel: RecommendationsViewModel

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel = ViewModelProvider(
            requireActivity(),
            AuthViewModelFactory(requireActivity())
        )[AuthViewModel::class.java]

        recommendationsViewModel = ViewModelProvider(
            requireActivity(),
            RecommendationsViewModelFactory(authViewModel.getUserId())
        )[RecommendationsViewModel::class.java]

        val factory = MainViewModelFactory(authViewModel.getUserId(), authViewModel.loggedInUser.value!!)
        viewModel = ViewModelProvider(requireActivity(), factory)[MainViewModel::class.java]

        // check for internet connection
        if (!isInternetAvailable(requireContext())) {
            binding.noInternetTextView.visibility = TextView.VISIBLE
        }
        connectivityCallback = InternetConnectivityCallback.registerConnectivityCallback(requireContext()) { isConnected ->
            lifecycleScope.launch(Main) {

                if (isConnected) {
                    updateInternetConnection(true)
                } else {
                    updateInternetConnection(false)
                }
            }

        }

        logoutButton = binding.buttonLogOut
        currentUserTextView = binding.textViewCurrentUser
        openChatButton = binding.openChatButton
        openFriendsButton = binding.openFriendsButton
        suggestedEventsRecyclerView = binding.suggestedEventsRecyclerView
        overlayMenuSwitch = binding.switchOverlayMenu
        recommendationsSwitch = binding.switchRecommendations

        lifecycleScope.launch {
            authViewModel.loggedInUser.collect { user ->
                currentUserTextView.text = user?.username ?: getString(R.string.not_logged_in)
            }
        }

        logoutButton.setOnClickListener {
            authViewModel.loggedInUser.value = UserData("","empty", "empty")
            viewModel.auth.signOut()
            UserPreferences.logoutUser(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        openChatButton.setOnClickListener{
            val intent = Intent(requireContext(), StartChatActivity::class.java)
            startActivity(intent)
        }

        openFriendsButton.setOnClickListener{
            val intent = Intent(requireContext(), FriendsActivity::class.java)
            startActivity(intent)
        }

        val isEnabled = UserPreferences.isOverlayEnabled(requireContext())
        overlayMenuSwitch.isChecked = isEnabled

        if (isEnabled && Settings.canDrawOverlays(requireContext())) {
            startOverlayService()
        }

        overlayMenuSwitch.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setOverlayEnabled(requireContext(), isChecked)

            if (isChecked) {
                if (Settings.canDrawOverlays(requireContext())) {
                    startOverlayService()
                } else {
                    requestOverlayPermission()
                    overlayMenuSwitch.isChecked = false
                    UserPreferences.setOverlayEnabled(requireContext(), false)
                }
            } else {
                stopOverlayService()
            }
        }

        recommendationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            UserPreferences.setRecommendationsEnabled(requireContext(), isChecked)

            if (isChecked) {
                lifecycleScope.launch {
                    val recommendedTags = recommendationsViewModel.getRecommendedItems()
                    Log.d("HomeFragmentasdf", "Recommended tags: $recommendedTags")
                }
            }
        }

    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${requireContext().packageName}".toUri()
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(requireContext(), OverlayService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(requireContext(), OverlayService::class.java)
        requireContext().stopService(intent)
    }

    override fun onResume() {
        super.onResume()

        if (overlayMenuSwitch.isChecked && Settings.canDrawOverlays(requireContext())) {
            startOverlayService()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        InternetConnectivityCallback.unregisterConnectivityCallback(requireContext(), connectivityCallback)

    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

            else -> false
        }
    }

    private fun updateInternetConnection(isConnected: Boolean) {
        if (isConnected) {
            binding.noInternetTextView.visibility = TextView.GONE
        }
        else {
            binding.noInternetTextView.visibility = TextView.VISIBLE
        }
    }
}