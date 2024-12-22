package com.example.airlinesv2

import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var scanningPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)
        rootLayout.setBackgroundColor(Color.WHITE)

        barcodeScanner = BarcodeScanning.getClient()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 1)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.previewView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(findViewById<PreviewView>(R.id.previewView).surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, { imageProxy ->
                if (!scanningPaused) {
                    processImageProxy(imageProxy)
                } else {
                    imageProxy.close()
                }
            })

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            try {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            if (barcode.format != Barcode.FORMAT_QR_CODE) {
                                ToneGenerator(
                                    AudioManager.STREAM_MUSIC,
                                    ToneGenerator.MAX_VOLUME
                                ).startTone(
                                    ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
                                    150
                                )

                                val barcodeValue = barcode.rawValue ?: barcode.displayValue
                                val extractedData = barcodeValue?.let { extractBarcodeData(it) }
                                if (extractedData != null) {
                                    populateBoardingPass(extractedData)
                                } else{
                                    scanningPaused = false
                                    return@addOnSuccessListener
                                }

                                scanningPaused = true
                                extractedData?.let {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (preValidateBarcode(extractedData)) {
                                            val apiResult = getRequestAsync(it)
                                            if (apiResult != null) {
                                                processApiResults(apiResult,extractedData.flightDate)
                                            }
                                            delay(2000)
                                            scanningPaused = false
                                        } else {
                                            delay(2000)
                                            scanningPaused = false
                                        }
                                    }
                                } ?: run {
                                    println("Barcode data extraction failed or barcodeValue is null")
                                }

                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MLKit", "Barcode scanning failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                validationUIResponse(false)
                scanningPaused = false
                imageProxy.close()
            }
        } else {
            Log.e("ImageProxyError", "MediaImage is null")
            imageProxy.close()
        }
    }

    fun preValidateBarcode(barcode: BarcodeData?): Boolean {
        scanningPaused = true
        // Ensure barcode is not null
        if (barcode != null) {
            // Get the current date
            val currentDate = LocalDate.now()

            // Compare only the date part of the flightDate
            if (barcode.flightDate.isBefore(currentDate)) {
                validationUIResponse(false)
                return false
            }
        }
        return true
    }

    fun processApiResults(jsonResponse: String, ticketDate: LocalDate) {
        val jsonObject = Json.parseToJsonElement(jsonResponse).jsonObject
        val flightStatuses = jsonObject["flightStatuses"]?.jsonArray

        if (flightStatuses != null && flightStatuses.isNotEmpty()) {
            val departureDate = getDepartureDateLocal(jsonResponse) ?: ""
            val airport = getDepartureAirportFsCode(jsonResponse) ?: ""

            val errorMsg = airport?.let {
                if (departureDate != null) {
                    validateFlight(it, departureDate, ticketDate)
                } else {
                    "Error during scan"
                }
            }

            if (!errorMsg.isNullOrEmpty()) {
                validationUIResponse(false)
            } else {
                validationUIResponse(true)
            }

        } else {
            validationUIResponse(false)
        }
    }

    fun getDepartureDateLocal(jsonString: String): String? {
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val flightStatuses = jsonObject["flightStatuses"]?.jsonArray
        if (flightStatuses != null && flightStatuses.isNotEmpty()) {
            val matchingFlight = if (flightStatuses.size > 1) {
                flightStatuses.find {
                    it.jsonObject["departureAirportFsCode"]?.jsonPrimitive?.content == "SIN"
                }
            } else {
                null
            }

            if (matchingFlight != null) {
                val flightStatus = matchingFlight.jsonObject
                val departureDate = flightStatus["departureDate"]?.jsonObject
                return departureDate?.get("dateLocal")?.toString()?.trim('"')
            } else {
                val flightStatus = flightStatuses[0].jsonObject
                val departureDate = flightStatus["departureDate"]?.jsonObject
                return departureDate?.get("dateLocal")?.toString()?.trim('"')
            }

        }

        return null
    }

    fun getDepartureAirportFsCode(jsonString: String): String? {
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val flightStatuses = jsonObject["flightStatuses"]?.jsonArray
        if (flightStatuses != null && flightStatuses.isNotEmpty()) {
            val matchingFlight = if (flightStatuses.size > 1) {
                flightStatuses.find {
                    it.jsonObject["departureAirportFsCode"]?.jsonPrimitive?.content == "SIN"
                }
            } else {
                null
            }

            if (matchingFlight != null) {
                val flightStatus = matchingFlight.jsonObject
                return flightStatus["departureAirportFsCode"]?.toString()?.trim('"')
            } else {
                val flightStatus = flightStatuses[0].jsonObject
                return flightStatus["departureAirportFsCode"]?.toString()?.trim('"')
            }
        }

        return null
    }

    fun validateFlight(departureAirportFsCode: String, scheduledDepartureDate: String, ticketDate: LocalDate ): String {
        return try {
            // Check if departure is from SIN
            if (departureAirportFsCode != "SIN") {
                return "Alert: Departure is not from SIN!"
            }

            // Define the formatter to parse the input date
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            // Parse the input date string into a LocalDateTime object
            val apiDepDate = LocalDateTime.parse(scheduledDepartureDate, formatter)
            // Get the current date and time
            val currentDate = LocalDateTime.now()

            // Check if the input date is in the past
            if (apiDepDate.isBefore(currentDate) || apiDepDate.toLocalDate().isAfter(ticketDate)) {
                return "Alert: The flight has already departed!"
            }

            // Calculate the difference in hours
            val hoursDifference = ChronoUnit.HOURS.between(currentDate, apiDepDate)

            // Check if the difference is within 24 hours
            return if (hoursDifference in 0..24) {
                ""
            } else {
                "The flight is NOT within 24 hours."
            }
        } catch (ex: Exception) {
            "Internal Server Error Occurred"
        }
    }

    fun populateBoardingPass(data: BarcodeData) {
            val flightIata = findViewById<TextView>(R.id.flightIata)
            flightIata.text = "Flight IATA: ${data.flightIata}"

            val formattedFlightDate =
                data.flightDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

            val departureDate = findViewById<TextView>(R.id.departureDate)
            departureDate.text = "Departure Date: $formattedFlightDate"
    }

    fun clearTicketDetails() {
        val departureTime = findViewById<TextView>(R.id.flightIata)
        departureTime.text = "Flight IATA: "

        val flightAirport = findViewById<TextView>(R.id.departureDate)
        flightAirport.text = "Boarding Date: "
    }

    fun extractBarcodeData(encodedBarcode: String): BarcodeData? {
        try {
            // Extract data from the barcode
            val passengerName = encodedBarcode.substring(2, 22).trim()
            val airlineCode = encodedBarcode.substring(36, 39).trim()
            val flightNumber = encodedBarcode.substring(39, 44).trim()
            val julianDate = encodedBarcode.substring(44, 47).toInt()
            val seatNumber = encodedBarcode.substring(48, 51).trim()

            // Get the current year
            val currentYear = LocalDate.now().year

            // Calculate the flight date from the Julian date
            val flightDate = LocalDate.ofYearDay(currentYear, julianDate)

            // Construct IATA flight code
            val flightIata = "$airlineCode${flightNumber.trimStart('0')}"

            // Return a BarcodeData instance
            return BarcodeData(
                passengerName = passengerName,
                airlineCode = airlineCode,
                flightNumber = flightNumber,
                flightDate = flightDate,
                seatNumber = seatNumber,
                flightIata = flightIata
            )
        } catch (e: Exception) {
            validationUIResponse(false)
            clearTicketDetails()
            return  null
        }
    }

    fun validationUIResponse(valid: Boolean) {
        val rootLayout =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
                R.id.rootLayout
            )

        if (valid) {
            val validationMsg = findViewById<TextView>(R.id.validationMsg)
            validationMsg.text = "GO!"
            rootLayout.setBackgroundColor(Color.GREEN)

        } else {
            val validationMsg = findViewById<TextView>(R.id.validationMsg)
            validationMsg.text = "STOP!"
            rootLayout.setBackgroundColor(Color.RED)
        }
    }

    suspend fun getRequestAsync(request: BarcodeData): String? {
        val appId = "6acd1100"
        val appKey = "a287bbec7d155e99d39eae55fe341828"

        val currentDate = LocalDate.now()
        var ticketDate = request.flightDate
        if (ticketDate.isAfter(currentDate)) {
            ticketDate = ticketDate.minusDays(1)
        }
        val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val strDate = ticketDate.format(dateFormat)
        val url =
            "https://api.flightstats.com/flex/flightstatus/rest/v2/json/flight/status/${request.airlineCode}/${request.flightNumber}/dep/${strDate}?appId=${appId}&appKey=${appKey}&utc=true"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                    // Return the raw JSON string instead of deserializing
                    jsonString
                } else {
                    println("\tERROR: Response Code ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                println("\tERROR: ${e.message}")
                null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Log.e("Permission", "Camera permission denied")
            }
        }
    }
}

data class BarcodeData(
    val passengerName: String,
    val airlineCode: String,
    val flightNumber: String,
    val flightDate: LocalDate,
    val seatNumber: String,
    val flightIata: String
)
