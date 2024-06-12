package com.example.eventup.fragments

import android.content.Intent
import android.os.Bundle
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

    private lateinit var userEmailTextView: TextView
    private lateinit var userUidTextView: TextView
    private lateinit var loginLogoutButton: Button
    private lateinit var addEventButton: Button
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>
    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        userEmailTextView = view.findViewById(R.id.userEmailTextView)
        userUidTextView = view.findViewById(R.id.userUidTextView)
        loginLogoutButton = view.findViewById(R.id.loginLogoutButton)
        addEventButton = view.findViewById(R.id.addEventButton)

        loginActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                checkIfUserLoggedIn()
            }
        }

        loginLogoutButton.setOnClickListener {
            if (currentUser != null) {
                UserUtils.logoutUser()
                updateUI(null)
            } else {
                val intent = Intent(activity, LoginActivity::class.java)
                loginActivityResultLauncher.launch(intent)
            }
        }

        addEventButton.setOnClickListener {
            startActivity(Intent(activity, ManageEventActivity::class.java))
        }

        checkIfUserLoggedIn()

        return view
    }

    private fun checkIfUserLoggedIn() {
        currentUser = UserUtils.getCurrentUser()
        updateUI(currentUser)
    }

    private fun updateUI(user: User?) {
        if (user != null) {
            userEmailTextView.text = "Email: ${user.email}"
            userUidTextView.text = "UID: ${user.id}"
            userEmailTextView.visibility = View.VISIBLE
            userUidTextView.visibility = View.VISIBLE
            loginLogoutButton.text = "Logout"
            loginLogoutButton.visibility = View.VISIBLE
            user.id?.let { fetchUserRole(it) }
        } else {
            userEmailTextView.visibility = View.GONE
            userUidTextView.visibility = View.GONE
            loginLogoutButton.text = "Login"
            loginLogoutButton.visibility = View.VISIBLE
            addEventButton.visibility = View.GONE
        }
    }

    private fun fetchUserRole(userId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val query = "SELECT role FROM users WHERE id = $userId"
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
        }
    }
}
