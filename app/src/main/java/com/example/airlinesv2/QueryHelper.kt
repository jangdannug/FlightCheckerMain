package com.example.airlinesv2

import android.util.Log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getFlightId(flightStatuses: JsonArray?): List<String> {
    return flightStatuses?.mapNotNull { flight ->
        flight.jsonObject["flightId"]?.jsonPrimitive?.content
    } ?: emptyList()
}

fun getFsCode(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content
        val carrierFsCode = flight.jsonObject["carrierFsCode"]?.jsonPrimitive?.content
        flightId to carrierFsCode
    } ?: emptyMap()
}

fun getFlightNumber(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content
        val flightNumber = flight.jsonObject["flightNumber"]?.jsonPrimitive?.content
        flightId to flightNumber
    } ?: emptyMap()
}

fun getAirPortFsCode(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content
        val departureAirportFsCode = flight.jsonObject["departureAirportFsCode"]?.jsonPrimitive?.content
        flightId to departureAirportFsCode
    } ?: emptyMap()
}

fun getScheduleDt(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content
        val scheduledGateDepartureDateLocal = flight
            .jsonObject["operationalTimes"]
            ?.jsonObject?.get("scheduledGateDeparture")
            ?.jsonObject?.get("dateLocal")
            ?.jsonPrimitive?.content
        flightId to scheduledGateDepartureDateLocal
    } ?: emptyMap()
}


fun getEstimatedDt(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content
        val estimatedGateDepartureDateLocal = flight
            .jsonObject["operationalTimes"]
            ?.jsonObject?.get("estimatedGateDeparture")
            ?.jsonObject?.get("dateLocal")
            ?.jsonPrimitive?.content
        flightId to estimatedGateDepartureDateLocal
    } ?: emptyMap()
}

fun getLatestDepartureDt(flightStatuses: JsonArray?): Map<String?, String?> {
    return flightStatuses?.associate { flight ->
        // Extract flightId
        val flightId = flight.jsonObject["flightId"]?.jsonPrimitive?.content

        // Extract scheduled gate departure date (dateLocal)
        val scheduledGateDepartureDateLocal = flight
            .jsonObject["operationalTimes"]
            ?.jsonObject?.get("scheduledGateDeparture")
            ?.jsonObject?.get("dateLocal")
            ?.jsonPrimitive?.content

        // Extract estimated gate departure date (dateLocal)
        val estimatedGateDepartureDateLocal = flight
            .jsonObject["operationalTimes"]
            ?.jsonObject?.get("estimatedGateDeparture")
            ?.jsonObject?.get("dateLocal")
            ?.jsonPrimitive?.content

        // Compare the dates and take the latest one
        val latestDepartureDt = listOfNotNull(
            scheduledGateDepartureDateLocal,
            estimatedGateDepartureDateLocal
        ).maxOrNull() // Uses string comparison assuming ISO-8601 format

        // Map flightId to the latest departure date
        flightId to latestDepartureDt
    } ?: emptyMap()
}



fun parseDate(dateStr: String): LocalDateTime? {
    return try {
        LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME)
    } catch (e: Exception) {
        Log.e("DATE_PARSE_ERROR", "Error parsing date: $dateStr", e)
        null
    }
}

