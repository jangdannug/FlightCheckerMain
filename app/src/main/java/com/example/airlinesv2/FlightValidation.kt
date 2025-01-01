package com.example.airlinesv2

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit




fun validateFlight(departureAirportFsCode: String, scheduledDepartureDate: String, ticketDate: LocalDate): String {
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
        if (apiDepDate.isBefore(currentDate) || apiDepDate.toLocalDate().isBefore(ticketDate)) {
            return "Alert: The flight has already departed!"
        }

        val minutesDifference = ChronoUnit.MINUTES.between(currentDate, apiDepDate)

        return if (minutesDifference in 0..1440) {
            ""
        } else {
            "The flight is NOT within 24 hours."
        }
    } catch (ex: Exception) {
        "Internal Server Error Occurred"
    }
}

   fun test(context: Context, barcode: BarcodeData)
   {
val db = DataBaseHandler(context)
       val test1 = barcode.flightDate
       val test2 = barcode.flightIata

       db.getDataByFlightCode(test2)

   }




