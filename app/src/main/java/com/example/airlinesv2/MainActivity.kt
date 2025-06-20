package com.example.airlinesv2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    public var scanningPaused = false

    private lateinit var alertMessage: TextView
    private lateinit var detailsFrmDb: TextView
    private lateinit var test3: TextView


    private lateinit var advanceTimeClock: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {

        try {

            super.onCreate(savedInstanceState)
            enableEdgeToEdge()
            setContentView(R.layout.activity_main)

            clearDetails()

            alertMessage = findViewById(R.id.alertMessage)
            detailsFrmDb = findViewById(R.id.detailsFrmDb)
            test3 = findViewById(R.id.test3)

            advanceTimeClock = findViewById(R.id.advanceTimeClock)
            startClocks()

            val context = this
            lifecycleScope.launch {
                scanningPaused = true
                queryApi(context)
            }


            barcodeScanner = BarcodeScanning.getClient()
            cameraExecutor = Executors.newSingleThreadExecutor()

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    1
                )
            }

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.previewView)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            scanningPaused = false
        } catch (e: Exception) {
            null
        }
    }

    fun startCamera() {

        try {
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
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            try {
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            if (barcode.format != Barcode.FORMAT_QR_CODE) {
                                val barcodeValue = barcode.rawValue ?: barcode.displayValue
                                val extractedData = barcodeValue?.let { extractBarcodeData(it) }
                                if (extractedData != null) {
                                   // ToneGenerator(
                                     //  AudioManager.STREAM_MUSIC,
                                     //   ToneGenerator.MAX_VOLUME
                                   // ).startTone(
                                   //     ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
                                  //      150
                                  //  )

                                    populateBoardingPass(extractedData)
                                } else {
                                    scanningPaused = false
                                    return@addOnSuccessListener
                                }

                                scanningPaused = true
                                extractedData?.let {
                                    CoroutineScope(Dispatchers.Main).launch {

                                        if (preValidateBarcode(extractedData))
                                        {
                                            val context: Context = applicationContext
                                            val queryRes = queryApi(context)
                                            if (queryRes) {
                                                val isValidFlight =
                                                    validateFlight(context, extractedData)
                                                validationUIResponse(isValidFlight)
                                            }

                                            val db = DataBaseHandler(context)
                                            var code = extractedData.flightIata
                                           val dataFrmDb = db.getDataByFlightCode(extractedData)
                                            detailsFrmDb.text = "flightID: ${dataFrmDb?.flightIds}\n " +
                                                   "CarrierFsCode: ${dataFrmDb?.carrierIata}\n" +
                                                   "FlightNumber: ${dataFrmDb?.flightNumber}\n" +
                                            "flightDate: ${dataFrmDb?.departureDates}"
                                            delay(3000)
                                            scanningPaused = false
                                            clearDetails()
                                        } else {
                                            delay(3000)
                                            scanningPaused = false
                                            clearDetails()
                                        }
                                    }
                                } ?: run {
                                    //clearDetails()
                                }

                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        //clearDetails()
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                validationUIResponse(false)
                scanningPaused = false
                clearDetails()
                imageProxy.close()
            }
        } else {
            Log.e("ImageProxyError", "MediaImage is null")
            imageProxy.close()
            //clearDetails()
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
             alertMessage.text = "EXPIRED TICKET"
             detailsFrmDb.text = ""
              validationUIResponse(false)
             return false
           }

            if (barcode.seatNumber.isNullOrEmpty()) {
                alertMessage.text = "NO SEAT NUMBER"
                detailsFrmDb.text = ""
                validationUIResponse(false)
                return false
           }

            if (barcode.seatNumber == "000") {
                alertMessage.text = "NO SEAT NUMBER"
                detailsFrmDb.text = ""
                validationUIResponse(false)
                return false
            }
        }
        return true
    }

    private fun playErrorSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.error) // R.raw.error refers to error.mp3
        mediaPlayer.setVolume(3.0f, 3.0f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release() // Release the MediaPlayer once done
        }
    }

    private fun playSuccessSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.success) // R.raw.success refers to success.mp3
        mediaPlayer.setVolume(1.0f, 1.0f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release() // Release the MediaPlayer once done
        }
    }


    fun getDepartureAirportFsCode(jsonString: String, ticketDate: LocalDate): String? {
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val flightStatuses = jsonObject["flightStatuses"]?.jsonArray
        if (flightStatuses != null && flightStatuses.isNotEmpty()) {
            val matchingFlight = flightStatuses.firstOrNull {
                val scheduledGateDeparture = it.jsonObject["operationalTimes"]?.jsonObject
                    ?.get("scheduledGateDeparture")?.jsonObject
                val dateLocal = scheduledGateDeparture?.get("dateLocal")?.jsonPrimitive?.content
                dateLocal?.startsWith(ticketDate.toString()) == true
            }

            if (matchingFlight != null) {
                val flightStatus = matchingFlight.jsonObject
                return flightStatus["departureAirportFsCode"]?.toString()?.trim('"')
            } else {
                return null
            }
        }

        return null
    }


    fun clearDetails() {
        val departureTime = findViewById<TextView>(R.id.flightIata)
        departureTime.text = "Flight IATA: "

        val flightAirport = findViewById<TextView>(R.id.departureDate)
        flightAirport.text = "Boarding Date: "

        val rootLayout =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)
        rootLayout.setBackgroundColor(Color.WHITE)
        val validationMsg = findViewById<TextView>(R.id.validationMsg)
        validationMsg.text = ""
        val noSeatNumber = findViewById<TextView>(R.id.alertMessage)
        noSeatNumber.text = ""
    }

    private fun extractBarcodeData(encodedBarcode: String): BarcodeData? {
        try {
            // Extract data from the barcode
            val passengerName = encodedBarcode.substring(2, 22).trim()
            val airlineCode = encodedBarcode.substring(36, 39).trim()
            val flightNumber = encodedBarcode.substring(39, 44).trim()
            val julianDate = encodedBarcode.substring(44, 47).toInt()
            val seatNumber = encodedBarcode.substring(48, 51).trim()

            // Get the current year
            val currentYear = LocalDate.now().year
            val nowJulianDate = LocalDate.now().dayOfYear
            val adjustedJulianDate = nowJulianDate + 2

            val previousYear = if (julianDate > adjustedJulianDate) {
                currentYear -1
            } else {
                currentYear
            }
                // Calculate the flight date from the Julian date
            val flightDate = LocalDate.ofYearDay(previousYear, julianDate)

            // Construct IATA flight code
            val flightIata = "$airlineCode${flightNumber.trimStart('0')}"

            // Return a BarcodeData instance
            return BarcodeData(
                passengerName = passengerName,
                Iata = airlineCode,
                flightNumber = flightNumber,
                flightDate = flightDate,
                seatNumber = seatNumber,
                flightIata = flightIata
            )
        } catch (e: Exception) {
            validationUIResponse(false)
            clearDetails()
            return null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun populateBoardingPass(data: BarcodeData) {
        val flightIata = findViewById<TextView>(R.id.flightIata)
        flightIata.text = "Flight IATA: ${data.flightIata}"

        val formattedFlightDate =
            data.flightDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

        val departureDate = findViewById<TextView>(R.id.departureDate)
        departureDate.text = "Departure Date: $formattedFlightDate"
    }

    private fun validationUIResponse(valid: Boolean) {
        val rootLayout =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
                R.id.rootLayout
            )


        if (valid) {
            val validationMsg = findViewById<TextView>(R.id.validationMsg)
            ToneGenerator(
                AudioManager.STREAM_MUSIC,
                ToneGenerator.MAX_VOLUME
            ).startTone(
                ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
                150
            )
            validationMsg.text = "GO!"
            rootLayout.setBackgroundColor(Color.GREEN)
        } else {
            val validationMsg = findViewById<TextView>(R.id.validationMsg)
            playErrorSound()
            validationMsg.text = "STOP!"
            rootLayout.setBackgroundColor(Color.RED)
        }

    }


    private fun startClocks() {
        handler.post(object : Runnable {
            override fun run() {
                val now = Calendar.getInstance()

                now.add(Calendar.HOUR_OF_DAY, 24)
                val advanceTime = dateTimeFormat.format(now.time)
                advanceTimeClock.text = "24 Hours: $advanceTime"

                handler.postDelayed(this, 1000) // Update every second
            }
        })
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
    val Iata: String,
    val flightNumber: String,
    val flightDate: LocalDate,
    val seatNumber: String,
    val flightIata: String
)
