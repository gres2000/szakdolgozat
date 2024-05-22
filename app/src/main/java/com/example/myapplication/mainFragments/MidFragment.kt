package com.example.myapplication.mainFragments

import com.example.myapplication.common.InternetConnectivityCallback
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.authentication.LoginActivity
import com.example.myapplication.authentication.UserPreferences
import com.example.myapplication.chat.StartChatActivity
import com.example.myapplication.databinding.MidFragmentBinding
import com.example.myapplication.friends.FriendsActivity
import com.example.myapplication.viewModel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MidFragment : Fragment() {
    private var _binding: MidFragmentBinding? = null
    private lateinit var logoutButton: Button
    private lateinit var currentUserTextView: TextView
    private lateinit var openChatButton: Button
    private lateinit var openFriendsButton: Button
    private lateinit var suggestedEventsRecyclerView: RecyclerView
    private lateinit var sendButton: FloatingActionButton
    private lateinit var connectivityCallback: InternetConnectivityCallback

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MidFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //check for internet connection
        if (!isInternetAvailable(requireContext())) {
            binding.noInternetTextView.visibility = TextView.VISIBLE
        }
        connectivityCallback = InternetConnectivityCallback.registerConnectivityCallback(requireContext()) { isConnected ->
            lifecycleScope.launch(Main) {

                if (isConnected) {
                    updateUI(true)
                } else {
                    updateUI(false)
                }
            }

        }

        logoutButton = view.findViewById(R.id.buttonLogOut)
        currentUserTextView = view.findViewById(R.id.textViewCurrentUser)
        openChatButton = view.findViewById(R.id.openChatButton)
        openFriendsButton = view.findViewById(R.id.openFriendsButton)
        suggestedEventsRecyclerView = view.findViewById(R.id.suggestedEventsRecyclerView)
        sendButton = view.findViewById(R.id.fab_send_request)

        if (MainViewModel.auth.currentUser != null) {
            currentUserTextView.text = MainViewModel.auth.currentUser!!.email
        }
        else {
            currentUserTextView.text = getString(R.string.not_logged_in)
        }

        logoutButton.setOnClickListener {
            MainViewModel.loggedInUser = null
            MainViewModel.auth.signOut()
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

        sendButton.setOnClickListener{
            lifecycleScope.launch {
                val client = HttpClient(CIO)
                val response: HttpResponse = client.get("https://newsapi.org/v2/top-headlines?country=hu&apiKey=56fa83c71bf84433a94e739663d52bac")
                val body: String = response.body()
                Log.d("adatok", body)

            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()

        InternetConnectivityCallback.unregisterConnectivityCallback(requireContext(), connectivityCallback)

    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun updateUI(isConnected: Boolean) {
        if (isConnected) {
            binding.noInternetTextView.visibility = TextView.GONE
        }
        else {
            binding.noInternetTextView.visibility = TextView.VISIBLE
        }
    }
}