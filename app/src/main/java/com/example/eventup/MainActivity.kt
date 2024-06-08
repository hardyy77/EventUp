package com.example.eventup

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()

        val fragmentTitle = findViewById<TextView>(R.id.fragment_title)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            var title: String? = null
            when (item.itemId) {
                R.id.navigation_home -> {
                    selectedFragment = HomeFragment()
                    title = "Główna"
                }
                R.id.navigation_search -> {
                    selectedFragment = SearchFragment()
                    title = "Szukaj"
                }
                R.id.navigation_favorites -> {
                    selectedFragment = FavoritesFragment()
                    title = "Ulubione"
                }
                R.id.navigation_profile -> {
                    selectedFragment = ProfileFragment()
                    title = "Profil"
                }
            }
            if (selectedFragment != null && title != null) {
                supportFragmentManager.beginTransaction().replace(R.id.container, selectedFragment).commit()
                fragmentTitle.text = title
            }
            true
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.container, HomeFragment()).commit()
            fragmentTitle.text = getString(R.string.default_fragment_title)
            bottomNavigation.selectedItemId = R.id.navigation_home
        }

        // Planowanie zadania aktualizacji wydarzeń o 7 rano codziennie
        val updateRequest = PeriodicWorkRequestBuilder<UpdateInterestingEventsWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueue(updateRequest)
    }

    private fun calculateInitialDelay(): Long {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (now.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        return targetTime.timeInMillis - now.timeInMillis
    }

    // Przykładowa funkcja zapisywania danych do Firestore
    private fun saveEvent(name: String, location: String, date: String) {
        val event = hashMapOf(
            "name" to name,
            "location" to location,
            "date" to date
        )
        firestore.collection("events")
            .add(event)
            .addOnSuccessListener { documentReference ->
                println("Event added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding event: $e")
            }
    }

    // Przykładowa funkcja pobierania danych z Firestore
    private fun getEvents() {
        firestore.collection("events")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    println("${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting events: $exception")
            }
    }
}
