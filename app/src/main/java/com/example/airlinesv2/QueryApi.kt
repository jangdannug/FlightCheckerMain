package com.example.airlinesv2

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

    suspend fun queryApi(context : Context) {
        val context = context
        val db = DataBaseHandler(context)

        try {

            val  checkUpdate = db.getLatestUpdate()
            val localDateTime = LocalDateTime.parse(checkUpdate)
            val isUpdated = isCurrentTimeInInterval(localDateTime)

            if (!isUpdated) {

                showCustomToast(context, "UPDATING PLEASE WAIT")
                val jsonResponse = getApiAsync()
                val jsonObject = jsonResponse?.let { Json.parseToJsonElement(jsonResponse).jsonObject }
                val flightStatuses = jsonObject?.get("flightStatuses")?.jsonArray

                if (!flightStatuses.isNullOrEmpty()) {
                    val flightId = getFlightId(flightStatuses)
                    val fsCode = getFsCode(flightStatuses)
                    val flightNumber = getFlightNumber(flightStatuses)
                    val airportFsCode = getAirPortFsCode(flightStatuses)
                    val departureDt = getLatestDepartureDt(flightStatuses)

                    //Mapping
                    val flightIdInt = flightId.mapNotNull { it.toIntOrNull() }

                    val flightCodesMap = flightId.map { flightId ->
                        val carrierFsCode = fsCode[flightId] ?: ""
                        val flightNumber = flightNumber[flightId] ?: ""
                        "$carrierFsCode$flightNumber"
                    }

                    val airportFsCodeMap = flightId.map { flightId ->
                        airportFsCode[flightId] ?: ""
                    }


                    val departureDtMap = flightId.map { flightId ->
                        departureDt[flightId] ?: ""
                    }

                    val dbFlights = Flights(
                        flightIds = flightIdInt,
                        flightCodes = flightCodesMap,
                        departureAirportFsCodes = airportFsCodeMap,
                        departureDates = departureDtMap
                    )

                    val currentDate = LocalDateTime.now().toString()
                    val flightIdsSize = dbFlights.flightIds.size
                    val execType = "update"

                    val DbDataLogs = DbDataLogs(
                        executeDt = currentDate,
                        dataSize = flightIdsSize.toString(),
                        execType = execType.toString()
                    )

                    db.deleteDatabase(context)

                    db.insertFlights(dbFlights)
                    db.insertDataLogs(DbDataLogs)

                }
            }
            return
        } catch (e: Exception) {
                Log.e("ERROR_dbProcessSave", "Error Exception: ${e.message}", e)
            }

    }

    suspend fun getApiAsync(): String? {
        val baseUrl =
            "https://api.flightstats.com/flex/flightstatus/rest/v2/json/airport/status/SIN/dep/"

        val currentDate = LocalDate.now()
        val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd/")
        val reqHours = 8
        val numberHours = 6
        val currDate = currentDate.format(dateFormat)
        val appId = "6acd1100"
        val appKey = "a287bbec7d155e99d39eae55fe341828"

        val url =
            "${baseUrl}${currDate}${reqHours}?appId=${appId}&appKey=${appKey}&utc=false&numHours=${numberHours}"

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

fun isCurrentTimeInInterval(dateTime: LocalDateTime): Boolean {
    val time = dateTime.toLocalTime()
    val date = dateTime.toLocalDate()

    // Check if the date is today
    val today = LocalDate.now()

    // If the date is today, check the time intervals
    return if (date.isEqual(today)) {
        when {
            time.isAfter(LocalTime.of(0, 0)) && time.isBefore(LocalTime.of(6, 0)) -> true // 12:00 AM to 6:00 AM
            time.isAfter(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(12, 0)) -> true // 6:00 AM to 12:00 PM
            time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(18, 0)) -> true // 12:00 PM to 6:00 PM
            time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(24, 0)) -> true // 6:00 PM to 12:00 AM
            else -> false // Invalid time
        }
    } else {
        false // The date is not today
    }
}

    fun showCustomToast(context: Context, message: String) {

        // Inflate the custom layout
        val inflater = LayoutInflater.from(context)
        val layout: View = inflater.inflate(R.layout.custom_toast, null)

        // Set the message text
        val text: TextView = layout.findViewById(R.id.toast_text)
        text.text = message

        // Set the icon (optional)
        val icon: ImageView = layout.findViewById(R.id.toast_icon)
        icon.setImageResource(R.drawable.ic_error) // Replace with your icon resource

        // Create the Toast
        val toast = Toast(context)
        toast.view = layout
        toast.setGravity(Gravity.CENTER, 0, 0) // Center the Toast

        // Handler for blinking effect
        val handler = Handler()
        var isVisible = true
        val blinkInterval = 800L // Blink every 500 milliseconds
        val totalDuration = 20000L // Total duration of 30 seconds

        // Runnable to handle the blinking effect
        val blinkRunnable = object : Runnable {
            override fun run() {
                if (isVisible) {
                    toast.show() // Show the Toast
                } else {
                    toast.cancel() // Hide the Toast
                }
                isVisible = !isVisible // Toggle visibility
                handler.postDelayed(this, blinkInterval) // Repeat after the blink interval
            }
        }

        // Start the blinking effect
        handler.post(blinkRunnable)

        // Stop the blinking effect after 30 seconds
        handler.postDelayed({
            handler.removeCallbacks(blinkRunnable) // Stop the runnable
            toast.cancel() // Ensure the Toast is hidden
        }, totalDuration)
    }