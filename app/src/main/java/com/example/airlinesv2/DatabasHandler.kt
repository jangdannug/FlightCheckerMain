package com.example.airlinesv2

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale

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

class DataBaseHandler ( context: Context) : SQLiteOpenHelper(context, DATABASE_NAME,null,1) {

    init {
    // Delete the existing database file
    context.deleteDatabase(DATABASE_NAME)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = " CREATE TABLE " +
                TABLE_NAME +
                " (" +
                COL_FlightId + " INTEGER PRIMARY KEY," +
                COL_FlightCode + " TEXT," +
                COL_DepartureAirportFsCode + " TEXT," +
                COL_DepartureDate + " TEXT" +

                " )"
        db?.execSQL(createTable)

        // Create DataLogs table
        val createTableDataLogs = "CREATE TABLE " +
                TABLE_dataLogs +
                " (" +
                COL_executeDt + " TEXT," +
                COL_dataSize + "TEXT," +
                COL_execType + " TEXT" +
                " )"
        db?.execSQL(createTableDataLogs)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME") // Drop the existing table
        onCreate(db) // Recreate the table
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

                // Prepare individual flight data based on the index
                val flightId = (flights.flightIds[i] as? JsonElement)?.jsonPrimitive?.contentOrNull ?: flights.flightIds[i].toString()
                val flightCode = (flights.flightCodes[i] as? JsonElement)?.jsonPrimitive?.contentOrNull ?: flights.flightCodes[i]
                val departureAirportFsCode = (flights.departureAirportFsCodes[i] as? JsonElement)?.jsonPrimitive?.contentOrNull ?: flights.departureAirportFsCodes[i]
                val departureDate = (flights.departureDates[i] as? JsonElement)?.jsonPrimitive?.contentOrNull ?: flights.departureDates[i]

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
                    Log.e("DB_INSERT", "Failed to insert data. Flight ID: $flightId, ContentValues: $cv")
                }
            }

            val testSuccess = successCount
            val testFail = failureCount
            // Mark the transaction as successful

        }catch (e:Exception){
            e.printStackTrace() // Log the full stack trace
            Log.e("DB_INSERT", "Error during DB update", e)
        }
    }

    fun insertDataLogs(dbLogs: DbDataLogs) {
        val db = this.writableDatabase

        try {
            var successCount = 0 // Counter for successful inserts
            var failureCount = 0 // Counter for failed inserts

            // ContentValues for inserting or updating
            val cv = ContentValues()
            cv.put(COL_executeDt, dbLogs.executeDt)
            cv.put(COL_dataSize, dbLogs.dataSize)
            cv.put(COL_execType, dbLogs.execType)

            // Check if a record already exists with the same executeDt
            val selection = "$COL_execType = ?"
            val selectionArgs = arrayOf(dbLogs.execType)

            val cursor = db.query(
                TABLE_dataLogs,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                // Record exists, perform UPDATE
                val result = db.update(
                    TABLE_dataLogs,
                    cv,
                    selection,
                    selectionArgs
                )
                if (result != -1) {
                    successCount++ // Increment success counter
                } else {
                    failureCount++ // Increment failure counter
                    Log.e("DB_UPDATE", "Failed to update data.")
                }
            } else {
                // Record doesn't exist, perform INSERT
                val result = db.insert(TABLE_dataLogs, null, cv)
                if (result != -1L) {
                    successCount++ // Increment success counter
                } else {
                    failureCount++ // Increment failure counter
                    Log.e("DB_INSERT", "Failed to insert data.")
                }
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

}