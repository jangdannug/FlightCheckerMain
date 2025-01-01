package com.example.airlinesv2

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

const val DATABASE_NAME = "Flights"
const val TABLE_NAME = "FlightStatuses"
const val COL_FlightId = "flightIds"
const val COL_FlightCode = "FlightCode"
const val COL_DepartureAirportFsCode = "DepartureAirportFsCode"
const val COL_DepartureDate = "DepartureDate"

const val  TABLE_dataLogs = "dataLogs"
const val COL_executeDt = "executeDt"
const val COL_dataSize = "dataSize"
const val COL_execType = "execType"

class DataBaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME,null,1) {

    init {
        // Delete the existing database file
        context.deleteDatabase(DATABASE_NAME)
    }


    override fun onCreate(db: SQLiteDatabase?) {

        val createTable = " CREATE TABLE $TABLE_NAME (" +
                "$COL_FlightId INTEGER PRIMARY KEY," +
                "$COL_FlightCode TEXT," +
                "$COL_DepartureAirportFsCode TEXT," +
                "$COL_DepartureDate TEXT)"
        db?.execSQL(createTable)

        // Create DataLogs table
        val createTableDataLogs = "CREATE TABLE $TABLE_dataLogs (" +
                "$COL_executeDt TEXT," +
                "$COL_dataSize TEXT," +
                "$COL_execType TEXT)"

        db?.execSQL(createTableDataLogs)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")  // Drop the existing table if it exists
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_dataLogs")  // Drop another table if it exists
        onCreate(db)  // Recreate the tables as defined in the onCreate method
    }

    fun insertFlights(flights: Flights) {
        val db = this.writableDatabase
        db.beginTransaction()

        try {

            // Check if list sizes are consistent
            if (flights.flightIds.size != flights.flightCodes.size) {
                return
            }

            if (flights.flightIds.size != flights.departureAirportFsCodes.size) {
                return
            }

            if (flights.flightIds.size != flights.departureDates.size) {
                return
            }

            var successCount = 0 // Counter for successful inserts
            var failureCount = 0 // Counter for failed inserts

            // Loop through each flightId and perform the insert
            for (i in flights.flightIds.indices) {
                val cv = ContentValues()

                // Put the data into ContentValues
                val values = ContentValues().apply {
                    put("flightIds", flights.flightIds.joinToString(","))
                    put("FlightCode", flights.flightCodes.joinToString(","))
                    put("DepartureAirportFsCode", flights.departureAirportFsCodes.joinToString(","))
                    put("DepartureDate", flights.departureDates.joinToString(","))
                }

                val whereClause = "flightIds = ?"
                val whereArgs = arrayOf(flights.flightIds.joinToString(","))

                // Insert the data into the database
                val result = db.update("FlightStatuses", values, whereClause, whereArgs)

                if (result.toLong() != -1L) {
                    successCount++ // Increment success counter
                } else {
                    failureCount++ // Increment failure counter
                    Log.e("DB_INSERT", "Failed to insert data.")
                }
            }

            val testSuccess = successCount
            val testFail = failureCount
            // Mark the transaction as successful

        } catch (e: Exception) {
            e.printStackTrace() // Log the full stack trace
            Log.e("DB_INSERT", "Error during DB update", e)
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun insertDataLogs(dbLogs: DbDataLogs) {
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            var successCount = 0 // Counter for successful inserts
            var failureCount = 0 // Counter for failed inserts

            // ContentValues for inserting or updating
            val values = ContentValues().apply {
                put("$COL_executeDt", dbLogs.executeDt)
                put("$COL_dataSize", dbLogs.dataSize)
                put("$COL_execType", dbLogs.execType)
            }

            val whereClause = "executeDt = ?"
            val whereArgs = arrayOf(dbLogs.execType)

            val result = db.update("dataLogs", values, whereClause, whereArgs)

            if (result != -1) {
                successCount++ // Increment success counter
            } else {
                failureCount++ // Increment failure counter
                Log.e("DB_UPDATE", "Failed to update data.")
            }

            val testSuccess = successCount
            val testFail = failureCount

            db.setTransactionSuccessful() // Mark the transaction as successful
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error inserting or updating data", e)
        } finally {
            // Ensure the transaction is ended properly
            db.endTransaction()
        }
    }

    fun getDataByFlightCode(flightCode: String): DbFlight {
        try {
            val db = this.readableDatabase // Use readableDatabase to fetch data
            val query = "SELECT * FROM $TABLE_NAME WHERE $COL_FlightCode = ?"

            // Initialize collections to store data
            val flights = mutableListOf<Int>()
            val flightCodes = mutableListOf<String>()
            val departureAirportFsCodes = mutableListOf<String>()
            val departureDates = mutableListOf<String>()
            val delayFlights = mutableListOf<String>()
            val createdDt = mutableListOf<String>()

            var cursor: Cursor? = null

            try {
                cursor = db.rawQuery(query, arrayOf(flightCode))
                while (cursor.moveToNext()) {
                    // Collect data from the cursor
                    flights.add(cursor.getInt(cursor.getColumnIndexOrThrow(COL_FlightId)))
                    flightCodes.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightCode)))
                    departureAirportFsCodes.add(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COL_DepartureAirportFsCode
                            )
                        )
                    )
                    departureDates.add(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COL_DepartureDate
                            )
                        )
                    )

                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("DB_ERROR", "Error fetching data for flightCode: $flightCode", e)
            } finally {
                cursor?.close() // Ensure the cursor is closed
            }

            // Return result
            return if (flights.isNotEmpty()) {
                DbFlight(
                    flightIds = flights.toString(),
                    flightCodes = flightCodes.toString(),
                    departureAirportFsCodes = departureAirportFsCodes.toString(),
                    departureDates = departureDates.toString(),
                    delayFlights = delayFlights.toString(),
                    createdDt = createdDt.toString()
                )
            } else {
                DbFlight(
                    flightIds = "",
                    flightCodes = "",
                    departureAirportFsCodes = "",
                    departureDates = "",
                    delayFlights = "",
                    createdDt = ""
                )
            }
        }catch (e : Exception)
        {
            e.printStackTrace()
            Log.e("DB_ERROR", "Error fetching data for flightCode: $flightCode", e)
        }
        return    DbFlight(
            flightIds = "",
            flightCodes = "",
            departureAirportFsCodes = "",
            departureDates = "",
            delayFlights = "",
            createdDt = ""
        )
    }

}