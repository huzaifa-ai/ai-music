package com.holisticapp.fitness.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.holisticapp.fitness.R
import com.holisticapp.fitness.ui.facial.FacialExpressionActivity
import com.holisticapp.fitness.ui.music.MusicPlayerActivity

class HomeFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCardClickListeners(view)
    }
    
    private fun setupCardClickListeners(view: View) {
        val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Assessment Card - Navigate to Assessment tab
        view.findViewById<CardView>(R.id.assessmentCard)?.setOnClickListener {
            bottomNav?.selectedItemId = R.id.nav_assessment
        }
        
        // Charts Card - Navigate to Progress tab
        view.findViewById<CardView>(R.id.chartsCard)?.setOnClickListener {
            bottomNav?.selectedItemId = R.id.nav_progress
        }
        
        // Facial Expression Card - Launch Facial Expression Activity
        view.findViewById<CardView>(R.id.facialCard)?.setOnClickListener {
            launchFacialExpressionAnalysis()
        }
        
        // Music Cards - Launch Music Player Activity
        view.findViewById<CardView>(R.id.musicCard)?.setOnClickListener {
            launchMusicPlayer()
        }
        
        view.findViewById<CardView>(R.id.musicHealthCard)?.setOnClickListener {
            launchMusicPlayer()
        }
        
        // Workout Card - Navigate to Progress tab
        view.findViewById<CardView>(R.id.workoutCard)?.setOnClickListener {
            bottomNav?.selectedItemId = R.id.nav_progress
        }
        
        // Weather Card - Navigate to Progress tab
        view.findViewById<CardView>(R.id.weatherCard)?.setOnClickListener {
            bottomNav?.selectedItemId = R.id.nav_progress
        }
        
        // Journaling Card - Navigate to Assessment tab
        view.findViewById<CardView>(R.id.journalingCard)?.setOnClickListener {
            bottomNav?.selectedItemId = R.id.nav_assessment
        }
    }
    
    private fun launchMusicPlayer() {
        val intent = Intent(requireContext(), MusicPlayerActivity::class.java)
        startActivity(intent)
    }
    
    private fun launchFacialExpressionAnalysis() {
        val intent = Intent(requireContext(), FacialExpressionActivity::class.java)
        startActivity(intent)
    }
} 