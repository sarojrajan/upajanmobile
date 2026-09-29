package com.example.farmerapplication.farmer

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
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
import androidx.activity.result.contract.ActivityResultContracts
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.BlockItem
import com.example.farmerapplication.models.BlockResponse
import com.example.farmerapplication.models.CircleItem
import com.example.farmerapplication.models.CircleResponse
import com.example.farmerapplication.models.HalkaItem
import com.example.farmerapplication.models.HalkaResponse
import com.example.farmerapplication.models.LandRecordEntry
import com.example.farmerapplication.models.MaujaItem
import com.example.farmerapplication.models.MaujaResponse
import com.example.farmerapplication.models.SaveLandRecordsRequest
import com.example.farmerapplication.models.SaveLandRecordsResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode

class LandDetailsEntryActivity : ComponentActivity() {

    companion object {
        private const val MAXIMUM_PDF_SIZE_BYTES = 512L * 1024L
        private const val QUINTALS_PER_ACRE = 16.0
    }

    private lateinit var spLandType: Spinner
    private lateinit var spSubdistrict: Spinner
    private lateinit var imgLandTypeDropdown: ImageView
    private lateinit var imgSubdistrictDropdown: ImageView
    private lateinit var llDynamicLandContainer: LinearLayout
    private lateinit var btnAddLandEntry: Button
    private lateinit var btnUploadLandDocument: Button
    private lateinit var txtSelectedLandDocument: TextView
    private lateinit var progressSubmit: ProgressBar
    private lateinit var btnUpdate: Button

    private var farmerId: String = ""
    private var districtId: String = ""

    private var basicDetail: Int = 0
    private var landDetail: Int = 0

    private val landTypeDisplayList = listOf("Select Land Type", "Own", "Lease")
    private var subdistrictItems: List<BlockItem> = emptyList()
    private var circleItems: List<CircleItem> = emptyList()
    private val landEntryHolders = mutableListOf<LandEntryViewHolder>()

    private var selectedDocumentUri: Uri? = null
    private var selectedDocumentBase64: String? = null
    private var selectedDocumentName: String? = null

    private var blocksCall: Call<BlockResponse>? = null
    private var circlesCall: Call<CircleResponse>? = null
    private var saveLandCall: Call<SaveLandRecordsResponse>? = null

    /*
     * Opens PDF documents through Android's document picker.
     */
    private val documentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        validateAndReadPdf(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.land_entry_layout)

        farmerId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_ID).orEmpty().trim()
        districtId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_ID).orEmpty().trim()
        basicDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_BASIC_DETAIL, 0)
        landDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_LAND_DETAIL, 0)

        initializeViews()
        setupLandTypeDropdown()
        setupButtons()
        setupMainDropdownArrows()

        /*
         * Add the first land card automatically.
         */
        addNewLandEntry()

        if (districtId.isBlank()) {
            showToast("District ID is missing. Please log in again.")
            disableForm()
            return
        }

        if (farmerId.isBlank()) {
            showToast("Farmer ID is missing. Please log in again.")
            disableForm()
            return
        }

        fetchSubdistricts()
        fetchCircles()
    }

    private fun initializeViews() {
        spLandType = findViewById(R.id.spLandType)
        spSubdistrict = findViewById(R.id.spSubdistrict)
        imgLandTypeDropdown = findViewById(R.id.imgLandTypeDropdown)
        imgSubdistrictDropdown = findViewById(R.id.imgSubdistrictDropdown)
        llDynamicLandContainer = findViewById(R.id.llDynamicLandContainer)
        btnAddLandEntry = findViewById(R.id.btnAddLandEntry)
        btnUploadLandDocument = findViewById(R.id.btnUploadLandDocument)
        txtSelectedLandDocument = findViewById(R.id.txtSelectedLandDocument)
        progressSubmit = findViewById(R.id.progressSubmit)
        btnUpdate = findViewById(R.id.btnUpdate)
    }

    private fun setupButtons() {
        btnAddLandEntry.setOnClickListener { addNewLandEntry() }
        btnUploadLandDocument.setOnClickListener { documentPicker.launch(arrayOf("application/pdf")) }
        btnUpdate.setOnClickListener { validateAndSubmitLandRecords() }
    }

    private fun setupLandTypeDropdown() { setSpinnerTextItems(spinner = spLandType, items = landTypeDisplayList) }

    private fun setupMainDropdownArrows() {
        setupArrowOnlyDropdown(spinner = spLandType, arrowImage = imgLandTypeDropdown)
        setupArrowOnlyDropdown(spinner = spSubdistrict, arrowImage = imgSubdistrictDropdown)
    }

    // ---------------------------------------------------------
    // DYNAMIC LAND CARD
    // ---------------------------------------------------------

    private fun addNewLandEntry() {
        val itemView = LayoutInflater.from(this).inflate(R.layout.land_entry_item, llDynamicLandContainer, false)

        val holder = LandEntryViewHolder(
            rootView = itemView,
            title = itemView.findViewById(R.id.tvLandDetailNumber),
            removeButton = itemView.findViewById(R.id.btnRemoveLandEntry),
            circleSpinner = itemView.findViewById(R.id.spCircle),
            circleDropdownArrow = itemView.findViewById(R.id.imgCircleDropdown),
            halkaSpinner = itemView.findViewById(R.id.spHalka),
            halkaDropdownArrow = itemView.findViewById(R.id.imgHalkaDropdown),
            maujaSpinner = itemView.findViewById(R.id.spMauja),
            maujaDropdownArrow = itemView.findViewById(R.id.imgMaujaDropdown),
            cropSpinner = itemView.findViewById(R.id.spCrop),
            cropDropdownArrow = itemView.findViewById(R.id.imgCropDropdown),
            volumeNoEditText = itemView.findViewById(R.id.edtVolumeNo),
            pageNoEditText = itemView.findViewById(R.id.edtPageNo),
            plotNoEditText = itemView.findViewById(R.id.edtPlotNo),
            khataNoEditText = itemView.findViewById(R.id.edtKhataNo),
            rakbaEditText = itemView.findViewById(R.id.edtRakba),
            irrigatedEditText = itemView.findViewById(R.id.edtIrrigated),
            unirrigatedEditText = itemView.findViewById(R.id.edtUnirrigated),
            expectedIrrigatedEditText = itemView.findViewById(R.id.edtExpectedIrrigated),
            expectedUnirrigatedEditText = itemView.findViewById(R.id.edtExpectedUnirrigated)
        )

        landEntryHolders.add(holder)
        llDynamicLandContainer.addView(itemView)

        /*
         * Configure each dropdown so only its arrow opens it.
         */
        setupArrowOnlyDropdown(spinner = holder.circleSpinner, arrowImage = holder.circleDropdownArrow)
        setupArrowOnlyDropdown(spinner = holder.halkaSpinner, arrowImage = holder.halkaDropdownArrow)
        setupArrowOnlyDropdown(spinner = holder.maujaSpinner, arrowImage = holder.maujaDropdownArrow)
        setupArrowOnlyDropdown(spinner = holder.cropSpinner, arrowImage = holder.cropDropdownArrow)

        setupCropDropdown(holder)
        setupDependentDropdowns(holder)
        setupLandCalculation(holder)

        if (circleItems.isNotEmpty()) {
            populateCircleDropdown(holder)
        } else {
            setSpinnerTextItems(holder.circleSpinner, listOf("Loading circles..."))
        }

        setSpinnerTextItems(holder.halkaSpinner, listOf("Select Circle First"))
        setSpinnerTextItems(holder.maujaSpinner, listOf("Select Halka First"))

        holder.removeButton.setOnClickListener { removeLandEntry(holder) }

        updateHolderArrowStates(holder)
        renumberLandEntries()
    }

    private fun removeLandEntry(holder: LandEntryViewHolder) {
        if (landEntryHolders.size <= 1) {
            showToast("At least one land detail is required.")
            return
        }
        holder.halkaApiCall?.cancel()
        holder.maujaApiCall?.cancel()
        landEntryHolders.remove(holder)
        llDynamicLandContainer.removeView(holder.rootView)
        renumberLandEntries()
    }

    private fun renumberLandEntries() {
        landEntryHolders.forEachIndexed { index, holder -> holder.title.text = "Land Detail ${index + 1}" }
    }

    private fun setupCropDropdown(holder: LandEntryViewHolder) {
        setSpinnerTextItems(holder.cropSpinner, listOf("Select Crop", "धान सादा"))
        holder.cropSpinner.isEnabled = true
        holder.cropDropdownArrow.isEnabled = true
    }

    // ---------------------------------------------------------
    // SUBDISTRICT
    // ---------------------------------------------------------

    private fun fetchSubdistricts() {
        setSpinnerTextItems(spSubdistrict, listOf("Loading subdistricts..."))
        spSubdistrict.isEnabled = false
        imgSubdistrictDropdown.isEnabled = false

        blocksCall?.cancel()
        blocksCall = RetrofitClient.apiService.getBlocks(districtId)

        blocksCall?.enqueue(object : Callback<BlockResponse> {
            override fun onResponse(call: Call<BlockResponse>, response: Response<BlockResponse>) {
                if (isFinishing || isDestroyed) return

                val responseBody = response.body()
                if (response.isSuccessful && responseBody?.statusCode == 200) {
                    subdistrictItems = responseBody.data.orEmpty().filter {
                        !it.subdistrictCode.isNullOrBlank() && !it.subdistrictName.isNullOrBlank()
                    }

                    val names = mutableListOf("Select Subdistrict")
                    names.addAll(subdistrictItems.map { it.subdistrictName.orEmpty() })

                    setSpinnerTextItems(spSubdistrict, names)
                    spSubdistrict.isEnabled = subdistrictItems.isNotEmpty()
                    imgSubdistrictDropdown.isEnabled = subdistrictItems.isNotEmpty()
                } else {
                    subdistrictItems = emptyList()
                    setSpinnerTextItems(spSubdistrict, listOf("No subdistrict available"))
                    spSubdistrict.isEnabled = false
                    imgSubdistrictDropdown.isEnabled = false
                    showToast(getApiErrorMessage(response = response, defaultMessage = "Unable to load subdistricts."))
                }
            }

            override fun onFailure(call: Call<BlockResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return

                subdistrictItems = emptyList()
                setSpinnerTextItems(spSubdistrict, listOf("Unable to load subdistricts"))
                spSubdistrict.isEnabled = false
                imgSubdistrictDropdown.isEnabled = false
                showToast(throwable.message ?: "Unable to load subdistricts.")
            }
        })
    }

    // ---------------------------------------------------------
    // CIRCLE
    // ---------------------------------------------------------

    private fun fetchCircles() {
        circlesCall?.cancel()
        circlesCall = RetrofitClient.apiService.getCircles(districtId)

        circlesCall?.enqueue(object : Callback<CircleResponse> {
            override fun onResponse(call: Call<CircleResponse>, response: Response<CircleResponse>) {
                if (isFinishing || isDestroyed) return

                val responseBody = response.body()
                if (response.isSuccessful && responseBody?.statusCode == 200) {
                    circleItems = responseBody.data.orEmpty().filter {
                        !it.circleCode.isNullOrBlank() && !it.circleName.isNullOrBlank()
                    }
                    landEntryHolders.forEach { holder -> populateCircleDropdown(holder) }
                } else {
                    circleItems = emptyList()
                    landEntryHolders.forEach { holder ->
                        setSpinnerTextItems(holder.circleSpinner, listOf("No circle available"))
                        holder.circleSpinner.isEnabled = false
                        holder.circleDropdownArrow.isEnabled = false
                    }
                    showToast(getApiErrorMessage(response = response, defaultMessage = "Unable to load circles."))
                }
            }

            override fun onFailure(call: Call<CircleResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return

                circleItems = emptyList()
                landEntryHolders.forEach { holder ->
                    setSpinnerTextItems(holder.circleSpinner, listOf("Unable to load circles"))
                    holder.circleSpinner.isEnabled = false
                    holder.circleDropdownArrow.isEnabled = false
                }
                showToast(throwable.message ?: "Unable to load circles.")
            }
        })
    }

    private fun populateCircleDropdown(holder: LandEntryViewHolder) {
        val circleNames = mutableListOf("Select Circle")
        circleNames.addAll(circleItems.map { it.circleName.orEmpty() })

        /*
         * Remove old listener while resetting adapter.
         */
        holder.circleSpinner.onItemSelectedListener = null
        setSpinnerTextItems(holder.circleSpinner, circleNames)

        holder.selectedCircle = null
        holder.selectedHalka = null
        holder.selectedMauja = null
        holder.halkaItems = emptyList()
        holder.maujaItems = emptyList()

        setSpinnerTextItems(holder.halkaSpinner, listOf("Select Circle First"))
        setSpinnerTextItems(holder.maujaSpinner, listOf("Select Halka First"))

        holder.circleSpinner.isEnabled = circleItems.isNotEmpty()
        holder.circleDropdownArrow.isEnabled = circleItems.isNotEmpty()

        holder.halkaSpinner.isEnabled = false
        holder.halkaDropdownArrow.isEnabled = false
        holder.maujaSpinner.isEnabled = false
        holder.maujaDropdownArrow.isEnabled = false

        setupDependentDropdowns(holder)
    }

    // ---------------------------------------------------------
    // CIRCLE -> HALKA -> MAUJA
    // ---------------------------------------------------------

    private fun setupDependentDropdowns(holder: LandEntryViewHolder) {
        holder.circleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0) {
                    holder.selectedCircle = null
                    holder.selectedHalka = null
                    holder.selectedMauja = null
                    holder.halkaItems = emptyList()
                    holder.maujaItems = emptyList()

                    setSpinnerTextItems(holder.halkaSpinner, listOf("Select Circle First"))
                    setSpinnerTextItems(holder.maujaSpinner, listOf("Select Halka First"))

                    holder.halkaSpinner.isEnabled = false
                    holder.halkaDropdownArrow.isEnabled = false
                    holder.maujaSpinner.isEnabled = false
                    holder.maujaDropdownArrow.isEnabled = false
                    return
                }

                val selectedIndex = position - 1
                if (selectedIndex !in circleItems.indices) return

                holder.selectedCircle = circleItems[selectedIndex]
                holder.selectedHalka = null
                holder.selectedMauja = null

                val circleCode = holder.selectedCircle?.circleCode.orEmpty()
                if (circleCode.isNotBlank()) fetchHalkas(holder = holder, circleCode = circleCode)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { holder.selectedCircle = null }
        }

        holder.halkaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position <= 0) {
                    holder.selectedHalka = null
                    holder.selectedMauja = null
                    holder.maujaItems = emptyList()

                    setSpinnerTextItems(holder.maujaSpinner, listOf("Select Halka First"))
                    holder.maujaSpinner.isEnabled = false
                    holder.maujaDropdownArrow.isEnabled = false
                    return
                }

                val selectedIndex = position - 1
                if (selectedIndex !in holder.halkaItems.indices) return

                holder.selectedHalka = holder.halkaItems[selectedIndex]
                holder.selectedMauja = null

                val circleCode = holder.selectedCircle?.circleCode.orEmpty()
                val halkaCode = holder.selectedHalka?.halkaCode.orEmpty()

                if (circleCode.isNotBlank() && halkaCode.isNotBlank()) {
                    fetchMaujas(holder = holder, circleCode = circleCode, halkaCode = halkaCode)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { holder.selectedHalka = null }
        }

        holder.maujaSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                holder.selectedMauja = if (position > 0 && position - 1 in holder.maujaItems.indices) {
                    holder.maujaItems[position - 1]
                } else null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { holder.selectedMauja = null }
        }
    }

    private fun fetchHalkas(holder: LandEntryViewHolder, circleCode: String) {
        holder.halkaApiCall?.cancel()
        holder.maujaApiCall?.cancel()

        holder.halkaItems = emptyList()
        holder.maujaItems = emptyList()
        holder.selectedHalka = null
        holder.selectedMauja = null

        holder.halkaSpinner.isEnabled = false
        holder.halkaDropdownArrow.isEnabled = false
        holder.maujaSpinner.isEnabled = false
        holder.maujaDropdownArrow.isEnabled = false

        setSpinnerTextItems(holder.halkaSpinner, listOf("Loading Halkas..."))
        setSpinnerTextItems(holder.maujaSpinner, listOf("Select Halka First"))

        holder.halkaApiCall = RetrofitClient.apiService.getHalkas(districtCode = districtId, circleCode = circleCode)
        holder.halkaApiCall?.enqueue(object : Callback<HalkaResponse> {
            override fun onResponse(call: Call<HalkaResponse>, response: Response<HalkaResponse>) {
                if (call.isCanceled || isFinishing || isDestroyed || !landEntryHolders.contains(holder)) return

                val responseBody = response.body()
                if (response.isSuccessful && responseBody?.statusCode == 200) {
                    holder.halkaItems = responseBody.data.orEmpty().filter {
                        !it.halkaCode.isNullOrBlank() && !it.halkaName.isNullOrBlank()
                    }

                    val names = mutableListOf("Select Halka")
                    names.addAll(holder.halkaItems.map { it.halkaName.orEmpty() })

                    setSpinnerTextItems(holder.halkaSpinner, names)

                    val isAvailable = holder.halkaItems.isNotEmpty()
                    holder.halkaSpinner.isEnabled = isAvailable
                    holder.halkaDropdownArrow.isEnabled = isAvailable
                } else {
                    setSpinnerTextItems(holder.halkaSpinner, listOf("No Halka available"))
                    holder.halkaSpinner.isEnabled = false
                    holder.halkaDropdownArrow.isEnabled = false
                    showToast(getApiErrorMessage(response = response, defaultMessage = "Unable to load Halkas."))
                }
            }

            override fun onFailure(call: Call<HalkaResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed || !landEntryHolders.contains(holder)) return
                setSpinnerTextItems(holder.halkaSpinner, listOf("Unable to load Halkas"))
                holder.halkaSpinner.isEnabled = false
                holder.halkaDropdownArrow.isEnabled = false
                showToast(throwable.message ?: "Unable to load Halkas.")
            }
        })
    }

    private fun fetchMaujas(holder: LandEntryViewHolder, circleCode: String, halkaCode: String) {
        holder.maujaApiCall?.cancel()
        holder.maujaItems = emptyList()
        holder.selectedMauja = null

        holder.maujaSpinner.isEnabled = false
        holder.maujaDropdownArrow.isEnabled = false
        setSpinnerTextItems(holder.maujaSpinner, listOf("Loading Maujas..."))

        holder.maujaApiCall = RetrofitClient.apiService.getMaujas(districtCode = districtId, circleCode = circleCode, halkaCode = halkaCode)
        holder.maujaApiCall?.enqueue(object : Callback<MaujaResponse> {
            override fun onResponse(call: Call<MaujaResponse>, response: Response<MaujaResponse>) {
                if (call.isCanceled || isFinishing || isDestroyed || !landEntryHolders.contains(holder)) return

                val responseBody = response.body()
                if (response.isSuccessful && responseBody?.statusCode == 200) {
                    holder.maujaItems = responseBody.data.orEmpty().filter {
                        !it.maujaCode.isNullOrBlank() && !it.maujaName.isNullOrBlank()
                    }

                    val names = mutableListOf("Select Mauja")
                    names.addAll(holder.maujaItems.map { it.maujaName.orEmpty() })

                    setSpinnerTextItems(holder.maujaSpinner, names)
                    val isAvailable = holder.maujaItems.isNotEmpty()
                    holder.maujaSpinner.isEnabled = isAvailable
                    holder.maujaDropdownArrow.isEnabled = isAvailable
                } else {
                    setSpinnerTextItems(holder.maujaSpinner, listOf("No Mauja available"))
                    holder.maujaSpinner.isEnabled = false
                    holder.maujaDropdownArrow.isEnabled = false
                    showToast(getApiErrorMessage(response = response, defaultMessage = "Unable to load Maujas."))
                }
            }

            override fun onFailure(call: Call<MaujaResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed || !landEntryHolders.contains(holder)) return
                setSpinnerTextItems(holder.maujaSpinner, listOf("Unable to load Maujas"))
                holder.maujaSpinner.isEnabled = false
                holder.maujaDropdownArrow.isEnabled = false
                showToast(throwable.message ?: "Unable to load Maujas.")
            }
        })
    }

    // ---------------------------------------------------------
    // RAKBA AND QUANTITY CALCULATION
    // ---------------------------------------------------------

    private fun setupLandCalculation(holder: LandEntryViewHolder) {
        val calculationWatcher = object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(editable: Editable?) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) { calculateLandValues(holder) }
        }
        holder.rakbaEditText.addTextChangedListener(calculationWatcher)
        holder.irrigatedEditText.addTextChangedListener(calculationWatcher)
    }

    private fun calculateLandValues(holder: LandEntryViewHolder) {
        val rakba = holder.rakbaEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()
        val irrigated = holder.irrigatedEditText.text?.toString()?.trim().orEmpty().toDoubleOrNull()

        holder.irrigatedEditText.error = null

        if (rakba == null || rakba < 0.0 || irrigated == null || irrigated < 0.0) {
            clearCalculatedValues(holder)
            return
        }

        if (irrigated > rakba) {
            holder.irrigatedEditText.error = "Irrigated land cannot exceed Rakba"
            clearCalculatedValues(holder)
            return
        }

        val unirrigated = rakba - irrigated
        val irrigatedQuantity = irrigated * QUINTALS_PER_ACRE
        val unirrigatedQuantity = unirrigated * QUINTALS_PER_ACRE

        holder.unirrigatedEditText.setText(formatDecimal(unirrigated))
        holder.expectedIrrigatedEditText.setText(formatDecimal(irrigatedQuantity))
        holder.expectedUnirrigatedEditText.setText(formatDecimal(unirrigatedQuantity))
    }

    private fun clearCalculatedValues(holder: LandEntryViewHolder) {
        holder.unirrigatedEditText.setText("")
        holder.expectedIrrigatedEditText.setText("")
        holder.expectedUnirrigatedEditText.setText("")
    }

    // ---------------------------------------------------------
    // PDF PICKER AND BASE64
    // ---------------------------------------------------------

    private fun validateAndReadPdf(uri: Uri) {
        val fileInformation = getFileInformation(uri)
        val fileName = fileInformation.first
        val fileSize = fileInformation.second

        if (fileName.isBlank() || !fileName.lowercase().endsWith(".pdf")) {
            clearSelectedDocument()
            showToast("Only PDF documents are allowed.")
            return
        }

        if (fileSize <= 0L) {
            clearSelectedDocument()
            showToast("Unable to determine the selected file size.")
            return
        }

        if (fileSize > MAXIMUM_PDF_SIZE_BYTES) {
            clearSelectedDocument()
            showToast("The PDF document must be smaller than 512 KB.")
            return
        }

        try {
            val fileBytes = contentResolver.openInputStream(uri)?.use { inputStream -> inputStream.readBytes() }

            if (fileBytes == null || fileBytes.isEmpty()) {
                clearSelectedDocument()
                showToast("The selected PDF document is empty.")
                return
            }

            if (fileBytes.size > MAXIMUM_PDF_SIZE_BYTES) {
                clearSelectedDocument()
                showToast("The PDF document must be smaller than 512 KB.")
                return
            }

            /*
             * Confirm the PDF signature: %PDF
             */
            if (!hasPdfSignature(fileBytes)) {
                clearSelectedDocument()
                showToast("The selected file is not a valid PDF document.")
                return
            }

            selectedDocumentUri = uri
            selectedDocumentName = fileName
            selectedDocumentBase64 = "data:application/pdf;base64," + Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            txtSelectedLandDocument.text = "$fileName (${formatFileSize(fileBytes.size.toLong())})"
            txtSelectedLandDocument.error = null

        } catch (exception: IOException) {
            clearSelectedDocument()
            showToast(exception.message ?: "Unable to read the selected document.")
        } catch (exception: SecurityException) {
            clearSelectedDocument()
            showToast("Permission to read the selected document was denied.")
        }
    }

    private fun getFileInformation(uri: Uri): Pair<String, Long> {
        var fileName = ""
        var fileSize = -1L

        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumnIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameColumnIndex >= 0) fileName = cursor.getString(nameColumnIndex).orEmpty()
                if (sizeColumnIndex >= 0 && !cursor.isNull(sizeColumnIndex)) fileSize = cursor.getLong(sizeColumnIndex)
            }
        }
        return Pair(fileName, fileSize)
    }

    private fun hasPdfSignature(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes[0].toInt().toChar() == '%' && bytes[1].toInt().toChar() == 'P' && bytes[2].toInt().toChar() == 'D' && bytes[3].toInt().toChar() == 'F'
    }

    private fun clearSelectedDocument() {
        selectedDocumentUri = null
        selectedDocumentBase64 = null
        selectedDocumentName = null
        txtSelectedLandDocument.text = "No document selected"
    }

    // ---------------------------------------------------------
    // VALIDATION AND REQUEST CREATION
    // ---------------------------------------------------------

    private fun validateAndSubmitLandRecords() {
        clearAllErrors()

        if (farmerId.isBlank()) { showToast("Farmer ID is missing."); return }
        if (districtId.isBlank()) { showToast("District ID is missing."); return }

        if (spLandType.selectedItemPosition <= 0) {
            imgLandTypeDropdown.requestFocus()
            showToast("Please select Land Type.")
            return
        }

        if (spSubdistrict.selectedItemPosition <= 0 || subdistrictItems.isEmpty()) {
            imgSubdistrictDropdown.requestFocus()
            showToast("Please select Subdistrict.")
            return
        }

        val selectedSubdistrictIndex = spSubdistrict.selectedItemPosition - 1

        if (selectedSubdistrictIndex !in subdistrictItems.indices) {
            showToast("The selected Subdistrict is invalid.")
            return
        }

        val subdistrictCode = subdistrictItems[selectedSubdistrictIndex].subdistrictCode.orEmpty().trim()
        if (subdistrictCode.isBlank()) {
            showToast("The selected Subdistrict code is missing.")
            return
        }

        val documentBase64 = selectedDocumentBase64
        if (documentBase64.isNullOrBlank()) {
            txtSelectedLandDocument.error = "Please select a valid PDF document"
            showToast("Please upload the land document.")
            return
        }

        if (landEntryHolders.isEmpty()) {
            showToast("Please add at least one land detail.")
            return
        }

        val landType = when (spLandType.selectedItemPosition) {
            1 -> "Owned"
            2 -> "Leased"
            else -> ""
        }

        val requestEntries = mutableListOf<LandRecordEntry>()
        landEntryHolders.forEachIndexed { index, holder ->
            val validatedEntry = validateAndCreateEntry(
                holder = holder,
                entryNumber = index + 1,
                subdistrictCode = subdistrictCode,
                landType = landType
            ) ?: return

            requestEntries.add(validatedEntry)
        }

        val request = SaveLandRecordsRequest(
            farmerId = farmerId,
            landEntries = requestEntries,
            landDocumentBase64 = documentBase64
        )

        submitLandRecords(request)
    }

    private fun validateAndCreateEntry(
        holder: LandEntryViewHolder,
        entryNumber: Int,
        subdistrictCode: String,
        landType: String
    ): LandRecordEntry? {
        val circle = holder.selectedCircle
        if (circle == null) {
            holder.circleDropdownArrow.requestFocus()
            showToast("Select Circle in Land Detail $entryNumber.")
            return null
        }

        val halka = holder.selectedHalka
        if (halka == null) {
            holder.halkaDropdownArrow.requestFocus()
            showToast("Select Halka in Land Detail $entryNumber.")
            return null
        }

        val mauja = holder.selectedMauja
        if (mauja == null) {
            holder.maujaDropdownArrow.requestFocus()
            showToast("Select Mauja in Land Detail $entryNumber.")
            return null
        }

        if (holder.cropSpinner.selectedItemPosition <= 0) {
            holder.cropDropdownArrow.requestFocus()
            showToast("Select Crop in Land Detail $entryNumber.")
            return null
        }

        val volumeNo = holder.volumeNoEditText.value()
        if (volumeNo.isBlank()) return showFieldError(holder.volumeNoEditText, "Enter Volume No in Land Detail $entryNumber.")

        val pageNo = holder.pageNoEditText.value()
        if (pageNo.isBlank()) return showFieldError(holder.pageNoEditText, "Enter Page No in Land Detail $entryNumber.")

        val plotNo = holder.plotNoEditText.value()
        if (plotNo.isBlank()) return showFieldError(holder.plotNoEditText, "Enter Plot No in Land Detail $entryNumber.")

        val khataNo = holder.khataNoEditText.value()
        if (khataNo.isBlank()) return showFieldError(holder.khataNoEditText, "Enter Khata No in Land Detail $entryNumber.")

        val rakbaText = holder.rakbaEditText.value()
        val rakba = rakbaText.toDoubleOrNull()
        if (rakba == null || rakba <= 0.0) return showFieldError(holder.rakbaEditText, "Enter a valid Rakba in Land Detail $entryNumber.")

        val irrigatedText = holder.irrigatedEditText.value()
        val irrigated = irrigatedText.toDoubleOrNull()
        if (irrigated == null || irrigated < 0.0) return showFieldError(holder.irrigatedEditText, "Enter valid Irrigated land in Land Detail $entryNumber.")

        if (irrigated > rakba) return showFieldError(holder.irrigatedEditText, "Irrigated land cannot exceed Rakba in Land Detail $entryNumber.")

        val unirrigated = rakba - irrigated
        val irrigatedQuantity = irrigated * QUINTALS_PER_ACRE
        val unirrigatedQuantity = unirrigated * QUINTALS_PER_ACRE

        val circleId = circle.circleCode.orEmpty().trim()
        val halkaId = halka.halkaCode.orEmpty().trim()
        val maujaId = mauja.maujaCode.orEmpty().trim()

        /*
         * Read names directly from the currently selected Spinner items.
         * This guarantees that the API receives exactly what the user sees
         * and selects in each dropdown.
         */
        val circleName = holder.circleSpinner.selectedItem?.toString()?.trim().orEmpty()
        val halkaName = holder.halkaSpinner.selectedItem?.toString()?.trim().orEmpty()
        val maujaName = holder.maujaSpinner.selectedItem?.toString()?.trim().orEmpty()
        val cropName = holder.cropSpinner.selectedItem?.toString()?.trim().orEmpty()

        if (circleId.isBlank() || halkaId.isBlank() || maujaId.isBlank()) {
            showToast("Dropdown code is missing in Land Detail $entryNumber.")
            return null
        }

        if (circleName.isBlank() || halkaName.isBlank() || maujaName.isBlank() || cropName.isBlank()) {
            showToast("Dropdown name is missing in Land Detail $entryNumber.")
            return null
        }

        /*
         * Total procured quantity must be the sum of the expected
         * irrigated and unirrigated quantities.
         */
        val procuredQuantity = irrigatedQuantity + unirrigatedQuantity

        return LandRecordEntry(
            subdistrictCode = subdistrictCode,
            landOwnerName = volumeNo,
            landOwnerRinPustikaNo = pageNo,
            landType = landType,
            khasaraNo = khataNo,
            rakba = formatDecimal(rakba),
            irrigatedLand = formatDecimal(irrigated),
            unirrigatedLand = formatDecimal(unirrigated),
            irrigatedQuantity = formatDecimal(irrigatedQuantity),
            unirrigatedQuantity = formatDecimal(unirrigatedQuantity),
            procuredQuantity = formatDecimal(procuredQuantity),
            circleId = circleId,
            circleName = circleName,
            maujaId = maujaId,
            maujaName = maujaName,
            halkaId = halkaId,
            halkaName = halkaName,
            plotNo = plotNo,
            crop = cropName
        )
    }

    private fun showFieldError(editText: EditText, message: String): LandRecordEntry? {
        editText.error = message
        editText.requestFocus()
        showToast(message)
        return null
    }

    // ---------------------------------------------------------
    // SAVE LAND RECORD API
    // ---------------------------------------------------------

    private fun submitLandRecords(request: SaveLandRecordsRequest) {
        setSubmitting(true)
        saveLandCall?.cancel()
        saveLandCall = RetrofitClient.apiService.saveLandRecords(request)

        saveLandCall?.enqueue(object : Callback<SaveLandRecordsResponse> {
            override fun onResponse(call: Call<SaveLandRecordsResponse>, response: Response<SaveLandRecordsResponse>) {
                if (isFinishing || isDestroyed) return

                setSubmitting(false)
                val responseBody = response.body()

                if (response.isSuccessful && responseBody?.statusCode == 200) {

                    /*
                     * Update the local completion status.
                     */
                    landDetail = 1

                    val successMessage = responseBody.message?.trim().orEmpty().ifBlank { "Land records updated successfully." }

                    /*
                     * Return the updated status to FarmerMainDashboardActivity.
                     * After receiving this result:
                     * - Land Details will become blurred.
                     * - Book Slot will become unblurred.
                     */
                    val resultIntent = Intent().apply {
                        putExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_BASIC_DETAIL, basicDetail)
                        putExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_LAND_DETAIL, landDetail)
                        putExtra(FarmerMainDashboardActivity.EXTRA_SUCCESS_MESSAGE, successMessage)
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()

                } else {
                    showToast(getApiErrorMessage(response = response, defaultMessage = responseBody?.message ?: "Unable to update land records."))
                }
            }

            override fun onFailure(call: Call<SaveLandRecordsResponse>, throwable: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                setSubmitting(false)
                showToast(throwable.message ?: "Unable to connect to the server.")
            }
        })
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        progressSubmit.visibility = if (isSubmitting) View.VISIBLE else View.GONE
        btnUpdate.isEnabled = !isSubmitting
        btnAddLandEntry.isEnabled = !isSubmitting
        btnUploadLandDocument.isEnabled = !isSubmitting
        spLandType.isEnabled = !isSubmitting
        imgLandTypeDropdown.isEnabled = !isSubmitting

        val isSubdistrictAvailable = subdistrictItems.isNotEmpty()
        spSubdistrict.isEnabled = !isSubmitting && isSubdistrictAvailable
        imgSubdistrictDropdown.isEnabled = !isSubmitting && isSubdistrictAvailable

        landEntryHolders.forEach { holder ->
            holder.removeButton.isEnabled = !isSubmitting
            holder.circleSpinner.isEnabled = !isSubmitting && circleItems.isNotEmpty()
            holder.circleDropdownArrow.isEnabled = holder.circleSpinner.isEnabled
            holder.halkaSpinner.isEnabled = !isSubmitting && holder.halkaItems.isNotEmpty()
            holder.halkaDropdownArrow.isEnabled = holder.halkaSpinner.isEnabled
            holder.maujaSpinner.isEnabled = !isSubmitting && holder.maujaItems.isNotEmpty()
            holder.maujaDropdownArrow.isEnabled = holder.maujaSpinner.isEnabled
            holder.cropSpinner.isEnabled = !isSubmitting
            holder.cropDropdownArrow.isEnabled = !isSubmitting
            holder.volumeNoEditText.isEnabled = !isSubmitting
            holder.pageNoEditText.isEnabled = !isSubmitting
            holder.plotNoEditText.isEnabled = !isSubmitting
            holder.khataNoEditText.isEnabled = !isSubmitting
            holder.rakbaEditText.isEnabled = !isSubmitting
            holder.irrigatedEditText.isEnabled = !isSubmitting
        }
    }

    // ---------------------------------------------------------
    // ARROW-ONLY DROPDOWN
    // ---------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun setupArrowOnlyDropdown(spinner: Spinner, arrowImage: ImageView) {
        /*
         * Consume all direct touch events on the Spinner.
         * Therefore, touching the displayed text will not
         * open the dropdown.
         */
        spinner.setOnTouchListener { _, _ -> true }

        /*
         * Prevent keyboard or directional selection from
         * opening the spinner through the text area.
         */
        spinner.isFocusable = false
        spinner.isFocusableInTouchMode = false

        /*
         * Open dropdown only through the arrow icon.
         */
        arrowImage.setOnClickListener {
            if (!spinner.isEnabled || !arrowImage.isEnabled) return@setOnClickListener
            spinner.performClick()
        }
    }

    private fun updateHolderArrowStates(holder: LandEntryViewHolder) {
        holder.circleDropdownArrow.isEnabled = holder.circleSpinner.isEnabled
        holder.halkaDropdownArrow.isEnabled = holder.halkaSpinner.isEnabled
        holder.maujaDropdownArrow.isEnabled = holder.maujaSpinner.isEnabled
        holder.cropDropdownArrow.isEnabled = holder.cropSpinner.isEnabled
    }

    // ---------------------------------------------------------
    // GENERAL HELPERS
    // ---------------------------------------------------------

    private fun setSpinnerTextItems(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun formatDecimal(value: Double): String {
        if (!value.isFinite()) return "0"
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }

    private fun formatFileSize(sizeBytes: Long): String = if (sizeBytes < 1024L) {
        "$sizeBytes bytes"
    } else {
        "${formatDecimal(sizeBytes.toDouble() / 1024.0)} KB"
    }

    private fun clearAllErrors() {
        txtSelectedLandDocument.error = null
        landEntryHolders.forEach { holder ->
            holder.volumeNoEditText.error = null
            holder.pageNoEditText.error = null
            holder.plotNoEditText.error = null
            holder.khataNoEditText.error = null
            holder.rakbaEditText.error = null
            holder.irrigatedEditText.error = null
        }
    }

    private fun disableForm() {
        btnUpdate.isEnabled = false
        btnAddLandEntry.isEnabled = false
        btnUploadLandDocument.isEnabled = false
        spLandType.isEnabled = false
        spSubdistrict.isEnabled = false
        imgLandTypeDropdown.isEnabled = false
        imgSubdistrictDropdown.isEnabled = false

        landEntryHolders.forEach { holder ->
            holder.circleSpinner.isEnabled = false
            holder.halkaSpinner.isEnabled = false
            holder.maujaSpinner.isEnabled = false
            holder.cropSpinner.isEnabled = false
            holder.circleDropdownArrow.isEnabled = false
            holder.halkaDropdownArrow.isEnabled = false
            holder.maujaDropdownArrow.isEnabled = false
            holder.cropDropdownArrow.isEnabled = false
        }
    }

    private fun <T> getApiErrorMessage(response: Response<T>, defaultMessage: String): String {
        return try {
            val errorText = response.errorBody()?.string().orEmpty()
            if (errorText.isBlank()) defaultMessage else JSONObject(errorText).optString("message", defaultMessage)
        } catch (_: Exception) {
            defaultMessage
        }
    }

    private fun EditText.value(): String = text?.toString()?.trim().orEmpty()

    private fun showToast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }

    override fun onDestroy() {
        blocksCall?.cancel()
        circlesCall?.cancel()
        saveLandCall?.cancel()
        landEntryHolders.forEach { holder ->
            holder.halkaApiCall?.cancel()
            holder.maujaApiCall?.cancel()
        }
        super.onDestroy()
    }

    // ---------------------------------------------------------
    // EACH DYNAMIC LAND CARD'S VIEW AND STATE
    // ---------------------------------------------------------

    private data class LandEntryViewHolder(
        val rootView: View,
        val title: TextView,
        val removeButton: ImageButton,
        val circleSpinner: Spinner,
        val circleDropdownArrow: ImageView,
        val halkaSpinner: Spinner,
        val halkaDropdownArrow: ImageView,
        val maujaSpinner: Spinner,
        val maujaDropdownArrow: ImageView,
        val cropSpinner: Spinner,
        val cropDropdownArrow: ImageView,
        val volumeNoEditText: EditText,
        val pageNoEditText: EditText,
        val plotNoEditText: EditText,
        val khataNoEditText: EditText,
        val rakbaEditText: EditText,
        val irrigatedEditText: EditText,
        val unirrigatedEditText: EditText,
        val expectedIrrigatedEditText: EditText,
        val expectedUnirrigatedEditText: EditText,

        var halkaItems: List<HalkaItem> = emptyList(),
        var maujaItems: List<MaujaItem> = emptyList(),
        var selectedCircle: CircleItem? = null,
        var selectedHalka: HalkaItem? = null,
        var selectedMauja: MaujaItem? = null,
        var halkaApiCall: Call<HalkaResponse>? = null,
        var maujaApiCall: Call<MaujaResponse>? = null
    )
}