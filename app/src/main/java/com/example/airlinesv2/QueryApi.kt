package com.example.airlinesv2

import getCodeshares
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import getAppendix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

suspend fun queryApi(context: Context): Boolean {
    val db = DataBaseHandler(context)

    return try {
        var isUpdated = false
        val checkUpdate = db.getLatestUpdate()
        val checkExecType = checkUpdate.execType

        if (checkUpdate.executeDt.isNotEmpty()) {
            val dbUpdateDt = LocalDateTime.parse(checkUpdate.executeDt)
            isUpdated = isDbUpdated(dbUpdateDt,checkExecType)
        }

        if (!isUpdated) {

            showCustomToast(context, "UPDATING PLEASE WAIT")

                val jsonResponses = getApiAsync()

                db.deleteDatabase(context)
                db.deleteFlightCodesData()
                db.deleteFlightData()
                db.deleteDataLogsData()

                var batch = 0
                var execType = 0


                jsonResponses.forEach { jsonResponse ->

                    val jsonObject = jsonResponse?.let { Json.parseToJsonElement(it).jsonObject }
                    val flightStatuses = jsonObject?.get("flightStatuses")?.jsonArray
                    val dbFsCodesList = getAppendix(jsonObject)

                    db.insertCodes(dbFsCodesList)
                    var test = dbFsCodesList


                    if (!flightStatuses.isNullOrEmpty()) {
                        val flightId = getFlightId(flightStatuses)
                        val carrierIata = getIata(flightStatuses)
                        val flightNumber = getFlightNumber(flightStatuses)
                        val departureDt = getLatestDepartureDt(flightStatuses)
                        val codeShareMap = getCodeshares(flightStatuses)


                        // Mapping
                        val flightIdMap = flightId.mapNotNull { it.toIntOrNull() }

                        val carrierFsCodeMap = flightId.map { flightId ->
                            val carrierFsCode = (carrierIata[flightId] ?: "")

                            var code = carrierFsCode
                            var fixFscode = ""
                            if (code.contains("*") == true) {
                                // Handle the case where there was an asterisk
                                println("Warning: Flight ID $flightId has an asterisk in the carrier code.")
                                fixFscode = code.replace("*", "")

                            } else {
                                fixFscode = code
                            }

                            "$fixFscode"

                        }

                        val flightNumberMap = flightId.map { flightId ->
                            flightNumber[flightId] ?: ""
                        }

                        val departureDtMap = flightId.map { flightId ->
                            departureDt[flightId] ?: ""
                        }

                        var currentLocalDate = LocalDate.now()
                        var queryDateListMap = List(flightIdMap.size) { currentLocalDate }

                        val batchTypeList = MutableList(flightIdMap.size) { "" }
                        batch++
                        var batchCount = batch

                        for (i in flightIdMap.indices) {
                            batchTypeList[i] = when (batchCount) {
                                1 -> "CurrentDateHour0"
                                2 -> "CurrentDateHour6"
                                3 -> "CurrentDateHour12"
                                4 -> "CurrentDateHour18"
                                5 -> "NextDateHour0"
                                6 -> "NextDateHour6"
                                7 -> "NextDateHour12"
                                8 -> "NextDateHour18"
                                else -> ""
                            }
                        }

                        if (batch > 4) {
                            currentLocalDate = LocalDate.now().plusDays(1)
                            queryDateListMap = List(flightIdMap.size) { currentLocalDate }
                        }

                        val dbFlights = Flights(
                            flightIds = flightIdMap,
                            carrierFsCode = carrierFsCodeMap,
                            flightNumber = flightNumberMap,
                            departureDates = departureDtMap,
                            queryDate = queryDateListMap,
                            codeShare = codeShareMap,
                            batchType = batchTypeList
                        )
                        execType++
                        val currentDateLogs = listOf(LocalDateTime.now().toString())
                        val flightIdsSizeLogs = listOf(dbFlights.flightIds.size.toString())
                        val execTypeLogs = listOf(execType.toString())

                        val dbDataLogs = DbDataLogs(
                            executeDt = currentDateLogs,
                            dataSize = flightIdsSizeLogs,
                            execType = execTypeLogs
                        )

                        //   val  dbFsCodes = DbFsCodes(
                        // )

                        // Uncomment this line if you want to delete the database
                        // db.deleteDatabase(context)

                        db.insertFlights(dbFlights)
                        //db.insertCodes()
                        db.insertDataLogs(dbDataLogs)


                    } else {
                        println("No flight statuses found in the response.") // Log the absence of flight statuses
                    }
                }

        }
        true // Return true if everything went well
    } catch (e: Exception) {
        Log.e("ERROR_dbProcessSave", "Error Exception: ${e.message}", e)
        false // Return false if an error occurred
    }
}

suspend fun getApiAsync(): List<String?> {
    val baseUrl =
        "https://api.flightstats.com/flex/flightstatus/rest/v2/json/airport/status/SIN/dep/"

    // Get current date and the next date
    val currentDate = LocalDate.now()
    val nextDate = currentDate.plusDays(1)

    // Date format for the API request
    val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd/")
    val numberHours = 6
    val appId = "6acd1100"
    val appKey = "a287bbec7d155e99d39eae55fe341828"

    // List of request hours
    val reqHoursList = listOf(0, 6, 12, 18)
    val responses = mutableListOf<String?>()

    // Function to make requests for a given date
    suspend fun makeRequestsForDate(date: LocalDate) {
        val currDate = date.format(dateFormat)

        //val currDate = "2025/30/01/"

        // Make requests for each hour
        for (reqHours in reqHoursList) {
            val url =
                "${baseUrl}${currDate}${reqHours}?appId=${appId}&appKey=${appKey}&utc=false&numHours=${numberHours}&extendedOptions=useInlinedReferences"

            val response = withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonString =
                            connection.inputStream.bufferedReader().use { it.readText() }
                        jsonString // Return the raw JSON string
                    } else {
                        println("\tERROR: Response Code ${connection.responseCode}")
                        null
                    }
                } catch (e: Exception) {
                    println("\tERROR: ${e.message}")
                    null
                }
            }

            responses.add(response) // Add the response to the list
        }
    }

    // Make requests for the current date and the next date
    makeRequestsForDate(currentDate)
    makeRequestsForDate(nextDate)

    return responses // Return the list of responses for both dates
}

fun isDbUpdated(dbUpdateDt: LocalDateTime, execType: String): Boolean {
    // Get the current time
    val currentTime = LocalDateTime.now()

    val sixtyMinutesAgo = currentTime.minusMinutes(60)
    val isWithinLast60Minutes = !dbUpdateDt.isBefore(sixtyMinutesAgo)
    val isExecTypeEight = execType == "8"

    if (isWithinLast60Minutes && isExecTypeEight)
        {
            return true
        } else
        {
        return false
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
        val blinkInterval = 400L // Blink every 500 milliseconds
        val totalDuration = 15000L // Total duration of 15 seconds

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