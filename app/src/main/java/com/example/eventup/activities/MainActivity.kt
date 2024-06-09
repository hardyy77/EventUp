package com.example.eventup.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.utils.NavigationManager
import com.example.eventup.R
import com.example.eventup.utils.TaskScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()

        val fragmentTitle = findViewById<TextView>(R.id.fragment_title)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        NavigationManager.setupBottomNavigation(
            supportFragmentManager,
            bottomNavigation,
            fragmentTitle
        )
        TaskScheduler.scheduleDailyTask(this)
    }
}
