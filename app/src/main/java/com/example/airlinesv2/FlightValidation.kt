package com.example.airlinesv2

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun validateFlight(context: Context, barcode: BarcodeData): Boolean {
    return try {
        val db = DataBaseHandler(context)

        val dbResult = db.getDataByFlightCode(barcode)

        val currentDate = LocalDateTime.now()

        val formatterDb = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val departureDt = LocalDateTime.parse(dbResult?.departureDates, formatterDb)

        val ticketDate = barcode.flightDate

        val isBoardingValid = !(departureDt.isBefore(currentDate) || departureDt.toLocalDate().isBefore(ticketDate))

        val minutesDifference = ChronoUnit.MINUTES.between(currentDate, departureDt)

        return isBoardingValid && minutesDifference in 0..1440

    } catch (ex: Exception) {
        false
    }
}





