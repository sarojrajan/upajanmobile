package com.example.farmerapplication.miller

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.farmerapplication.MainDashboardActivity
import com.example.farmerapplication.R

class MillerDashboardActivity : ComponentActivity() {

    companion object {
        // Intent Keys
        const val EXTRA_MILLER_ID = "Miller_id"
        const val EXTRA_MILLER_NAME = "Miller_name"
        const val EXTRA_DISTRICT_NAME = "District_Name"
        const val EXTRA_EMAIL_ID = "email_id"
        const val EXTRA_MOBILE_NO = "mobile_no"
        const val EXTRA_TINNO = "TINNO"
        const val EXTRA_PAN = "PAN"
        const val EXTRA_LICENSE_NO = "LicenseNO"
        const val EXTRA_BANK = "Bank"
        const val EXTRA_BRANCH_NAME = "BranchName"
        const val EXTRA_ACCOUNT_NO = "AccountNo"
        const val EXTRA_IFSC_CODE = "IFSCCode"
        const val EXTRA_MILLER_HEAD_NAME = "miller_head_name"
        const val EXTRA_MT_PER_HOUR = "mt_per_hour"
        const val EXTRA_LOGIN_MESSAGE = "login_message"
    }

    private lateinit var txtWelcomeMiller: TextView
    private lateinit var btnLogout: ImageView
    private lateinit var btnViewProfile: LinearLayout
    private lateinit var btnAdvanceCMR: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.miller_dashboard_main)

        // Initialize views
        txtWelcomeMiller = findViewById(R.id.txtWelcomeMiller)
        btnLogout = findViewById(R.id.btnLogout)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        btnAdvanceCMR = findViewById(R.id.btnAdvanceCMR)

        // Display Miller Name
        val millerName = intent.getStringExtra(EXTRA_MILLER_NAME).orEmpty()
        txtWelcomeMiller.text = millerName.ifBlank { "Miller User Name" }

        // Display login success message for 5 seconds
        val loginMessage = intent.getStringExtra(EXTRA_LOGIN_MESSAGE)
        if (!loginMessage.isNullOrBlank()) {
            val loginToast = Toast.makeText(this@MillerDashboardActivity, loginMessage, Toast.LENGTH_LONG)
            loginToast.show()
            Handler(Looper.getMainLooper()).postDelayed({ loginToast.cancel() }, 5000L)
        }

        // Click listeners
        btnViewProfile.setOnClickListener { openMillerProfile() }
        btnLogout.setOnClickListener { showLogoutConfirmation() }
        btnAdvanceCMR.setOnClickListener { openAdvanceCMR() }

        // Setup custom back press handler
        setupBackPressHandler()
    }

    // Open Miller Profile screen and forward all details
    private fun openMillerProfile() {
        val profileIntent = Intent(this@MillerDashboardActivity, MillerProfileActivity::class.java).apply {
            putExtra(EXTRA_MILLER_ID, intent.getIntExtra(EXTRA_MILLER_ID, 0))
            putExtra(EXTRA_MILLER_NAME, intent.getStringExtra(EXTRA_MILLER_NAME).orEmpty())
            putExtra(EXTRA_DISTRICT_NAME, intent.getStringExtra(EXTRA_DISTRICT_NAME).orEmpty())
            putExtra(EXTRA_EMAIL_ID, intent.getStringExtra(EXTRA_EMAIL_ID).orEmpty())
            putExtra(EXTRA_MOBILE_NO, intent.getStringExtra(EXTRA_MOBILE_NO).orEmpty())
            putExtra(EXTRA_TINNO, intent.getStringExtra(EXTRA_TINNO).orEmpty())
            putExtra(EXTRA_PAN, intent.getStringExtra(EXTRA_PAN).orEmpty())
            putExtra(EXTRA_LICENSE_NO, intent.getStringExtra(EXTRA_LICENSE_NO).orEmpty())
            putExtra(EXTRA_BANK, intent.getStringExtra(EXTRA_BANK).orEmpty())
            putExtra(EXTRA_BRANCH_NAME, intent.getStringExtra(EXTRA_BRANCH_NAME).orEmpty())
            putExtra(EXTRA_ACCOUNT_NO, intent.getStringExtra(EXTRA_ACCOUNT_NO).orEmpty())
            putExtra(EXTRA_IFSC_CODE, intent.getStringExtra(EXTRA_IFSC_CODE).orEmpty())
            putExtra(EXTRA_MILLER_HEAD_NAME, intent.getStringExtra(EXTRA_MILLER_HEAD_NAME).orEmpty())
            putExtra(EXTRA_MT_PER_HOUR, intent.getIntExtra(EXTRA_MT_PER_HOUR, 0))
        }
        startActivity(profileIntent)
    }

    // Logout confirmation dialog
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setIcon(R.drawable.ic_logout)
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ -> logoutMiller() }
            .show()
    }

    // Clear task stack and return to Main Dashboard
    private fun logoutMiller() {
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

    private fun showFeatureComingSoon() {
        Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun openAdvanceCMR() {
        val advanceCMRIntent = Intent(this@MillerDashboardActivity, AdvanceCMRActivity::class.java).apply {
            putExtra(EXTRA_MILLER_ID, intent.getIntExtra(EXTRA_MILLER_ID, 0))
        }
        startActivity(advanceCMRIntent)
    }
}