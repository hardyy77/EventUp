package com.example.eventup.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.utils.NavigationManager
import com.example.eventup.R
import com.example.eventup.utils.TaskScheduler
import com.example.eventup.utils.DatabaseHandler
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize DatabaseHandler to ensure connection is established
        DatabaseHandler.init()

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
