package com.example.farmerapplication.miller

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.miller.BagDetail
import com.example.farmerapplication.models.miller.CommodityTypeItem
import com.example.farmerapplication.models.miller.GunnyBagTypeItem
import com.example.farmerapplication.models.miller.GunnyBagYearItem
import com.example.farmerapplication.models.miller.MillerDistrictItem
import com.example.farmerapplication.models.miller.MillerJsfcGodownItem
import com.example.farmerapplication.models.miller.RiceSubmitRequest
import com.example.farmerapplication.models.miller.RiceSubmitResponse
import com.example.farmerapplication.models.miller.VehicleTypeItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class AdvanceCMRActivity : ComponentActivity() {

    private lateinit var spDistrict: Spinner
    private lateinit var spJSFCGodown: Spinner
    private lateinit var spBankGuaranteeCMR: Spinner
    private lateinit var spCommodityType: Spinner

    private lateinit var imgDistrictArrow: ImageView
    private lateinit var imgJSFCGodownArrow: ImageView
    private lateinit var imgBankGuaranteeCMRArrow: ImageView
    private lateinit var imgCommodityTypeArrow: ImageView

    private lateinit var edtAgency: EditText
    private lateinit var edtCMRSubmitted: EditText
    private lateinit var edtDateOfSubmit: EditText
    private lateinit var edtStencilingCode: EditText

    private lateinit var llDynamicVehicleContainer: LinearLayout
    private lateinit var btnAddVehicle: Button
    private lateinit var btnSave: Button
    private lateinit var progressSave: ProgressBar

    private var vehicleCount = 0
    private var millerId: Int = 0

    // Cached dropdown data from API
    private var districtItems: List<MillerDistrictItem> = emptyList()
    private var godownItems: List<MillerJsfcGodownItem> = emptyList()
    private var commodityItems: List<CommodityTypeItem> = emptyList()
    private var vehicleTypeItems: List<VehicleTypeItem> = emptyList()
    private var gunnyBagYearItems: List<GunnyBagYearItem> = emptyList()
    private var gunnyBagTypeItems: List<GunnyBagTypeItem> = emptyList()

    // Load-completion flags for vehicle-related dropdowns, used to know when
    // it is safe to add the first vehicle entry with populated spinners
    private var vehicleTypeLoaded = false
    private var gunnyBagYearLoaded = false
    private var gunnyBagTypeLoaded = false
    private var firstVehicleAdded = false
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.advance_cmr_layout)

        millerId = intent.getIntExtra(MillerDashboardActivity.EXTRA_MILLER_ID, 0)

        initializeViews()
        setupDistrictSelectionListener()
        setupDatePicker()
        setupButtons()

        fetchDistricts()
        fetchCommodityTypes()
        fetchVehicleTypes()
        fetchGunnyBagYears()
        fetchGunnyBagTypes()
    }

    private fun initializeViews() {
        spDistrict = findViewById(R.id.spDistrict)
        spJSFCGodown = findViewById(R.id.spJSFCGodown)
        spBankGuaranteeCMR = findViewById(R.id.spBankGuaranteeCMR)
        spCommodityType = findViewById(R.id.spCommodityType)

        imgDistrictArrow = findViewById(R.id.imgDistrictArrow)
        imgJSFCGodownArrow = findViewById(R.id.imgJSFCGodownArrow)
        imgBankGuaranteeCMRArrow = findViewById(R.id.imgBankGuaranteeCMRArrow)
        imgCommodityTypeArrow = findViewById(R.id.imgCommodityTypeArrow)

        edtAgency = findViewById(R.id.edtAgency)
        edtCMRSubmitted = findViewById(R.id.edtCMRSubmitted)
        edtDateOfSubmit = findViewById(R.id.edtDateOfSubmit)
        edtStencilingCode = findViewById(R.id.edtStencilingCode)

        llDynamicVehicleContainer = findViewById(R.id.llDynamicVehicleContainer)

        btnAddVehicle = findViewById(R.id.btnAddVehicle)
        btnSave = findViewById(R.id.btnSave)
        progressSave = findViewById(R.id.progressSave)

        // Static dropdown not backed by an API
        setupSpinner(spBankGuaranteeCMR, listOf("Select Bank Guarantee CMR", "Yes", "No"))

        // Only the rightmost arrow icon opens each dropdown
        bindDropdownArrow(spDistrict, imgDistrictArrow)
        bindDropdownArrow(spJSFCGodown, imgJSFCGodownArrow)
        bindDropdownArrow(spBankGuaranteeCMR, imgBankGuaranteeCMRArrow)
        bindDropdownArrow(spCommodityType, imgCommodityTypeArrow)
    }

    // Disables direct-tap-to-open on the spinner, and makes only the
    // arrow icon (rightmost) open the dropdown via performClick().
    private fun bindDropdownArrow(spinner: Spinner, arrow: ImageView) {
        spinner.setOnTouchListener { _, _ -> true } // consume touch, block default open
        spinner.isFocusable = false
        arrow.isClickable = true
        arrow.setOnClickListener { spinner.performClick() }
    }

    private fun setupButtons() {
        btnAddVehicle.setOnClickListener { addVehicle() }
        btnSave.setOnClickListener { saveAdvanceCMR() }
    }

    // ---------------- API: Districts ----------------

    private fun fetchDistricts() {
        RetrofitClient.apiService.getMillerDistricts(millerId)
            .enqueue(object : Callback<List<MillerDistrictItem>> {
                override fun onResponse(
                    call: Call<List<MillerDistrictItem>>,
                    response: Response<List<MillerDistrictItem>>
                ) {
                    if (response.isSuccessful) {
                        districtItems = response.body().orEmpty()
                        val names = mutableListOf("Select District")
                        names.addAll(districtItems.map { it.districtName })
                        setupSpinner(spDistrict, names)
                    } else {
                        showFailureMessage()
                    }
                }

                override fun onFailure(call: Call<List<MillerDistrictItem>>, t: Throwable) {
                    showFailureMessage()
                }
            })
    }

    private fun setupDistrictSelectionListener() {
        spDistrict.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in 1..districtItems.size) {
                    val selectedDistrict = districtItems[position - 1]
                    fetchJsfcGodowns(selectedDistrict.districtCode)
                } else {
                    godownItems = emptyList()
                    setupSpinner(spJSFCGodown, listOf("Select JSFC Godown"))
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ---------------- API: JSFC Godowns ----------------

    private fun fetchJsfcGodowns(districtCode: String) {
        RetrofitClient.apiService.getJsfcGodowns(districtCode)
            .enqueue(object : Callback<List<MillerJsfcGodownItem>> {
                override fun onResponse(
                    call: Call<List<MillerJsfcGodownItem>>,
                    response: Response<List<MillerJsfcGodownItem>>
                ) {
                    if (response.isSuccessful) {
                        godownItems = response.body().orEmpty()
                        val names = mutableListOf("Select JSFC Godown")
                        names.addAll(godownItems.map { it.depoName })
                        setupSpinner(spJSFCGodown, names)
                    } else {
                        showFailureMessage()
                    }
                }

                override fun onFailure(call: Call<List<MillerJsfcGodownItem>>, t: Throwable) {
                    showFailureMessage()
                }
            })
    }

    // ---------------- API: Commodity Types ----------------

    private fun fetchCommodityTypes() {
        RetrofitClient.apiService.getCommodityTypes()
            .enqueue(object : Callback<List<CommodityTypeItem>> {
                override fun onResponse(
                    call: Call<List<CommodityTypeItem>>,
                    response: Response<List<CommodityTypeItem>>
                ) {
                    if (response.isSuccessful) {
                        commodityItems = response.body().orEmpty()
                        val names = mutableListOf("Select Commodity Type")
                        names.addAll(commodityItems.map { it.name })
                        setupSpinner(spCommodityType, names)
                    } else {
                        showFailureMessage()
                    }
                }

                override fun onFailure(call: Call<List<CommodityTypeItem>>, t: Throwable) {
                    showFailureMessage()
                }
            })
    }

    // ---------------- API: Vehicle Types ----------------

    private fun fetchVehicleTypes() {
        RetrofitClient.apiService.getVehicleTypes()
            .enqueue(object : Callback<List<VehicleTypeItem>> {
                override fun onResponse(
                    call: Call<List<VehicleTypeItem>>,
                    response: Response<List<VehicleTypeItem>>
                ) {
                    if (response.isSuccessful) {
                        vehicleTypeItems = response.body().orEmpty()
                    }
                    vehicleTypeLoaded = true
                    maybeAddFirstVehicle()
                }

                override fun onFailure(call: Call<List<VehicleTypeItem>>, t: Throwable) {
                    vehicleTypeLoaded = true
                    maybeAddFirstVehicle()
                    showFailureMessage()
                }
            })
    }

    // ---------------- API: Gunny Bag Years ----------------

    private fun fetchGunnyBagYears() {
        RetrofitClient.apiService.getGunnyBagYears()
            .enqueue(object : Callback<List<GunnyBagYearItem>> {
                override fun onResponse(
                    call: Call<List<GunnyBagYearItem>>,
                    response: Response<List<GunnyBagYearItem>>
                ) {
                    if (response.isSuccessful) {
                        gunnyBagYearItems = response.body().orEmpty()
                    }
                    gunnyBagYearLoaded = true
                    maybeAddFirstVehicle()
                }

                override fun onFailure(call: Call<List<GunnyBagYearItem>>, t: Throwable) {
                    gunnyBagYearLoaded = true
                    maybeAddFirstVehicle()
                    showFailureMessage()
                }
            })
    }

    // ---------------- API: Gunny Bag Types ----------------

    private fun fetchGunnyBagTypes() {
        RetrofitClient.apiService.getGunnyBagTypes()
            .enqueue(object : Callback<List<GunnyBagTypeItem>> {
                override fun onResponse(
                    call: Call<List<GunnyBagTypeItem>>,
                    response: Response<List<GunnyBagTypeItem>>
                ) {
                    if (response.isSuccessful) {
                        gunnyBagTypeItems = response.body().orEmpty()
                    }
                    gunnyBagTypeLoaded = true
                    maybeAddFirstVehicle()
                }

                override fun onFailure(call: Call<List<GunnyBagTypeItem>>, t: Throwable) {
                    gunnyBagTypeLoaded = true
                    maybeAddFirstVehicle()
                    showFailureMessage()
                }
            })
    }

    private fun maybeAddFirstVehicle() {
        if (!firstVehicleAdded && vehicleTypeLoaded && gunnyBagYearLoaded && gunnyBagTypeLoaded) {
            firstVehicleAdded = true
            addVehicle() // Always start with one vehicle
        }
    }

    // ---------------- Vehicle rows ----------------

    private fun addVehicle() {
        vehicleCount++
        val inflater = LayoutInflater.from(this)
        val vehicleView = inflater.inflate(R.layout.advance_cmr_vehicle_detail_item, llDynamicVehicleContainer, false)

        val txtVehicleNumberHeading = vehicleView.findViewById<TextView>(R.id.txtVehicleNumberHeading)
        val btnDeleteVehicle = vehicleView.findViewById<ImageButton>(R.id.btnDeleteVehicle)

        txtVehicleNumberHeading.text = "Vehicle Detail $vehicleCount"

        if (vehicleCount == 1) {
            btnDeleteVehicle.visibility = View.GONE
        } else {
            btnDeleteVehicle.visibility = View.VISIBLE
            btnDeleteVehicle.setOnClickListener {
                llDynamicVehicleContainer.removeView(vehicleView)
                renumberVehicles()
            }
        }

        setupVehicleDropdowns(vehicleView)
        llDynamicVehicleContainer.addView(vehicleView)
    }

    private fun setupVehicleDropdowns(vehicleView: View) {
        val spVehicleType = vehicleView.findViewById<Spinner>(R.id.spVehicleType)
        val spGunnyBagYear = vehicleView.findViewById<Spinner>(R.id.spGunnyBagYear)
        val spGunnyBagType = vehicleView.findViewById<Spinner>(R.id.spGunnyBagType)

        val imgVehicleTypeArrow = vehicleView.findViewById<ImageView>(R.id.imgVehicleTypeArrow)
        val imgGunnyBagYearArrow = vehicleView.findViewById<ImageView>(R.id.imgGunnyBagYearArrow)
        val imgGunnyBagTypeArrow = vehicleView.findViewById<ImageView>(R.id.imgGunnyBagTypeArrow)

        val vehicleTypeNames = mutableListOf("Select Vehicle Type")
        vehicleTypeNames.addAll(vehicleTypeItems.map { it.vehicleType })
        setupSpinner(spVehicleType, vehicleTypeNames)

        val gunnyBagYearNames = mutableListOf("Select Gunny Bag Year")
        gunnyBagYearNames.addAll(gunnyBagYearItems.map { it.gunnyBagYear })
        setupSpinner(spGunnyBagYear, gunnyBagYearNames)

        val gunnyBagTypeNames = mutableListOf("Select Gunny Bag Type")
        gunnyBagTypeNames.addAll(gunnyBagTypeItems.map { it.gunnyBagType })
        setupSpinner(spGunnyBagType, gunnyBagTypeNames)

        // Only the rightmost arrow icon opens each dropdown
        bindDropdownArrow(spVehicleType, imgVehicleTypeArrow)
        bindDropdownArrow(spGunnyBagYear, imgGunnyBagYearArrow)
        bindDropdownArrow(spGunnyBagType, imgGunnyBagTypeArrow)
    }

    private fun renumberVehicles() {
        val childCount = llDynamicVehicleContainer.childCount

        for (i in 0 until childCount) {
            val vehicleView = llDynamicVehicleContainer.getChildAt(i)
            val heading = vehicleView.findViewById<TextView>(R.id.txtVehicleNumberHeading)
            val deleteButton = vehicleView.findViewById<ImageButton>(R.id.btnDeleteVehicle)

            heading.text = "Vehicle Detail ${i + 1}"

            if (i == 0) {
                deleteButton.visibility = View.GONE
                deleteButton.setOnClickListener(null)
            } else {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    llDynamicVehicleContainer.removeView(vehicleView)
                    renumberVehicles()
                }
            }
        }
        vehicleCount = childCount
    }

    private fun setupDatePicker() {
        edtDateOfSubmit.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                edtDateOfSubmit.setText(dateFormat.format(selectedCalendar.time))
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    // ---------------- Save / Submit ----------------
    private fun saveAdvanceCMR() {
        // Hard guard: block re-entry from double taps / rapid clicks
        if (isSubmitting) return

        if (edtDateOfSubmit.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please select Date of Submit.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spDistrict.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select District.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spJSFCGodown.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select JSFC Godown.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spBankGuaranteeCMR.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select Bank Guarantee CMR.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spCommodityType.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select Commodity Type.", Toast.LENGTH_SHORT).show()
            return
        }

        if (llDynamicVehicleContainer.childCount == 0) {
            Toast.makeText(this, "At least one vehicle detail is required.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedDistrict = districtItems.getOrNull(spDistrict.selectedItemPosition - 1)
        val selectedGodown = godownItems.getOrNull(spJSFCGodown.selectedItemPosition - 1)
        // Exactly ONE commodity item, taken from the single dropdown selection —
        // never iterated over the full commodityItems list.
        val selectedCommodity = commodityItems.getOrNull(spCommodityType.selectedItemPosition - 1)

        if (selectedDistrict == null || selectedGodown == null || selectedCommodity == null) {
            Toast.makeText(this, "Please re-select District, Godown and Commodity Type.", Toast.LENGTH_SHORT).show()
            return
        }

        val bagDetails = mutableListOf<BagDetail>()
        for (i in 0 until llDynamicVehicleContainer.childCount) {
            val vehicleView = llDynamicVehicleContainer.getChildAt(i)

            val spVehicleType = vehicleView.findViewById<Spinner>(R.id.spVehicleType)
            val spGunnyBagYear = vehicleView.findViewById<Spinner>(R.id.spGunnyBagYear)
            val spGunnyBagType = vehicleView.findViewById<Spinner>(R.id.spGunnyBagType)
            val edtVehicleNumber = vehicleView.findViewById<EditText>(R.id.edtVehicleNumber)
            val edtDriverName = vehicleView.findViewById<EditText>(R.id.edtDriverName)
            val edtDriverPhone = vehicleView.findViewById<EditText>(R.id.edtDriverPhone)
            val edtNetWeight = vehicleView.findViewById<EditText>(R.id.edtNetWeight)
            val edtTotalBags = vehicleView.findViewById<EditText>(R.id.edtTotalBags)

            val selectedVehicleType = vehicleTypeItems.getOrNull(spVehicleType.selectedItemPosition - 1)
            val selectedGunnyBagYear = gunnyBagYearItems.getOrNull(spGunnyBagYear.selectedItemPosition - 1)
            val selectedGunnyBagType = gunnyBagTypeItems.getOrNull(spGunnyBagType.selectedItemPosition - 1)

            if (selectedVehicleType == null || selectedGunnyBagYear == null || selectedGunnyBagType == null) {
                Toast.makeText(this, "Please complete all fields for Vehicle Detail ${i + 1}.", Toast.LENGTH_SHORT).show()
                return
            }

            bagDetails.add(
                BagDetail(
                    vehicleId = (i + 1).toString(),
                    vehicleType = selectedVehicleType.vehicleType,
                    vehicleNumber = edtVehicleNumber.text.toString().trim(),
                    driverName = edtDriverName.text.toString().trim(),
                    gunnyBagId = selectedGunnyBagYear.id,
                    gunnyBagYear = selectedGunnyBagYear.gunnyBagYear,
                    gunnyBagType = selectedGunnyBagType.gunnyBagType,
                    netWeight = edtNetWeight.text.toString().trim(),
                    totalBags = edtTotalBags.text.toString().trim(),
                    mobileNo = edtDriverPhone.text.toString().trim()
                )
            )
        }

        val riceSubmitted = extractNumber(edtCMRSubmitted.text.toString())
        val formattedDate = convertDateForRequest(edtDateOfSubmit.text.toString().trim())
        val bankGuaranteeValue = mapBankGuaranteeToInt(spBankGuaranteeCMR.selectedItemPosition)

        val request = RiceSubmitRequest(
            millerId = millerId.toString(),
            riceSubmitted = riceSubmitted,
            dateToSubmitted = formattedDate,
            transDistrictCode = selectedDistrict.districtCode,
            jsfcGodownId = selectedGodown.jsfcId,
            stancilingCode = edtStencilingCode.text.toString().trim(),
            commodityTypeId = selectedCommodity.id,
            bankGuaranteeCMR = bankGuaranteeValue,
            bagDetails = bagDetails
        )

        submitAdvanceCMR(request)
    }

    // spBankGuaranteeCMR options: 0 = "Select...", 1 = "Yes", 2 = "No"
    // API expects 1 for Yes, 0 for No.
    private fun mapBankGuaranteeToInt(selectedPosition: Int): Int {
        return if (selectedPosition == 1) 1 else 0
    }

    private fun submitAdvanceCMR(request: RiceSubmitRequest) {
        isSubmitting = true
        setSaving(true)
        RetrofitClient.apiService.submitRiceAdvanceCMR(request)
            .enqueue(object : Callback<RiceSubmitResponse> {
                override fun onResponse(call: Call<RiceSubmitResponse>, response: Response<RiceSubmitResponse>) {
                    isSubmitting = false
                    setSaving(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AdvanceCMRActivity, "Success", Toast.LENGTH_SHORT).show()
                        finish() // Go back to previous page
                    } else {
                        // Stay on this page; all entered details remain untouched
                        val errorMsg = response.body()?.message?.takeIf { it.isNotBlank() }
                            ?: "Submission failed. Please try again."
                        Toast.makeText(this@AdvanceCMRActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<RiceSubmitResponse>, t: Throwable) {
                    isSubmitting = false
                    setSaving(false)
                    // Stay on this page; all entered details remain untouched
                    Toast.makeText(
                        this@AdvanceCMRActivity,
                        "Submission failed. Please check your connection and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun setSaving(saving: Boolean) {
        progressSave.visibility = if (saving) View.VISIBLE else View.GONE
        btnSave.isEnabled = !saving
        btnAddVehicle.isEnabled = !saving
    }

    private fun showFailureMessage() {
        Toast.makeText(this, "Failure", Toast.LENGTH_SHORT).show()
    }

    private fun extractNumber(text: String): Int {
        val matcher = Pattern.compile("\\d+").matcher(text)
        return if (matcher.find()) matcher.group().toIntOrNull() ?: 0 else 0
    }

    // Converts UI date "dd/MM/yyyy" to API-required "dd-MM-yyyy"
    private fun convertDateForRequest(uiDate: String): String {
        return uiDate.replace("/", "-")
    }
}