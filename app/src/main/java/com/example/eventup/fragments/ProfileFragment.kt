package com.example.eventup.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.eventup.R
import com.example.eventup.activities.LoginActivity
import com.example.eventup.activities.ManageEventActivity
import com.example.eventup.models.User
import com.example.eventup.utils.DatabaseHandler
import com.example.eventup.utils.UserUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    // Deklaracja widoków i zmiennych używanych w fragmencie
    private lateinit var userEmailTextView: TextView
    private lateinit var userUidTextView: TextView
    private lateinit var loginLogoutButton: Button
    private lateinit var addEventButton: Button
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    private var currentUser: User? = null
    private lateinit var loginReceiver: BroadcastReceiver

    // Funkcja tworząca widok fragmentu
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Inicjalizacja widoków
        userEmailTextView = view.findViewById(R.id.userEmailTextView)
        userUidTextView = view.findViewById(R.id.userUidTextView)
        loginLogoutButton = view.findViewById(R.id.loginLogoutButton)
        addEventButton = view.findViewById(R.id.addEventButton)

        // Inicjalizacja rejestratora aktywności dla wyniku logowania
        loginActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                checkIfUserLoggedIn()
            }
        }

        // Ustawienie akcji dla przycisku logowania/wylogowania
        loginLogoutButton.setOnClickListener {
            if (currentUser != null) {
                UserUtils.logoutUser()
                updateUI(null)
            } else {
                val intent = Intent(activity, LoginActivity::class.java)
                loginActivityResultLauncher.launch(intent)
            }
        }

        // Ustawienie akcji dla przycisku dodawania wydarzenia
        addEventButton.setOnClickListener {
            startActivity(Intent(activity, ManageEventActivity::class.java))
        }

        // Sprawdzenie, czy użytkownik jest zalogowany
        checkIfUserLoggedIn()

        return view
    }

    // Rejestracja odbiornika broadcastu po rozpoczęciu fragmentu
    override fun onStart() {
        super.onStart()
        loginReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                checkIfUserLoggedIn()
            }
        }
        activity?.registerReceiver(loginReceiver, IntentFilter("com.example.eventup.LOGIN_SUCCESS"))
    }

    // Wyrejestrowanie odbiornika broadcastu po zatrzymaniu fragmentu
    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(loginReceiver)
    }

    // Funkcja sprawdzająca, czy użytkownik jest zalogowany
    private fun checkIfUserLoggedIn() {
        currentUser = UserUtils.getCurrentUser()
        Log.d("ProfileFragment", "Check if user logged in: $currentUser")
        updateUI(currentUser)
    }

    // Funkcja aktualizująca interfejs użytkownika w zależności od stanu zalogowania
    private fun updateUI(user: User?) {
        if (user != null) {
            userEmailTextView.text = "Email: ${user.email}"
            userUidTextView.text = "UID: ${user.uid}"
            userEmailTextView.visibility = View.VISIBLE
            userUidTextView.visibility = View.VISIBLE
            loginLogoutButton.text = "Wyloguj"
            loginLogoutButton.visibility = View.VISIBLE
            fetchUserRole(user.uid)
        } else {
            userEmailTextView.visibility = View.GONE
            userUidTextView.visibility = View.GONE
            loginLogoutButton.text = "Zaloguj"
            loginLogoutButton.visibility = View.VISIBLE
            addEventButton.visibility = View.GONE
            loginLogoutButton.setOnClickListener {
                val intent = Intent(activity, LoginActivity::class.java)
                loginActivityResultLauncher.launch(intent)
            }
        }
    }

    // Funkcja pobierająca rolę użytkownika z bazy danych
    private fun fetchUserRole(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val query = "SELECT role FROM users WHERE uid = '$userId'"
                val resultSet = withContext(Dispatchers.IO) {
                    DatabaseHandler.executeQuery(query)
                }
                if (resultSet?.next() == true) {
                    val role = resultSet.getString("role")
                    if (role == "admin") {
                        addEventButton.visibility = View.VISIBLE
                    } else {
                        addEventButton.visibility = View.GONE
                    }
                } else {
                    addEventButton.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Failed to fetch user role: ${e.message}", e)
            }
        }
    }
}
