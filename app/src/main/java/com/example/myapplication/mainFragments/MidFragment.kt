package com.example.myapplication.mainFragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.authentication.LoginActivity
import com.example.myapplication.authentication.UserPreferences
import com.example.myapplication.chat.ChatActivity
import com.example.myapplication.databinding.MidFragmentBinding
import com.google.firebase.auth.FirebaseAuth

class MidFragment : Fragment() {
    private var _binding: MidFragmentBinding? = null
    private lateinit var logoutButton: Button
    private lateinit var currentUserTextView: TextView
    private lateinit var openChatButton: Button

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.mid_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val auth = FirebaseAuth.getInstance()

        logoutButton = view.findViewById(R.id.buttonLogOut)
        currentUserTextView = view.findViewById(R.id.textViewCurrentUser)
        openChatButton = view.findViewById(R.id.openChatButton)

        if (auth.currentUser != null) {
            currentUserTextView.text = auth.currentUser!!.email
        }
        else {
            currentUserTextView.text = getString(R.string.not_logged_in)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            UserPreferences.logoutUser(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        openChatButton.setOnClickListener{
            val intent = Intent(requireContext(), ChatActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}