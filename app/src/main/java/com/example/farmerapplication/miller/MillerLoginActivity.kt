package com.example.farmerapplication.miller

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.miller.MillerLoginRequest
import com.example.farmerapplication.models.miller.MillerLoginResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MillerLoginActivity : ComponentActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnLogin: Button

    private var loginApiCall: Call<MillerLoginResponse>? = null
    private var isLoginInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.miller_login_dashboard)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            hideKeyboard()
            if (validateLoginFields()) {
                performMillerLogin()
            }
        }

        // Trigger login directly on keyboard action
        editTextPassword.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            if (validateLoginFields()) {
                performMillerLogin()
            }
            true
        }
    }

    private fun validateLoginFields(): Boolean {
        val username = editTextUsername.text.toString().trim()
        val password = editTextPassword.text.toString()

        editTextUsername.error = null
        editTextPassword.error = null

        if (username.isEmpty()) {
            editTextUsername.error = "Please enter Email-Id/Mobile no."
            editTextUsername.requestFocus()
            return false
        }

        if (password.isBlank()) {
            editTextPassword.error = "Please enter Password"
            editTextPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performMillerLogin() {
        if (isLoginInProgress) return

        val emailOrMobile = editTextUsername.text.toString().trim()
        val password = editTextPassword.text.toString()

        val request = MillerLoginRequest(
            email_id = emailOrMobile,
            password = password
        )

        setLoginLoading(true)

        loginApiCall = RetrofitClient.apiService.millerLogin(request)
        loginApiCall?.enqueue(object : Callback<MillerLoginResponse> {
            override fun onResponse(call: Call<MillerLoginResponse>, response: Response<MillerLoginResponse>) {
                if (isFinishing || isDestroyed) return

                setLoginLoading(false)
                val responseBody = response.body()

                if (response.isSuccessful && responseBody?.status_code == 200) {
                    handleSuccessfulLogin(responseBody)
                } else {
                    val errorResponse = parseErrorResponse(response)
                    val message = errorResponse?.message?.takeIf { it.isNotBlank() }
                        ?: responseBody?.message?.takeIf { it.isNotBlank() }
                        ?: when (response.code()) {
                            400 -> "Invalid login request."
                            401 -> "Invalid email/mobile or password."
                            403 -> "You are not authorized to log in."
                            404 -> "Miller login service was not found."
                            408 -> "The login request timed out."
                            429 -> "Too many login attempts. Please try again later."
                            in 500..599 -> "Server error occurred. Please try again."
                            else -> "Unable to complete Miller login."
                        }

                    Toast.makeText(this@MillerLoginActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<MillerLoginResponse>, throwable: Throwable) {
                if (isFinishing || isDestroyed || call.isCanceled) return

                setLoginLoading(false)
                val message = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection and API tunnel."
                    else -> throwable.localizedMessage ?: "Something went wrong during Miller login."
                }

                Toast.makeText(this@MillerLoginActivity, message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleSuccessfulLogin(response: MillerLoginResponse) {
        val dashboardIntent = Intent(this, MillerDashboardActivity::class.java).apply {
            putExtra(MillerDashboardActivity.EXTRA_MILLER_ID, response.Miller_id ?: 0)
            putExtra(MillerDashboardActivity.EXTRA_MILLER_NAME, response.Miller_name ?: "")
            putExtra(MillerDashboardActivity.EXTRA_DISTRICT_NAME, response.District_Name ?: "")
            putExtra(MillerDashboardActivity.EXTRA_EMAIL_ID, response.email_id ?: "")
            putExtra(MillerDashboardActivity.EXTRA_MOBILE_NO, response.mobile_no ?: "")
            putExtra(MillerDashboardActivity.EXTRA_TINNO, response.TINNO ?: "")
            putExtra(MillerDashboardActivity.EXTRA_PAN, response.PAN ?: "")
            putExtra(MillerDashboardActivity.EXTRA_LICENSE_NO, response.LicenseNO ?: "")
            putExtra(MillerDashboardActivity.EXTRA_BANK, response.Bank ?: "")
            putExtra(MillerDashboardActivity.EXTRA_BRANCH_NAME, response.BranchName ?: "")
            putExtra(MillerDashboardActivity.EXTRA_ACCOUNT_NO, response.AccountNo ?: "")
            putExtra(MillerDashboardActivity.EXTRA_IFSC_CODE, response.IFSCCode ?: "")
            putExtra(MillerDashboardActivity.EXTRA_MILLER_HEAD_NAME, response.miller_head_name ?: "")
            putExtra(MillerDashboardActivity.EXTRA_MT_PER_HOUR, response.mt_per_hour ?: 0)
            putExtra(MillerDashboardActivity.EXTRA_LOGIN_MESSAGE, response.message ?: "Login successful.")
        }

        startActivity(dashboardIntent)
        finish()
    }

    private fun parseErrorResponse(response: Response<MillerLoginResponse>): MillerLoginResponse? {
        return try {
            val errorJson = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
            Gson().fromJson(errorJson, MillerLoginResponse::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun setLoginLoading(isLoading: Boolean) {
        isLoginInProgress = isLoading
        editTextUsername.isEnabled = !isLoading
        editTextPassword.isEnabled = !isLoading
        btnLogin.isEnabled = !isLoading
        btnLogin.alpha = if (isLoading) 0.65f else 1.0f
        btnLogin.text = if (isLoading) "Logging in..." else "Login"
    }

    private fun hideKeyboard() {
        currentFocus?.let { focusedView ->
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
            focusedView.clearFocus()
        }
    }

    override fun onDestroy() {
        loginApiCall?.cancel()
        loginApiCall = null
        super.onDestroy()
    }
}