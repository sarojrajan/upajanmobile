package com.example.farmerapplication

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.farmerapplication.miller.MillerLoginActivity
import com.example.farmerapplication.msp.MSPLoginActivity

class OfficialLoginActivity : ComponentActivity() {

    private lateinit var btnMSPLogin: LinearLayout
    private lateinit var btnMillerLogin: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.official_login_dashboard)

        // Initialize Views
        btnMSPLogin = findViewById(R.id.btnMSPLogin)
        btnMillerLogin = findViewById(R.id.btnMillerLogin)


        // MSP Login Click
        btnMSPLogin.setOnClickListener {
            val intent = Intent(this, MSPLoginActivity::class.java)
            startActivity(intent)
        }

        // Miller Login Click
        btnMillerLogin.setOnClickListener {
            val intent = Intent(this, MillerLoginActivity::class.java)
            startActivity(intent)
        }
    }
}