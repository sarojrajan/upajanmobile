package com.example.farmerapplication.dmsfc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.DSOLoginRequest
import com.example.farmerapplication.models.DSOLoginResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.text.InputType
import android.widget.ImageButton

class DMSFCLoginActivity : ComponentActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnTogglePassword: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dmsfc_login_dashboard)

        etUsername = findViewById(R.id.editTextUsername)
        etPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)

        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        btnLogin.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val loginId = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (loginId.isEmpty()) {
            etUsername.error = "Enter Email/Mobile no."
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Enter Password"
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Please wait..."

        // usertype_id is always 6 for DMSFC
        val request = DSOLoginRequest(login_id = loginId, password = password, usertype_id = 6)

        RetrofitClient.apiService.dsoLogin(request).enqueue(object : Callback<DSOLoginResponse> {

            override fun onResponse(call: Call<DSOLoginResponse>, response: Response<DSOLoginResponse>) {
                resetButton()

                // Success body, or error body (e.g. 401) parsed into the same model
                val body = if (response.isSuccessful) {
                    response.body()
                } else {
                    try {
                        Gson().fromJson(response.errorBody()?.string(), DSOLoginResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (response.isSuccessful && body?.statusCode == 200 && !body.token.isNullOrEmpty()) {
                    saveSession(body)
                    Toast.makeText(this@DMSFCLoginActivity, body.message ?: "Login successful.", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@DMSFCLoginActivity, DMSFCDashboardActivity::class.java).apply {
                        putExtra("officer_name", body.officer_name)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val msg = body?.message ?: "Login failed. Please try again."
                    Toast.makeText(this@DMSFCLoginActivity, msg, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<DSOLoginResponse>, t: Throwable) {
                resetButton()
                Toast.makeText(
                    this@DMSFCLoginActivity,
                    "Network error: ${t.localizedMessage ?: "Please check your connection"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun saveSession(r: DSOLoginResponse) {
        getSharedPreferences("dmsfc_prefs", MODE_PRIVATE).edit()
            .putString("token", r.token)
            .putInt("user_id", r.user_id ?: 0)
            .putInt("usertype_id", r.usertype_id ?: 6)
            .putString("mobile_no", r.mobile_no)
            .putString("email_id", r.email_id)
            .putString("officer_name", r.officer_name)
            .apply()
    }

    private fun resetButton() {
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
    }

    private fun togglePasswordVisibility() {
        val cursorPosition = etPassword.selectionStart

        if (etPassword.inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            btnTogglePassword.contentDescription = "Hide password"
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            btnTogglePassword.setImageResource(R.drawable.ic_eye)
            btnTogglePassword.contentDescription = "Show password"
        }

        etPassword.setSelection(cursorPosition)
    }
}