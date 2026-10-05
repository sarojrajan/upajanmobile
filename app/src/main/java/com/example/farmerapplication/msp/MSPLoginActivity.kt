package com.example.farmerapplication.msp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.MSPLoginRequest
import com.example.farmerapplication.models.MSPLoginResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MSPLoginActivity : ComponentActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnLogin: Button

    private var loginApiCall: Call<MSPLoginResponse>? = null
    private var isLoginInProgress = false

    companion object {
        private const val TAG_LOGIN_REQUEST = "MSP_LOGIN_REQUEST"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.msp_login_dashboard)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupClickListeners() {
        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }

        btnLogin.setOnClickListener {
            hideKeyboard()
            if (validateLoginFields()) {
                performMSPLogin()
            }
        }

        editTextPassword.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            if (validateLoginFields()) {
                performMSPLogin()
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
            editTextUsername.error = "Please enter email or mobile number."
            editTextUsername.requestFocus()
            return false
        }

        if (password.isBlank()) {
            editTextPassword.error = "Please enter password."
            editTextPassword.requestFocus()
            return false
        }

        return true
    }

    private fun togglePasswordVisibility() {
        val cursorPosition = editTextPassword.selectionStart

        if (editTextPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            btnTogglePassword.contentDescription = "Hide password"
        } else {
            editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            btnTogglePassword.setImageResource(R.drawable.ic_eye)
            btnTogglePassword.contentDescription = "Show password"
        }
        editTextPassword.setSelection(cursorPosition)
    }

    private fun performMSPLogin() {
        if (isLoginInProgress) return

        val emailOrMobile = editTextUsername.text.toString().trim()
        val password = editTextPassword.text.toString()

        val request = MSPLoginRequest(
            emailId = emailOrMobile,
            password = password
        )

        setLoginLoading(true)

        loginApiCall = RetrofitClient.apiService.loginMSP(request)
        loginApiCall?.enqueue(object : Callback<MSPLoginResponse> {
            override fun onResponse(call: Call<MSPLoginResponse>, response: Response<MSPLoginResponse>) {
                if (isFinishing || isDestroyed) return
                setLoginLoading(false)

                val responseBody = response.body()

                if (response.isSuccessful && responseBody?.statusCode == 200) {
                    handleSuccessfulLogin(responseBody)
                } else {
                    val errorResponse = parseErrorResponse(response)
                    val message = errorResponse?.message?.takeIf { it.isNotBlank() }
                        ?: responseBody?.message?.takeIf { it.isNotBlank() }
                        ?: when (response.code()) {
                            400 -> "Invalid login request."
                            401 -> "Invalid email/mobile or password."
                            403 -> "You are not authorized to log in."
                            404 -> "MSP login service was not found."
                            408 -> "The login request timed out."
                            429 -> "Too many login attempts. Please try again later."
                            in 500..599 -> "Server error occurred. Please try again."
                            else -> "Unable to complete MSP login."
                        }

                    Toast.makeText(this@MSPLoginActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<MSPLoginResponse>, throwable: Throwable) {
                if (isFinishing || isDestroyed || call.isCanceled) return
                setLoginLoading(false)

                val message = when (throwable) {
                    is IOException -> "Unable to connect to the server. Check your internet connection and API tunnel."
                    else -> throwable.localizedMessage ?: "Something went wrong during MSP login."
                }

                Toast.makeText(this@MSPLoginActivity, message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun handleSuccessfulLogin(response: MSPLoginResponse) {
        if (response.mspCentreId == null) {
            Toast.makeText(this, "MSP login response is incomplete.", Toast.LENGTH_LONG).show()
            return
        }

        val responseJson = Gson().toJson(response)
        val dashboardIntent = Intent(this, MSPDashboardActivity::class.java).apply {
            putExtra(MSPDashboardActivity.EXTRA_MSP_RESPONSE, responseJson)
        }

        startActivity(dashboardIntent)
        finish()
    }

    private fun parseErrorResponse(response: Response<MSPLoginResponse>): MSPLoginResponse? {
        return try {
            val errorJson = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: return null
            Gson().fromJson(errorJson, MSPLoginResponse::class.java)
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