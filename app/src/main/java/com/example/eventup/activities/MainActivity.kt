package com.example.eventup.activities

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.eventup.utils.NavigationManager
import com.example.eventup.R
import com.example.eventup.utils.TaskScheduler
import com.example.eventup.utils.DatabaseHandler
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Funkcja wywoływana przy tworzeniu aktywności.
    // Odpowiada za inicjalizację widoku oraz ustawienie dolnej nawigacji.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // Inicjalizacja DatabaseHandler, aby zapewnić nawiązanie połączenia z bazą danych
            DatabaseHandler.init()
            Log.d("MainActivity", "DatabaseHandler initialized successfully")

            val fragmentTitle = findViewById<TextView>(R.id.fragment_title)
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

            // Ustawienie dolnej nawigacji
            NavigationManager.setupBottomNavigation(
                supportFragmentManager,
                bottomNavigation,
                fragmentTitle
            )
            Log.d("MainActivity", "Bottom navigation setup successfully")

            // Harmonogramowanie codziennych zadań
            TaskScheduler.scheduleDailyTask(this)
            Log.d("MainActivity", "Daily tasks scheduled successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization: ${e.message}", e)
        }
    }
}
