package com.holisticapp.fitness.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.holisticapp.fitness.R
import com.holisticapp.fitness.ui.auth.AuthActivity

class ProfileFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        setupLogoutButton(view)
        
        return view
    }
    
    private fun setupLogoutButton(view: View) {
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        logoutButton?.setOnClickListener {
            // Show confirmation toast
            Toast.makeText(requireContext(), "Logging out...", Toast.LENGTH_SHORT).show()
            
            // Navigate to AuthActivity
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            
            // Finish the current activity
            requireActivity().finish()
        }
    }
} 