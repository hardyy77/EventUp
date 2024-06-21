package com.example.eventup.utils

import android.util.Log
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.eventup.R
import com.example.eventup.fragments.FavoritesFragment
import com.example.eventup.fragments.HomeFragment
import com.example.eventup.fragments.ProfileFragment
import com.example.eventup.fragments.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationManager {

    // Funkcja konfigurująca dolną nawigację
    fun setupBottomNavigation(
        fragmentManager: FragmentManager,
        bottomNavigationView: BottomNavigationView,
        fragmentTitle: TextView
    ) {
        bottomNavigationView.setOnItemSelectedListener { item ->
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
                else -> {
                    Log.e("NavigationManager", "Unknown navigation item selected: ${item.itemId}")
                }
            }
            if (selectedFragment != null && title != null) {
                fragmentManager.beginTransaction().replace(R.id.container, selectedFragment).commit()
                fragmentTitle.text = title
                Log.d("NavigationManager", "Switched to fragment: $title")
            }
            true
        }

        // Inicjalizacja nawigacji z domyślnym fragmentem
        if (fragmentManager.findFragmentById(R.id.container) == null) {
            fragmentManager.beginTransaction().replace(R.id.container, HomeFragment()).commit()
            fragmentTitle.text = "Główna"
            bottomNavigationView.selectedItemId = R.id.navigation_home
            Log.d("NavigationManager", "Initialized with HomeFragment")
        }
    }
}
