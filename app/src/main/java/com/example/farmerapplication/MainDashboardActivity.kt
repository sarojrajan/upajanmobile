package com.example.farmerapplication

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.farmerapplication.farmer.FarmerLoginActivity

class MainDashboardActivity : ComponentActivity() {

    private lateinit var btnFarmerLogin: LinearLayout
    private lateinit var btnOfficialLogin: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_dashboard)

        // Initialize Views
        btnFarmerLogin = findViewById(R.id.btnFarmerLogin)
        btnOfficialLogin = findViewById(R.id.btnOfficialLogin)

        // Farmer Login Click
        btnFarmerLogin.setOnClickListener { startActivity(Intent(this, FarmerLoginActivity::class.java)) }

        // Official Login Click
        btnOfficialLogin.setOnClickListener { startActivity(Intent(this, OfficialLoginActivity::class.java)) }
    }
}