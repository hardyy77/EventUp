// app/src/main/java/com/example/eventup/fragments/ProfileFragment.kt
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userEmailTextView: TextView
    private lateinit var userUidTextView: TextView
    private lateinit var loginLogoutButton: Button
    private lateinit var loginActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        auth = FirebaseAuth.getInstance()
        userEmailTextView = view.findViewById(R.id.userEmailTextView)
        userUidTextView = view.findViewById(R.id.userUidTextView)
        loginLogoutButton = view.findViewById(R.id.loginLogoutButton)

        loginActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                checkIfUserLoggedIn()
            }
        }

        loginLogoutButton.setOnClickListener {
            if (auth.currentUser != null) {
                auth.signOut()
                updateUI(null)
            } else {
                val intent = Intent(activity, LoginActivity::class.java)
                loginActivityResultLauncher.launch(intent)
            }
        }

        checkIfUserLoggedIn()

        return view
    }

    private fun checkIfUserLoggedIn() {
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            userEmailTextView.text = "Email: ${user.email}"
            userUidTextView.text = "UID: ${user.uid}"
            userEmailTextView.visibility = View.VISIBLE
            userUidTextView.visibility = View.VISIBLE
            loginLogoutButton.text = "Logout"
            loginLogoutButton.visibility = View.VISIBLE
        } else {
            userEmailTextView.visibility = View.GONE
            userUidTextView.visibility = View.GONE
            loginLogoutButton.text = "Login"
            loginLogoutButton.visibility = View.VISIBLE
        }
    }
}
