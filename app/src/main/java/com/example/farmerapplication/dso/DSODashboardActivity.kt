package com.example.farmerapplication.dso

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.farmerapplication.R
import com.example.farmerapplication.MainDashboardActivity  // adjust package if different

class DSODashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dso_dashboard_main)

        val prefs = getSharedPreferences("dso_prefs", MODE_PRIVATE)
        val officerName = intent.getStringExtra("officer_name")
            ?: prefs.getString("officer_name", null)
            ?: "DSO"

        findViewById<TextView>(R.id.txtWelcomeDSO).text = officerName

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }

        findViewById<ImageView>(R.id.btnProfile).setOnClickListener {
            // TODO: open DSO profile screen
        }

        setupBackPressHandler()
    }

    // Logout confirmation dialog
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ -> logoutDSO() }
            .show()
    }

    private fun logoutDSO() {
        // Clear saved session (token, user info)
        getSharedPreferences("dso_prefs", MODE_PRIVATE).edit().clear().apply()

        val loginIntent = Intent(this, MainDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish()
    }

    // Intercept system back press to trigger logout confirmation
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showLogoutConfirmation()
                }
            }
        )
    }
}