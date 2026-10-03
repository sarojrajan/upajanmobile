package com.example.farmerapplication.farmer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.farmer.BankBranchItem
import com.example.farmerapplication.models.farmer.BankDetailsResponse
import com.example.farmerapplication.models.farmer.BankItem
import com.example.farmerapplication.models.farmer.BankResponse
import com.example.farmerapplication.models.farmer.BlockItem
import com.example.farmerapplication.models.farmer.BlockResponse
import com.example.farmerapplication.models.farmer.CircleItem
import com.example.farmerapplication.models.farmer.CircleResponse
import com.example.farmerapplication.models.farmer.FarmerFullRegistrationRequest
import com.example.farmerapplication.models.farmer.FarmerFullRegistrationResponse
import com.example.farmerapplication.models.farmer.HalkaItem
import com.example.farmerapplication.models.farmer.HalkaResponse
import com.example.farmerapplication.models.farmer.PanchayatItem
import com.example.farmerapplication.models.farmer.PanchayatResponse
import com.example.farmerapplication.models.farmer.VillageItem
import com.example.farmerapplication.models.farmer.VillageResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class FarmerFullRegActivity : ComponentActivity() {

    // ========================== VIEWS ==========================

    private lateinit var edtFarmerName: EditText
    private lateinit var edtFatherHusbandName: EditText
    private lateinit var edtAadhaarNumber: EditText
    private lateinit var edtMobileNumber: EditText
    private lateinit var edtDistrict: EditText
    private lateinit var edtIFSC: EditText
    private lateinit var edtAccountNumber: EditText

    private lateinit var spBlock: Spinner
    private lateinit var spPanchayat: Spinner
    private lateinit var spVillage: Spinner
    private lateinit var spCategory: Spinner
    private lateinit var spBank: Spinner
    private lateinit var spBranch: Spinner
    private lateinit var spCircle: Spinner
    private lateinit var spPatwariHalkaNumber: Spinner

    private lateinit var imgBlockArrow: ImageView
    private lateinit var imgPanchayatArrow: ImageView
    private lateinit var imgVillageArrow: ImageView
    private lateinit var imgCategoryArrow: ImageView
    private lateinit var imgBankArrow: ImageView
    private lateinit var imgBranchArrow: ImageView
    private lateinit var imgCircleArrow: ImageView
    private lateinit var imgPatwariHalkaArrow: ImageView

    private lateinit var btnUploadAadhaar: Button
    private lateinit var btnUploadBankPassbook: Button
    private lateinit var btnSubmitBasicDetails: Button

    private lateinit var txtSelectedAadhaarFile: TextView
    private lateinit var txtSelectedPassbookFile: TextView
    private lateinit var progressSubmit: ProgressBar

    // ====================== FARMER DETAILS ======================

    private var farmerName: String = ""
    private var userId: Int = 0
    private var farmerId: String = ""
    private var districtId: String = ""
    private var districtName: String = ""
    private var mobileNumber: String = ""
    private var aadhaarNumber: String = ""

    private var basicDetail: Int = 0
    private var landDetail: Int = 0

    // ====================== DROPDOWN DATA =======================

    private val blockList = mutableListOf<BlockItem>()
    private val panchayatList = mutableListOf<PanchayatItem>()
    private val villageList = mutableListOf<VillageItem>()
    private val bankList = mutableListOf<BankItem>()
    private val branchList = mutableListOf<BankBranchItem>()
    private val circleList = mutableListOf<CircleItem>()
    private val halkaList = mutableListOf<HalkaItem>()

    private var selectedBlock: BlockItem? = null
    private var selectedPanchayat: PanchayatItem? = null
    private var selectedVillage: VillageItem? = null
    private var selectedCategoryValue: String? = null
    private var selectedBank: BankItem? = null
    private var selectedBranch: BankBranchItem? = null
    private var selectedCircle: CircleItem? = null
    private var selectedHalka: HalkaItem? = null

    private var isBlockSpinnerReady = false
    private var isPanchayatSpinnerReady = false
    private var isVillageSpinnerReady = false
    private var isBankSpinnerReady = false
    private var isBranchSpinnerReady = false
    private var isCircleSpinnerReady = false
    private var isHalkaSpinnerReady = false
    private var isCategorySpinnerReady = true

    // ========================= API CALLS ========================

    private var blockApiCall: Call<BlockResponse>? = null
    private var panchayatApiCall: Call<PanchayatResponse>? = null
    private var villageApiCall: Call<VillageResponse>? = null
    private var bankApiCall: Call<BankResponse>? = null
    private var branchApiCall: Call<BankDetailsResponse>? = null
    private var circleApiCall: Call<CircleResponse>? = null
    private var halkaApiCall: Call<HalkaResponse>? = null

    // ====================== DOCUMENT DATA =======================

    private var aadhaarDocumentUri: Uri? = null
    private var passbookDocumentUri: Uri? = null

    private val aadhaarDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { processSelectedDocument(uri = it, documentType = DocumentType.AADHAAR) }
    }

    private val passbookDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { processSelectedDocument(uri = it, documentType = DocumentType.PASSBOOK) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmer_full_reg_layout)

        initializeViews()
        loadFarmerDetailsFromIntent()
        prefillFarmerDetails()

        setupSpinnerClickInterceptors()
        initializeStaticDropdowns()
        initializeDependentDropdowns()

        setupDropdownListeners()
        setupClickListeners()

        if (validateInitialFarmerData()) {
            loadBlocks()
            loadBanks()
            loadCircles()
        }
    }

    // ===================== VIEW INITIALIZATION =====================

    private fun initializeViews() {
        edtFarmerName = findViewById(R.id.edtFarmerName)
        edtFatherHusbandName = findViewById(R.id.edtFatherHusbandName)
        edtAadhaarNumber = findViewById(R.id.edtAadhaarNumber)
        edtMobileNumber = findViewById(R.id.edtMobileNumber)
        edtDistrict = findViewById(R.id.edtDistrict)
        edtIFSC = findViewById(R.id.edtIFSC)
        edtAccountNumber = findViewById(R.id.edtAccountNumber)

        spBlock = findViewById(R.id.spBlock)
        spPanchayat = findViewById(R.id.spPanchayat)
        spVillage = findViewById(R.id.spVillage)
        spCategory = findViewById(R.id.spCategory)
        spBank = findViewById(R.id.spBank)
        spBranch = findViewById(R.id.spBranch)
        spCircle = findViewById(R.id.spCircle)
        spPatwariHalkaNumber = findViewById(R.id.spPatwariHalkaNumber)

        imgBlockArrow = findViewById(R.id.imgBlockArrow)
        imgPanchayatArrow = findViewById(R.id.imgPanchayatArrow)
        imgVillageArrow = findViewById(R.id.imgVillageArrow)
        imgCategoryArrow = findViewById(R.id.imgCategoryArrow)
        imgBankArrow = findViewById(R.id.imgBankArrow)
        imgBranchArrow = findViewById(R.id.imgBranchArrow)
        imgCircleArrow = findViewById(R.id.imgCircleArrow)
        imgPatwariHalkaArrow = findViewById(R.id.imgPatwariHalkaArrow)

        btnUploadAadhaar = findViewById(R.id.btnUploadAadhaar)
        btnUploadBankPassbook = findViewById(R.id.btnUploadBankPassbook)
        btnSubmitBasicDetails = findViewById(R.id.btnSubmitBasicDetails)

        txtSelectedAadhaarFile = findViewById(R.id.txtSelectedAadhaarFile)
        txtSelectedPassbookFile = findViewById(R.id.txtSelectedPassbookFile)
        progressSubmit = findViewById(R.id.progressSubmit)
    }

    // ===================== SPINNER CLICK HANDLING =====================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinnerClickInterceptors() {
        val spinners = listOf(spBlock, spPanchayat, spVillage, spCategory, spBank, spBranch, spCircle, spPatwariHalkaNumber)

        for (spinner in spinners) {
            // Consume all touch events; only the arrow buttons below can open them.
            spinner.setOnTouchListener { _, _ -> true }
        }

        // Searchable dialog instead of normal spinner popup for BLOCK.
        imgBlockArrow.setOnClickListener {
            if (isBlockSpinnerReady) {
                val values = blockList.mapNotNull { it.subdistrictName }
                showSearchableDropdown("Select Block", "Search block...", values) { selectedIndex ->
                    spBlock.setSelection(selectedIndex + 1) // +1 because spinner position 0 = "Select Block"
                }
            }
        }

        // Searchable dialog for PANCHAYAT.
        imgPanchayatArrow.setOnClickListener {
            if (isPanchayatSpinnerReady) {
                val values = panchayatList.mapNotNull { it.panchayatName }
                showSearchableDropdown("Select Panchayat", "Search panchayat...", values) { selectedIndex ->
                    spPanchayat.setSelection(selectedIndex + 1)
                }
            }
        }

        // Existing normal spinner behaviour remains unchanged for VILLAGE.
        imgVillageArrow.setOnClickListener { if (isVillageSpinnerReady) spVillage.performClick() }

        // Existing normal spinner behaviour remains unchanged for CATEGORY.
        imgCategoryArrow.setOnClickListener { if (isCategorySpinnerReady) spCategory.performClick() }

        // Searchable dialog for BANK.
        imgBankArrow.setOnClickListener {
            if (isBankSpinnerReady) {
                val values = bankList.mapNotNull { it.bankName }
                showSearchableDropdown("Select Bank", "Search bank...", values) { selectedIndex ->
                    spBank.setSelection(selectedIndex + 1)
                }
            }
        }

        // Searchable dialog for BRANCH.
        imgBranchArrow.setOnClickListener {
            if (isBranchSpinnerReady) {
                val values = branchList.mapNotNull { it.branchName }
                showSearchableDropdown("Select Branch", "Search branch...", values) { selectedIndex ->
                    spBranch.setSelection(selectedIndex + 1)
                }
            }
        }

        // Existing normal spinner behaviour remains unchanged for CIRCLE.
        imgCircleArrow.setOnClickListener { if (isCircleSpinnerReady) spCircle.performClick() }

        // Existing normal spinner behaviour remains unchanged for HALKA.
        imgPatwariHalkaArrow.setOnClickListener { if (isHalkaSpinnerReady) spPatwariHalkaNumber.performClick() }
    }

    // ===================== MODERN SEARCHABLE DROPDOWN =====================

    private fun showSearchableDropdown(title: String, searchHint: String, values: List<String>, onItemSelected: (Int) -> Unit) {
        if (values.isEmpty()) {
            showToast("No data available.")
            return
        }

        // Main container created programmatically (NO separate XML file is required).
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(6), dpToPx(20), dpToPx(12))
        }

        // SEARCH BOX
        val searchBox = EditText(this).apply {
            hint = searchHint
            textSize = 16f
            setTextColor(Color.BLACK)
            setHintTextColor(Color.parseColor("#757575"))
            isSingleLine = true
            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.WHITE)
                setStroke(dpToPx(1), Color.parseColor("#BDBDBD"))
            }
        }

        val searchBoxParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dpToPx(12) }
        mainContainer.addView(searchBox, searchBoxParams)

        // LIST
        val listView = ListView(this).apply { dividerHeight = 1; isVerticalScrollBarEnabled = true }
        val listViewParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(380))
        mainContainer.addView(listView, listViewParams)

        // Keep original index along with displayed value to know actual Spinner position after filtering.
        val originalItems = values.mapIndexed { index, value -> SearchableDropdownItem(originalIndex = index, displayText = value) }
        val filteredItems = originalItems.toMutableList()
        val listAdapter = SearchResultAdapter(context = this, items = filteredItems)
        listView.adapter = listAdapter

        // DIALOG
        val dialog = AlertDialog.Builder(this).setTitle(title).setView(mainContainer).setNegativeButton("Cancel", null).create()

        // SEARCH FILTER
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                filteredItems.clear()
                if (query.isBlank()) {
                    filteredItems.addAll(originalItems)
                } else {
                    filteredItems.addAll(originalItems.filter { it.displayText.contains(query, ignoreCase = true) })
                }
                listAdapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ITEM SELECT
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in filteredItems.indices) {
                val selectedItem = filteredItems[position]
                onItemSelected(selectedItem.originalIndex) // Return ORIGINAL position, not filtered position
                dialog.dismiss()
            }
        }

        // Close keyboard when dialog closes.
        dialog.setOnDismissListener {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchBox.windowToken, 0)
        }

        // Automatically focus search box and open keyboard when dialog appears.
        dialog.setOnShowListener {
            searchBox.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            searchBox.postDelayed({
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT)
            }, 150)
        }

        dialog.show()
    }

    // ====================== FARMER DETAILS ======================

    private fun loadFarmerDetailsFromIntent() {
        farmerName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_NAME).orEmpty()
        userId = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_USER_ID, 0)
        farmerId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_FARMER_ID).orEmpty()
        districtId = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_ID).orEmpty()
        districtName = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_DISTRICT_NAME).orEmpty()
        mobileNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_MOBILE_NUMBER).orEmpty()
        aadhaarNumber = intent.getStringExtra(FarmerMainDashboardActivity.EXTRA_AADHAAR_NUMBER).orEmpty()
        basicDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_BASIC_DETAIL, 0)
        landDetail = intent.getIntExtra(FarmerMainDashboardActivity.EXTRA_LAND_DETAIL, 0)
    }

    private fun prefillFarmerDetails() {
        edtFarmerName.setText(farmerName.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: "Not Available")
        edtAadhaarNumber.setText(formatAadhaarNumber(aadhaarNumber))
        edtMobileNumber.setText(mobileNumber.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: "Not Available")
        edtDistrict.setText(districtName.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) } ?: "Not Available")

        setPrefilledFieldState(edtFarmerName)
        setPrefilledFieldState(edtAadhaarNumber)
        setPrefilledFieldState(edtMobileNumber)
        setPrefilledFieldState(edtDistrict)
    }

    private fun setPrefilledFieldState(editText: EditText) {
        editText.isEnabled = false
        editText.isFocusable = false
        editText.isFocusableInTouchMode = false
        editText.isClickable = false
        editText.isLongClickable = false
        editText.alpha = 1.0f
    }

    private fun formatAadhaarNumber(aadhaar: String): String {
        val cleanAadhaar = aadhaar.replace(" ", "").replace("-", "").trim()
        if (cleanAadhaar.isBlank() || cleanAadhaar.equals("null", ignoreCase = true)) return "Not Available"
        if (cleanAadhaar.length == 12 && cleanAadhaar.all { it.isDigit() }) return "XXXX XXXX ${cleanAadhaar.takeLast(4)}"
        return cleanAadhaar
    }

    private fun validateInitialFarmerData(): Boolean {
        if (farmerId.isBlank() || districtId.isBlank()) {
            showToast("Required details are missing. Please login again.")
            btnSubmitBasicDetails.isEnabled = false
            return false
        }
        return true
    }

    // ====================== SPINNER INITIALIZATION =====================

    private fun initializeDependentDropdowns() {
        setStringSpinner(spBlock, listOf("Loading blocks..."))
        setStringSpinner(spPanchayat, listOf("Select block first"))
        setStringSpinner(spVillage, listOf("Select panchayat first"))
        setStringSpinner(spBank, listOf("Loading banks..."))
        setStringSpinner(spBranch, listOf("Select bank first"))
        setStringSpinner(spCircle, listOf("Loading circles..."))
        setStringSpinner(spPatwariHalkaNumber, listOf("Select circle first"))

        setSpinnerEnabled(spBlock, imgBlockArrow, false)
        setSpinnerEnabled(spPanchayat, imgPanchayatArrow, false)
        setSpinnerEnabled(spVillage, imgVillageArrow, false)
        setSpinnerEnabled(spBank, imgBankArrow, false)
        setSpinnerEnabled(spBranch, imgBranchArrow, false)
        setSpinnerEnabled(spCircle, imgCircleArrow, false)
        setSpinnerEnabled(spPatwariHalkaNumber, imgPatwariHalkaArrow, false)
    }

    private fun initializeStaticDropdowns() {
        val categories = listOf("Select Category", "General", "OBC", "SC", "ST", "Other")
        setStringSpinner(spCategory, categories)
        setSpinnerEnabled(spCategory, imgCategoryArrow, true)

        spCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryValue = when (position) {
                    1 -> "1" // General
                    2 -> "2" // OBC
                    3 -> "4" // SC
                    4 -> "5" // ST
                    5 -> "6" // Other
                    else -> null
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryValue = null
            }
        }
    }

    private fun setSpinnerEnabled(spinner: Spinner, arrow: ImageView, enabled: Boolean) {
        arrow.alpha = if (enabled) ENABLED_ARROW_ALPHA else DISABLED_ARROW_ALPHA
        spinner.alpha = if (enabled) ENABLED_SPINNER_ALPHA else DISABLED_SPINNER_ALPHA
    }

    // ====================== DEPENDENT SPINNERS ======================

    private fun setupDropdownListeners() {
        // BLOCK
        spBlock.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isBlockSpinnerReady || position == 0) {
                    selectedBlock = null
                    if (isBlockSpinnerReady) resetPanchayatSpinner("Select block first")
                    return
                }
                selectedBlock = blockList[position - 1]
                resetPanchayatSpinner("Loading panchayats...")
                selectedBlock?.subdistrictCode?.let { loadPanchayats(it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedBlock = null }
        }

        // PANCHAYAT
        spPanchayat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isPanchayatSpinnerReady || position == 0) {
                    selectedPanchayat = null
                    if (isPanchayatSpinnerReady) resetVillageSpinner("Select panchayat first")
                    return
                }
                selectedPanchayat = panchayatList[position - 1]
                resetVillageSpinner("Loading villages...")
                selectedPanchayat?.localBodyCode?.let { loadVillages(it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedPanchayat = null }
        }

        // VILLAGE
        spVillage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isVillageSpinnerReady || position == 0) {
                    selectedVillage = null
                    return
                }
                selectedVillage = villageList[position - 1]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedVillage = null }
        }

        // BANK
        spBank.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isBankSpinnerReady || position == 0) {
                    selectedBank = null
                    if (isBankSpinnerReady) resetBranchSpinner("Select bank first")
                    return
                }
                selectedBank = bankList[position - 1]
                resetBranchSpinner("Loading branches...")
                selectedBank?.bankId?.let { loadBranches(it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedBank = null }
        }

        // BRANCH
        spBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isBranchSpinnerReady || position == 0) {
                    selectedBranch = null
                    edtIFSC.setText("")
                    return
                }
                selectedBranch = branchList[position - 1]
                edtIFSC.setText(selectedBranch?.ifscCode ?: "")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedBranch = null }
        }

        // CIRCLE
        spCircle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isCircleSpinnerReady || position == 0) {
                    selectedCircle = null
                    if (isCircleSpinnerReady) resetHalkaSpinner("Select circle first")
                    return
                }
                selectedCircle = circleList[position - 1]
                resetHalkaSpinner("Loading halkas...")
                selectedCircle?.circleCode?.let { loadHalkas(districtId, it) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedCircle = null }
        }

        // HALKA
        spPatwariHalkaNumber.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isHalkaSpinnerReady || position == 0) {
                    selectedHalka = null
                    return
                }
                selectedHalka = halkaList[position - 1]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) { selectedHalka = null }
        }
    }

    // ========================== API METHODS ==========================

    private fun loadBlocks() {
        blockApiCall = RetrofitClient.apiService.getBlocks(districtId)
        blockApiCall?.enqueue(object : Callback<BlockResponse> {
            override fun onResponse(call: Call<BlockResponse>, response: Response<BlockResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    blockList.clear()
                    blockList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Block").apply { addAll(blockList.mapNotNull { it.subdistrictName }) }
                    setStringSpinner(spBlock, names)
                    isBlockSpinnerReady = true
                    setSpinnerEnabled(spBlock, imgBlockArrow, true)
                } else {
                    showToast("Failed to load blocks")
                }
            }
            override fun onFailure(call: Call<BlockResponse>, t: Throwable) {}
        })
    }

    private fun loadPanchayats(subdistrictCode: String) {
        panchayatApiCall = RetrofitClient.apiService.getPanchayats(subdistrictCode)
        panchayatApiCall?.enqueue(object : Callback<PanchayatResponse> {
            override fun onResponse(call: Call<PanchayatResponse>, response: Response<PanchayatResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    panchayatList.clear()
                    panchayatList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Panchayat").apply { addAll(panchayatList.mapNotNull { it.panchayatName }) }
                    setStringSpinner(spPanchayat, names)
                    isPanchayatSpinnerReady = true
                    setSpinnerEnabled(spPanchayat, imgPanchayatArrow, true)
                }
            }
            override fun onFailure(call: Call<PanchayatResponse>, t: Throwable) {}
        })
    }

    private fun loadVillages(localBodyCode: String) {
        villageApiCall = RetrofitClient.apiService.getVillages(localBodyCode)
        villageApiCall?.enqueue(object : Callback<VillageResponse> {
            override fun onResponse(call: Call<VillageResponse>, response: Response<VillageResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    villageList.clear()
                    villageList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Village").apply { addAll(villageList.mapNotNull { it.villageName }) }
                    setStringSpinner(spVillage, names)
                    isVillageSpinnerReady = true
                    setSpinnerEnabled(spVillage, imgVillageArrow, true)
                }
            }
            override fun onFailure(call: Call<VillageResponse>, t: Throwable) {}
        })
    }

    private fun loadBanks() {
        bankApiCall = RetrofitClient.apiService.getBanks()
        bankApiCall?.enqueue(object : Callback<BankResponse> {
            override fun onResponse(call: Call<BankResponse>, response: Response<BankResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    bankList.clear()
                    bankList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Bank").apply { addAll(bankList.mapNotNull { it.bankName }) }
                    setStringSpinner(spBank, names)
                    isBankSpinnerReady = true
                    setSpinnerEnabled(spBank, imgBankArrow, true)
                }
            }
            override fun onFailure(call: Call<BankResponse>, t: Throwable) {}
        })
    }

    private fun loadBranches(bankId: String) {
        branchApiCall = RetrofitClient.apiService.getBankDetails(bankId)
        branchApiCall?.enqueue(object : Callback<BankDetailsResponse> {
            override fun onResponse(call: Call<BankDetailsResponse>, response: Response<BankDetailsResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    branchList.clear()
                    branchList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Branch").apply { addAll(branchList.mapNotNull { it.branchName }) }
                    setStringSpinner(spBranch, names)
                    isBranchSpinnerReady = true
                    setSpinnerEnabled(spBranch, imgBranchArrow, true)
                }
            }
            override fun onFailure(call: Call<BankDetailsResponse>, t: Throwable) {}
        })
    }

    private fun loadCircles() {
        circleApiCall = RetrofitClient.apiService.getCircles(districtId)
        circleApiCall?.enqueue(object : Callback<CircleResponse> {
            override fun onResponse(call: Call<CircleResponse>, response: Response<CircleResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    circleList.clear()
                    circleList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Circle").apply { addAll(circleList.mapNotNull { it.circleName }) }
                    setStringSpinner(spCircle, names)
                    isCircleSpinnerReady = true
                    setSpinnerEnabled(spCircle, imgCircleArrow, true)
                }
            }
            override fun onFailure(call: Call<CircleResponse>, t: Throwable) {}
        })
    }

    private fun loadHalkas(districtCode: String, circleCode: String) {
        halkaApiCall = RetrofitClient.apiService.getHalkas(districtCode, circleCode)
        halkaApiCall?.enqueue(object : Callback<HalkaResponse> {
            override fun onResponse(call: Call<HalkaResponse>, response: Response<HalkaResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body()?.statusCode == 200) {
                    halkaList.clear()
                    halkaList.addAll(response.body()?.data ?: emptyList())
                    val names = mutableListOf("Select Halka").apply { addAll(halkaList.mapNotNull { it.halkaName }) }
                    setStringSpinner(spPatwariHalkaNumber, names)
                    isHalkaSpinnerReady = true
                    setSpinnerEnabled(spPatwariHalkaNumber, imgPatwariHalkaArrow, true)
                }
            }
            override fun onFailure(call: Call<HalkaResponse>, t: Throwable) {}
        })
    }

    // ========================== RESET METHODS ==========================

    private fun resetPanchayatSpinner(msg: String) {
        isPanchayatSpinnerReady = false
        selectedPanchayat = null
        panchayatList.clear()
        setStringSpinner(spPanchayat, listOf(msg))
        setSpinnerEnabled(spPanchayat, imgPanchayatArrow, false)
        resetVillageSpinner("Select panchayat first")
    }

    private fun resetVillageSpinner(msg: String) {
        isVillageSpinnerReady = false
        selectedVillage = null
        villageList.clear()
        setStringSpinner(spVillage, listOf(msg))
        setSpinnerEnabled(spVillage, imgVillageArrow, false)
    }

    private fun resetBranchSpinner(msg: String) {
        isBranchSpinnerReady = false
        selectedBranch = null
        branchList.clear()
        setStringSpinner(spBranch, listOf(msg))
        setSpinnerEnabled(spBranch, imgBranchArrow, false)
        edtIFSC.setText("")
    }

    private fun resetHalkaSpinner(msg: String) {
        isHalkaSpinnerReady = false
        selectedHalka = null
        halkaList.clear()
        setStringSpinner(spPatwariHalkaNumber, listOf(msg))
        setSpinnerEnabled(spPatwariHalkaNumber, imgPatwariHalkaArrow, false)
    }

    private fun setStringSpinner(spinner: Spinner, values: List<String>) {
        val adapter = WhiteSpinnerAdapter(this, values)
        spinner.adapter = adapter
        spinner.setSelection(0, false)
    }

    // ======================== CLICK EVENTS =========================

    private fun setupClickListeners() {
        btnUploadAadhaar.setOnClickListener {
            aadhaarDocumentLauncher.launch(arrayOf("image/png", "image/jpeg", "application/pdf"))
        }

        btnUploadBankPassbook.setOnClickListener {
            passbookDocumentLauncher.launch(arrayOf("image/png", "image/jpeg", "application/pdf"))
        }

        btnSubmitBasicDetails.setOnClickListener { submitBasicDetails() }
    }

    // ======================== DOCUMENT UPLOAD =========================

    private fun processSelectedDocument(uri: Uri, documentType: DocumentType) {
        val fileName = getFileName(uri)
        if (!isAllowedFileExtension(fileName)) {
            showToast("Only .jpg, .jpeg, .png and .pdf files are allowed.")
            return
        }

        val fileSize = getFileSize(uri)
        if (fileSize > MAXIMUM_FILE_SIZE_BYTES) {
            showToast("Document size must be less than or equal to 512 KB.")
            return
        }

        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {}

        when (documentType) {
            DocumentType.AADHAAR -> {
                aadhaarDocumentUri = uri
                txtSelectedAadhaarFile.text = "$fileName\n${formatFileSize(fileSize)}"
            }
            DocumentType.PASSBOOK -> {
                passbookDocumentUri = uri
                txtSelectedPassbookFile.text = "$fileName\n${formatFileSize(fileSize)}"
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) fileName = cursor.getString(index).orEmpty()
            }
        }
        return fileName.ifBlank { uri.lastPathSegment.orEmpty() }
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) fileSize = cursor.getLong(index)
            }
        }
        return fileSize
    }

    private fun isAllowedFileExtension(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT) in ALLOWED_FILE_EXTENSIONS
    private fun formatFileSize(size: Long): String = String.format(Locale.getDefault(), "%.2f KB", size / 1024.0)

    private fun uriToBase64(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                Base64.encodeToString(inputStream.readBytes(), Base64.NO_WRAP)
            } ?: ""
        } catch (_: Exception) { "" }
    }

    // ========================= FORM SUBMIT =========================

    private fun submitBasicDetails() {
        val fatherHusbandName = edtFatherHusbandName.text.toString().trim()
        val ifscCode = edtIFSC.text.toString().trim().uppercase(Locale.ROOT)
        val accountNumber = edtAccountNumber.text.toString().trim()

        when {
            farmerId.isBlank() -> showToast("Farmer ID is missing.")
            fatherHusbandName.isBlank() -> { edtFatherHusbandName.error = "Required"; edtFatherHusbandName.requestFocus() }
            selectedBlock == null -> showToast("Please select a block.")
            selectedPanchayat == null -> showToast("Please select a panchayat.")
            selectedVillage == null -> showToast("Please select a village.")
            selectedCategoryValue == null -> showToast("Please select a category.")
            selectedBank == null -> showToast("Please select a bank.")
            selectedBranch == null -> showToast("Please select a branch.")
            ifscCode.isBlank() || !IFSC_REGEX.matches(ifscCode) -> { edtIFSC.error = "Valid IFSC required"; edtIFSC.requestFocus() }
            accountNumber.isBlank() || accountNumber.length < 8 -> { edtAccountNumber.error = "Valid Account required"; edtAccountNumber.requestFocus() }
            selectedCircle == null -> showToast("Please select a circle.")
            selectedHalka == null -> showToast("Please select a halka.")
            aadhaarDocumentUri == null -> showToast("Aadhaar document is required.")
            passbookDocumentUri == null -> showToast("Bank passbook document is required.")
            else -> {
                setSubmitting(true)

                val req = FarmerFullRegistrationRequest(
                    farmerId = farmerId,
                    farmerName = farmerName,
                    fatherHusName = fatherHusbandName,
                    mobile = mobileNumber,
                    aadhaar = aadhaarNumber,
                    category = selectedCategoryValue!!,
                    halkaNo = selectedHalka?.halkaCode ?: "",
                    districtId = districtId,
                    blockId = selectedBlock?.subdistrictCode ?: "",
                    panchayatId = selectedPanchayat?.localBodyCode ?: "",
                    villageId = selectedVillage?.villageCode ?: "",
                    villageName = selectedVillage?.villageName ?: "",
                    bankId = selectedBank?.bankId ?: "",
                    bankName = selectedBank?.bankName ?: "",
                    branchId = selectedBranch?.branchId ?: "",
                    ifsc = ifscCode,
                    accountNo = accountNumber,
                    aadhaarBase64 = uriToBase64(aadhaarDocumentUri!!),
                    bankBase64 = uriToBase64(passbookDocumentUri!!)
                )

                RetrofitClient.apiService.submitFullRegistration(req).enqueue(object : Callback<FarmerFullRegistrationResponse> {
                    override fun onResponse(call: Call<FarmerFullRegistrationResponse>, response: Response<FarmerFullRegistrationResponse>) {
                        setSubmitting(false)
                        if (response.isSuccessful && response.body()?.statusCode == 200) {
                            basicDetail = 1 // Update local status.
                            val existingSuccessMessage = response.body()?.message?.trim().orEmpty().ifBlank { "Basic Details filled Successfully" }
                            val finalSuccessMessage = "$existingSuccessMessage Please fill the Land Details."

                            // Return updated completion status to dashboard.
                            val resultIntent = Intent().apply {
                                putExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_BASIC_DETAIL, basicDetail)
                                putExtra(FarmerMainDashboardActivity.EXTRA_UPDATED_LAND_DETAIL, landDetail)
                                putExtra(FarmerMainDashboardActivity.EXTRA_SUCCESS_MESSAGE, finalSuccessMessage)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {
                            showToast(response.body()?.message ?: "Failed to submit basic details")
                        }
                    }

                    override fun onFailure(call: Call<FarmerFullRegistrationResponse>, t: Throwable) {
                        setSubmitting(false)
                        showToast("Network Error: ${t.localizedMessage}")
                    }
                })
            }
        }
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        progressSubmit.visibility = if (isSubmitting) View.VISIBLE else View.GONE
        btnSubmitBasicDetails.isEnabled = !isSubmitting
    }

    private fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private fun dpToPx(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        blockApiCall?.cancel()
        panchayatApiCall?.cancel()
        villageApiCall?.cancel()
        bankApiCall?.cancel()
        branchApiCall?.cancel()
        circleApiCall?.cancel()
        halkaApiCall?.cancel()
        super.onDestroy()
    }

    // ========================= SEARCH MODEL =========================

    private data class SearchableDropdownItem(val originalIndex: Int, val displayText: String)

    private enum class DocumentType { AADHAAR, PASSBOOK }

    companion object {
        private const val MAXIMUM_FILE_SIZE_BYTES = 512L * 1024L
        private const val ENABLED_ARROW_ALPHA = 1.0f
        private const val DISABLED_ARROW_ALPHA = 0.35f
        private const val ENABLED_SPINNER_ALPHA = 1.0f
        private const val DISABLED_SPINNER_ALPHA = 0.65f
        private val ALLOWED_FILE_EXTENSIONS = setOf("jpg", "jpeg", "png", "pdf")
        private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    }

    // ===================== SEARCH RESULT ADAPTER =====================

    private class SearchResultAdapter(
        context: Context,
        private val items: List<SearchableDropdownItem>
    ) : ArrayAdapter<SearchableDropdownItem>(context, android.R.layout.simple_list_item_1, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val textView = view.findViewById<TextView>(android.R.id.text1)

            textView.text = items[position].displayText
            textView.setTextColor(Color.BLACK)
            textView.textSize = 16f

            val density = context.resources.displayMetrics.density
            textView.setPadding((16 * density).toInt(), (15 * density).toInt(), (16 * density).toInt(), (15 * density).toInt())

            return view
        }
    }
}

// ===================== ORIGINAL SPINNER ADAPTER =====================

private class WhiteSpinnerAdapter(
    context: Context,
    private val values: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).apply {
            (this as TextView).setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(dpToPx(context, 8), 0, dpToPx(context, 44), 0)
        }
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).apply {
            (this as TextView).setTextColor(Color.BLACK)
            textSize = 16f
            setPadding(dpToPx(context, 18), dpToPx(context, 14), dpToPx(context, 18), dpToPx(context, 14))
        }
    }

    private fun dpToPx(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}