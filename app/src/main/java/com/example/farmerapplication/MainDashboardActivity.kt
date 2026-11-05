package com.example.farmerapplication

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.farmer.FarmerLoginActivity
import com.example.farmerapplication.models.AppVersionResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainDashboardActivity : ComponentActivity() {

    private lateinit var btnFarmerLogin: LinearLayout
    private lateinit var btnOfficialLogin: LinearLayout
    private lateinit var txtAppVersion: TextView

    private var versionCall: Call<AppVersionResponse>? = null
    private var activeDialog: AlertDialog? = null
    private var isVersionVerified = false

    companion object {
        private const val UPDATE_URL = "https://uparjan.jharkhand.gov.in/farmerAppMobileVer.aspx"
        private const val REQUIRED_VERSION = "1.4"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_dashboard)

        btnFarmerLogin = findViewById(R.id.btnFarmerLogin)
        btnOfficialLogin = findViewById(R.id.btnOfficialLogin)
        txtAppVersion = findViewById(R.id.txtAppVersion)

        txtAppVersion.text = "Version : $REQUIRED_VERSION"

        btnFarmerLogin.setOnClickListener {
            if (isVersionVerified) startActivity(Intent(this, FarmerLoginActivity::class.java))
        }
        btnOfficialLogin.setOnClickListener {
            if (isVersionVerified) startActivity(Intent(this, OfficialLoginActivity::class.java))
        }

        // Block the login buttons until the version is verified
        setButtonsEnabled(false)
        checkAppVersion()
    }

    override fun onResume() {
        super.onResume()
        // Re-check if the user comes back unverified (e.g. returning from the browser)
        if (!isVersionVerified && activeDialog?.isShowing != true && versionCall?.isExecuted != true) {
            checkAppVersion()
        }
    }

    override fun onDestroy() {
        versionCall?.cancel()
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroy()
    }

    // ---------------- Version check ----------------

    private fun checkAppVersion() {
        versionCall?.cancel()
        val call = RetrofitClient.apiService.getApiVersion()
        versionCall = call

        call.enqueue(object : Callback<AppVersionResponse> {
            override fun onResponse(
                call: Call<AppVersionResponse>,
                response: Response<AppVersionResponse>
            ) {
                if (isFinishing || isDestroyed) return

                val body = response.body()
                val serverVersion = body?.version?.trim()

                if (response.isSuccessful && body?.statusCode == 200 && !serverVersion.isNullOrEmpty()) {
                    if (serverVersion == REQUIRED_VERSION) {
                        isVersionVerified = true
                        setButtonsEnabled(true)
                    } else {
                        showUpdateDialog()
                    }
                } else {
                    showVerifyFailedDialog()
                }
            }

            override fun onFailure(call: Call<AppVersionResponse>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                showVerifyFailedDialog()
            }
        })
    }

    private fun getInstalledVersion(): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            info.versionName ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }

    // ---------------- Dialogs ----------------

    /** Non-dismissable "update required" dialog. */
    private fun showUpdateDialog() {
        activeDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Update Required")
            .setMessage("A newer version of the app is available. Please download the latest version to continue.")
            .setCancelable(false)
            .setPositiveButton("OK", null) // overridden below so the dialog stays open
            .setNegativeButton("Exit") { _, _ -> finishAffinity() }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { openUpdatePage() }
        }
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        activeDialog = dialog
    }

    /** Shown when the version API fails; we don't let the user in without verification. */
    private fun showVerifyFailedDialog() {
        activeDialog?.dismiss()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Unable to Verify Version")
            .setMessage("Please check your internet connection and try again.")
            .setCancelable(false)
            .setPositiveButton("Retry") { d, _ ->
                d.dismiss()
                checkAppVersion()
            }
            .setNegativeButton("Exit") { _, _ -> finishAffinity() }
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        activeDialog = dialog
    }

    private fun openUpdatePage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_URL)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser found to open the download page", Toast.LENGTH_LONG).show()
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnFarmerLogin.isEnabled = enabled
        btnOfficialLogin.isEnabled = enabled
        val alpha = if (enabled) 1f else 0.5f
        btnFarmerLogin.alpha = alpha
        btnOfficialLogin.alpha = alpha
    }
}