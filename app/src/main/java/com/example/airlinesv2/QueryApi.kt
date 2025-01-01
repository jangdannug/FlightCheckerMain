package com.example.airlinesv2

import android.content.Context
import android.util.Log
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

    suspend fun queryApi(context : Context) {
        val context = context
        val db = DataBaseHandler(context)

        try {
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
                val execType = 1

                val dbDbDataLogs = DbDataLogs(
                    executeDt = "currentDate",
                    dataSize = flightIdsSize.toString(),
                    execType = execType.toString()
                )

                db.deleteDatabase(context)

                db.insertFlights(dbFlights)
                db.insertDataLogs(dbDbDataLogs)


            }

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
        val currDate = currentDate.format(dateFormat)
        val appId = "6acd1100"
        val appKey = "a287bbec7d155e99d39eae55fe341828"

        val url = "${baseUrl}${currDate}${reqHours}?appId=${appId}&appKey=${appKey}&utc=false&numHours=${reqHours}"

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
