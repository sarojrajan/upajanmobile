package com.example.farmerapplication.msp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.msp.CommodityReceivedItem
import com.example.farmerapplication.models.msp.CommodityReceivedResponse
import com.example.farmerapplication.utils.ReportExportUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FarmersPaddyGivenReportActivity : ComponentActivity() {

    private lateinit var tableContainer: LinearLayout
    private lateinit var txtEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnExportExcel: ImageView
    private lateinit var btnExportPdf: ImageView

    private var mspCentreId: String = ""
    private var recordsList: List<CommodityReceivedItem> = emptyList()

    private val headers = listOf("Farmer ID", "Farmer Name", "Qty Received (Qntls)", "Date of Receipt")
    private val weights = floatArrayOf(1.3f, 1.6f, 1.2f, 1.3f)

    companion object {
        const val EXTRA_MSP_CENTRE_ID = "extra_msp_centre_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_paddy_given_to_msp_report)
        initializeViews()
        receiveMSPCentreId()
        setupClickListeners()
        fetchCommodityReceived()
    }

    private fun initializeViews() {
        tableContainer = findViewById(R.id.tableContainer)
        txtEmptyState = findViewById(R.id.txtEmptyState)
        progressBar = findViewById(R.id.progressBar)
        btnExportExcel = findViewById(R.id.btnExportExcel)
        btnExportPdf = findViewById(R.id.btnExportPdf)
    }

    private fun receiveMSPCentreId() {
        mspCentreId = intent.getStringExtra(EXTRA_MSP_CENTRE_ID)?.trim().orEmpty()
        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        btnExportExcel.setOnClickListener { exportToExcel() }
        btnExportPdf.setOnClickListener { exportToPdf() }
    }

    private fun fetchCommodityReceived() {
        if (mspCentreId.isBlank()) {
            showEmptyState("MSP Centre ID is unavailable.")
            return
        }
        showLoading(true)
        RetrofitClient.apiService.getCommodityReceived(mspCentreId).enqueue(object : Callback<CommodityReceivedResponse> {
            override fun onResponse(call: Call<CommodityReceivedResponse>, response: Response<CommodityReceivedResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val body = response.body()
                    val list = body?.data.orEmpty()
                    if (list.isEmpty()) {
                        showEmptyState(body?.message ?: "No records found.")
                    } else {
                        recordsList = list
                        renderTable(list)
                    }
                } else {
                    showEmptyState("Failed to fetch report. Please try again.")
                }
            }
            override fun onFailure(call: Call<CommodityReceivedResponse>, t: Throwable) {
                showLoading(false)
                showEmptyState("Network error: ${t.localizedMessage ?: "Unable to connect."}")
            }
        })
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            txtEmptyState.visibility = View.GONE
            tableContainer.visibility = View.GONE
        }
    }

    private fun showEmptyState(message: String) {
        tableContainer.removeAllViews()
        tableContainer.visibility = View.GONE
        txtEmptyState.text = message
        txtEmptyState.visibility = View.VISIBLE
    }

    private fun renderTable(list: List<CommodityReceivedItem>) {
        tableContainer.removeAllViews()
        tableContainer.visibility = View.VISIBLE
        txtEmptyState.visibility = View.GONE
        // Header
        tableContainer.addView(buildRow(values = headers, isHeader = true))
        // Data rows
        list.forEachIndexed { index, item ->
            val rowValues = listOf(
                item.farmerId,
                item.farmerName,
                String.format(Locale.getDefault(), "%.2f", item.qtyReceived),
                formatDate(item.dateOfReceipt)
            )
            tableContainer.addView(buildRow(values = rowValues, isHeader = false, isAlt = index % 2 == 1))
        }
    }

    private fun buildRow(values: List<String>, isHeader: Boolean, isAlt: Boolean = false): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 1 }
        }
        values.forEachIndexed { index, text ->
            val cell = TextView(this).apply {
                this.text = text
                setSingleLine(false)             // Allow text to wrap into multiple lines.
                maxLines = 5                     // Allow text to wrap into multiple lines.
                includeFontPadding = true        // Break long content naturally.
                setPadding(10, 16, 10, 16)
                textSize = if (isHeader) 13f else 12.5f
                setTypeface(null, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (isHeader) Color.WHITE else Color.parseColor("#212121"))
                gravity = Gravity.CENTER
                setBackgroundColor(
                    when {
                        isHeader -> Color.parseColor("#6D4C41")
                        isAlt -> Color.parseColor("#F5F0EA")
                        else -> Color.WHITE
                    }
                )
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weights[index]).apply { marginEnd = 1 }
            }
            row.addView(cell)
        }
        return row
    }

    private fun formatDate(rawDate: String): String {
        val possibleFormats = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd", "dd-MM-yyyy")
        for (format in possibleFormats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                inputFormat.isLenient = false
                val parsedDate = inputFormat.parse(rawDate)
                if (parsedDate != null) {
                    val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    return outputFormat.format(parsedDate)
                }
            } catch (_: Exception) { /* Try next format. */ }
        }
        return rawDate
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    private fun getExportRows(): List<List<String>> {
        return recordsList.map {
            listOf(
                it.farmerId,
                it.farmerName,
                String.format(Locale.getDefault(), "%.2f", it.qtyReceived),
                formatDate(it.dateOfReceipt)
            )
        }
    }

    private fun exportToExcel() {
        if (recordsList.isEmpty()) {
            Toast.makeText(this, "No data available to export.", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = getExportRows()
        val currentDate = getCurrentDate()
        val fileName = "Farmers_Paddy_Given_Report_$currentDate.csv"
        val success = ReportExportUtils.exportCsv(
            context = this,
            fileName = fileName,
            title = "List of Farmers Paddy Given",
            headers = headers,
            rows = rows
        )
        Toast.makeText(
            this,
            if (success) "Saved to Downloads: $fileName" else "Failed to export report.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun exportToPdf() {
        if (recordsList.isEmpty()) {
            Toast.makeText(this, "No data available to export.", Toast.LENGTH_SHORT).show()
            return
        }
        val rows = getExportRows()
        val currentDate = getCurrentDate()
        val fileName = "Farmers_Paddy_Given_Report_$currentDate.pdf"
        val success = ReportExportUtils.exportPdf(
            context = this,
            fileName = fileName,
            title = "List of Farmers Paddy Given",
            headers = headers,
            rows = rows,
            columnWidths = listOf(135, 250, 165, 170),
            logoResId = R.drawable.mpgov_logo
        )
        Toast.makeText(
            this,
            if (success) "Saved to Downloads: $fileName" else "Failed to export report.",
            Toast.LENGTH_LONG
        ).show()
    }
}