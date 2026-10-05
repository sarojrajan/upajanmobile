package com.example.farmerapplication.msp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.farmerapplication.R
import com.example.farmerapplication.api.RetrofitClient
import com.example.farmerapplication.models.SavePaddyLiftRecordsRequest
import com.example.farmerapplication.models.SavePaddyLiftRecordsResponse
import com.example.farmerapplication.models.VehicleEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

class MonitorDetailsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MSP_CENTRE_ID = "extra_msp_centre_id"
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val CAMERA_PERMISSION_REQUEST = 1002
        private const val MIN_IMAGE_SIZE = 10 * 1024
        private const val MAX_IMAGE_SIZE = 512 * 1024
    }

    private lateinit var edtRoNumber: EditText
    private lateinit var llDynamicVehicleContainer: LinearLayout
    private lateinit var btnAddVehicle: Button
    private lateinit var btnSubmit: Button
    private lateinit var progressSubmit: ProgressBar

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var mspCentreId: String = ""
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var vehicleCount = 0

    // Compressed JPEG byte array stored per vehicle View
    private val vehicleImages = mutableMapOf<View, ByteArray>()
    private var currentVehicleView: View? = null
    private var cameraImageFile: File? = null
    private var cameraImageUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null && currentVehicleView != null) {
                processCapturedImage(currentVehicleView!!, cameraImageUri!!)
            } else {
                Toast.makeText(this, "Image capture cancelled.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.monitor_details_layout)
        initializeViews()
        receiveMSPCentreId()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupClickListeners()
        setupBackPressHandler()
        addVehicle()
        requestLocationPermission()
    }

    private fun initializeViews() {
        edtRoNumber = findViewById(R.id.edtRoNumber)
        llDynamicVehicleContainer = findViewById(R.id.llDynamicVehicleContainer)
        btnAddVehicle = findViewById(R.id.btnAddVehicle)
        btnSubmit = findViewById(R.id.btnSubmit)
        progressSubmit = findViewById(R.id.progressSubmit)
    }

    private fun receiveMSPCentreId() {
        mspCentreId = intent.getStringExtra(EXTRA_MSP_CENTRE_ID)?.trim().orEmpty()
        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        btnAddVehicle.setOnClickListener { addVehicle() }
        btnSubmit.setOnClickListener { submitPaddyLiftRecords() }
    }

    // Location permissions and fetching
    private fun requestLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    private fun fetchCurrentLocation() {
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                } else {
                    requestFreshLocation()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestFreshLocation() {
        val fineGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return

        val locationRequest = com.google.android.gms.location.LocationRequest
            .Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    // Dynamic vehicle UI management
    private fun addVehicle() {
        vehicleCount++
        val vehicleView = LayoutInflater.from(this).inflate(R.layout.vehicle_detail_item, llDynamicVehicleContainer, false)

        val txtVehicleHeading = vehicleView.findViewById<TextView>(R.id.txtVehicleNumberHeading)
        val btnDelete = vehicleView.findViewById<ImageButton>(R.id.btnDeleteVehicle)
        val btnCapture = vehicleView.findViewById<Button>(R.id.btnCaptureVehicleImage)

        txtVehicleHeading.text = "Vehicle $vehicleCount"

        if (vehicleCount == 1) {
            btnDelete.visibility = View.GONE
        } else {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { deleteVehicle(vehicleView) }
        }

        btnCapture.setOnClickListener {
            currentVehicleView = vehicleView
            requestCameraPermission()
        }

        llDynamicVehicleContainer.addView(vehicleView)
    }

    private fun deleteVehicle(vehicleView: View) {
        if (vehicleView == llDynamicVehicleContainer.getChildAt(0)) {
            Toast.makeText(this, "The first vehicle is mandatory.", Toast.LENGTH_SHORT).show()
            return
        }

        vehicleImages.remove(vehicleView)
        llDynamicVehicleContainer.removeView(vehicleView)
        renumberVehicles()
    }

    private fun renumberVehicles() {
        for (i in 0 until llDynamicVehicleContainer.childCount) {
            val vehicleView = llDynamicVehicleContainer.getChildAt(i)
            val heading = vehicleView.findViewById<TextView>(R.id.txtVehicleNumberHeading)
            heading.text = "Vehicle ${i + 1}"
        }
        vehicleCount = llDynamicVehicleContainer.childCount
    }

    // Camera handling
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        }
    }

    private fun openCamera() {
        val imageDirectory = File(cacheDir, "images")
        if (!imageDirectory.exists()) imageDirectory.mkdirs()

        cameraImageFile = File(imageDirectory, "vehicle_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", cameraImageFile!!)
        cameraLauncher.launch(cameraImageUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    fetchCurrentLocation()
                } else {
                    Toast.makeText(this, "Location permission is required to save vehicle details.", Toast.LENGTH_LONG).show()
                }
            }
            CAMERA_PERMISSION_REQUEST -> {
                val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required to capture vehicle image.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Image processing and compression
    private fun processCapturedImage(vehicleView: View, imageUri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Toast.makeText(this, "Unable to read captured image.", Toast.LENGTH_SHORT).show()
                return
            }

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Toast.makeText(this, "Invalid captured image.", Toast.LENGTH_SHORT).show()
                return
            }

            val compressedBytes = compressImageToRequiredSize(bitmap)
            if (compressedBytes == null) {
                Toast.makeText(this, "Unable to compress image between 10 KB and 512 KB.", Toast.LENGTH_LONG).show()
                bitmap.recycle()
                return
            }

            vehicleImages[vehicleView] = compressedBytes

            val imgVehicle = vehicleView.findViewById<ImageView>(R.id.imgVehicle)
            val previewBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

            if (previewBitmap != null) {
                imgVehicle.setImageBitmap(previewBitmap)
                imgVehicle.visibility = View.VISIBLE

                // Update capture button text after successful image processing
                val btnCapture = vehicleView.findViewById<Button>(R.id.btnCaptureVehicleImage)
                btnCapture.text = "Retake Image"
            }

            val sizeKb = compressedBytes.size / 1024
            Toast.makeText(this, "Image captured (${sizeKb} KB)", Toast.LENGTH_SHORT).show()
            bitmap.recycle()
        } catch (e: Exception) {
            Toast.makeText(this, "Image processing failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Compress image to 10 KB - 512 KB
    private fun compressImageToRequiredSize(originalBitmap: Bitmap): ByteArray? {
        var bitmap = resizeBitmapIfNeeded(originalBitmap)

        // First attempt: reduce JPEG quality until <= 512 KB
        var quality = 90

        while (quality >= 20) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val result = outputStream.toByteArray()
            outputStream.close()

            val size = result.size

            // Check required range: 10 KB to 512 KB
            if (size >= MIN_IMAGE_SIZE && size <= MAX_IMAGE_SIZE) return result

            if (size > MAX_IMAGE_SIZE) {
                quality -= 5
            } else {
                // Increase quality if below 10 KB
                quality += 5
                if (quality > 100) break
            }
        }

        // Progressively scale down dimensions if still > 512 KB
        repeat(5) {
            val newWidth = (bitmap.width * 0.75f).toInt().coerceAtLeast(1)
            val newHeight = (bitmap.height * 0.75f).toInt().coerceAtLeast(1)

            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            quality = 90

            while (quality >= 20) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val result = outputStream.toByteArray()
                outputStream.close()

                val size = result.size
                if (size >= MIN_IMAGE_SIZE && size <= MAX_IMAGE_SIZE) return result

                quality -= 5
            }
        }

        return null
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = 1600
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale = if (width > height) maxDimension.toFloat() / width else maxDimension.toFloat() / height
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun convertImageToBase64(imageBytes: ByteArray): String =
        android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

    // Form validation and submission
    private fun submitPaddyLiftRecords() {
        if (mspCentreId.isBlank()) {
            Toast.makeText(this, "MSP Centre ID is unavailable.", Toast.LENGTH_LONG).show()
            return
        }

        val roNumber = edtRoNumber.text.toString().trim()
        if (roNumber.isBlank()) {
            edtRoNumber.error = "Please enter RO number"
            edtRoNumber.requestFocus()
            return
        }

        if (currentLatitude == null || currentLongitude == null) {
            Toast.makeText(this, "Current location is not available. Please enable location and try again.", Toast.LENGTH_LONG).show()
            fetchCurrentLocation()
            return
        }

        if (llDynamicVehicleContainer.childCount == 0) {
            Toast.makeText(this, "At least one vehicle is required.", Toast.LENGTH_SHORT).show()
            return
        }

        val vehicleEntries = mutableListOf<VehicleEntry>()

        for (i in 0 until llDynamicVehicleContainer.childCount) {
            val vehicleView = llDynamicVehicleContainer.getChildAt(i)
            val edtVehicleNumber = vehicleView.findViewById<EditText>(R.id.edtVehicleNumber)
            val edtVehicleQuantity = vehicleView.findViewById<EditText>(R.id.edtVehicleQuantity)

            val vehicleNumber = edtVehicleNumber.text.toString().trim()
            val quantity = edtVehicleQuantity.text.toString().trim()

            if (vehicleNumber.isBlank()) {
                edtVehicleNumber.error = "Please enter vehicle number"
                edtVehicleNumber.requestFocus()
                return
            }

            if (quantity.isBlank()) {
                edtVehicleQuantity.error = "Please enter quantity"
                edtVehicleQuantity.requestFocus()
                return
            }

            val imageBytes = vehicleImages[vehicleView]
            if (imageBytes == null) {
                Toast.makeText(this, "Please capture image for Vehicle ${i + 1}.", Toast.LENGTH_LONG).show()
                return
            }

            if (imageBytes.size < MIN_IMAGE_SIZE || imageBytes.size > MAX_IMAGE_SIZE) {
                Toast.makeText(this, "Vehicle ${i + 1} image must be between 10 KB and 512 KB.", Toast.LENGTH_LONG).show()
                return
            }

            val base64Image = convertImageToBase64(imageBytes)
            vehicleEntries.add(
                VehicleEntry(
                    vehicle_number = vehicleNumber,
                    quantity = quantity,
                    image_base64 = base64Image,
                    latitude = String.format(Locale.US, "%.7f", currentLatitude!!),
                    longitude = String.format(Locale.US, "%.7f", currentLongitude!!)
                )
            )
        }

        val request = SavePaddyLiftRecordsRequest(
            mspCentreId = mspCentreId,
            roNumber = roNumber,
            vehicle_entries = vehicleEntries
        )

        sendPaddyLiftRecords(request)
    }

    // Network API dispatch
    private fun sendPaddyLiftRecords(request: SavePaddyLiftRecordsRequest) {
        setSubmitLoading(true)

        RetrofitClient.apiService.savePaddyLiftRecords(request)
            .enqueue(object : Callback<SavePaddyLiftRecordsResponse> {
                override fun onResponse(call: Call<SavePaddyLiftRecordsResponse>, response: Response<SavePaddyLiftRecordsResponse>) {
                    setSubmitLoading(false)

                    if (!response.isSuccessful) {
                        Toast.makeText(this@MonitorDetailsActivity, "Failed to save Paddy Lift records. Please try again.", Toast.LENGTH_LONG).show()
                        return
                    }

                    val body = response.body()

                    if (body?.status_code == 200) {
                        Toast.makeText(this@MonitorDetailsActivity, body.message ?: "Paddy Lift records saved successfully.", Toast.LENGTH_LONG).show()
                        // Return immediately to previous screen
                        finish()
                    } else {
                        Toast.makeText(this@MonitorDetailsActivity, body?.message ?: "Unable to save Paddy Lift records.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<SavePaddyLiftRecordsResponse>, t: Throwable) {
                    setSubmitLoading(false)
                    Toast.makeText(this@MonitorDetailsActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun setSubmitLoading(loading: Boolean) {
        btnSubmit.isEnabled = !loading
        btnAddVehicle.isEnabled = !loading
        edtRoNumber.isEnabled = !loading
        progressSubmit.visibility = if (loading) View.VISIBLE else View.GONE
        btnSubmit.text = if (loading) "Submitting..." else "Submit"
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { finish() }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up temporary camera cache file
        try {
            cameraImageFile?.delete()
        } catch (_: Exception) {
        }
    }
}