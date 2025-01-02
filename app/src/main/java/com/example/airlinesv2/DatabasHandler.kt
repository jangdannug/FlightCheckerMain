package com.example.airlinesv2

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
        //context.deleteDatabase(DATABASE_NAME)
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
            // Validate list sizes
            if (flights.flightIds.size != flights.flightCodes.size ||
                flights.flightIds.size != flights.departureAirportFsCodes.size ||
                flights.flightIds.size != flights.departureDates.size) {
                Log.e("DB_INSERT", "List sizes are inconsistent")
                return
            }

            var successCount = 0
            var failureCount = 0

            for (i in flights.flightIds.indices) {
                val values = ContentValues().apply {
                    put(COL_FlightId, flights.flightIds[i])
                    put(COL_FlightCode, flights.flightCodes[i])
                    put(COL_DepartureAirportFsCode, flights.departureAirportFsCodes[i])
                    put(COL_DepartureDate, flights.departureDates[i])
                }

                val result = db.insertWithOnConflict(
                    TABLE_NAME,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE // Replace if conflict on primary key
                )

                if (result != -1L) {
                    successCount++
                } else {
                    failureCount++
                    Log.e("DB_INSERT", "Failed to insert data for flightId: ${flights.flightIds[i]}")
                }
            }

            // Mark transaction as successful
            db.setTransactionSuccessful()
            Log.d("DB_INSERT", "Inserted: $successCount, Failed: $failureCount")
        } catch (e: Exception) {
            Log.e("DB_INSERT", "Error during DB insert", e)
        } finally {
            db.endTransaction() // End the transaction
            db.close()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun insertDataLogs(dbLogs: DbDataLogs) {
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            // Delete all existing records
            db.execSQL("DELETE FROM $TABLE_dataLogs")

            // ContentValues for inserting new data
            val values = ContentValues().apply {
                put(COL_executeDt, dbLogs.executeDt) // No need for "$" here
                put(COL_dataSize, dbLogs.dataSize) // Ensure this is the correct type
                put(COL_execType, dbLogs.execType)
            }

            // Insert new data
            val newRowId = db.insert(TABLE_dataLogs, null, values)
            if (newRowId != -1L) {
                Log.d("DB_INSERT", "Data inserted successfully with row ID: $newRowId")
            } else {
                Log.e("DB_INSERT", "Failed to insert new data.")
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error inserting data", e)
        } finally {
            // Ensure the transaction is ended properly
            db.endTransaction()
            db.close()
        }
    }

    fun checkIfTableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val query = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        val cursor = db.rawQuery(query, arrayOf(tableName))
        val exists = cursor.count > 0
        return exists
    }


    fun getDataByFlightCode(flightCode: String): DbFlight {
        try {
            val db = this.readableDatabase

            // Check if the table exists
            if (!checkIfTableExists(db, TABLE_NAME)) {
                Log.e("DB_ERROR", "Table $TABLE_NAME does not exist in the database")
                return DbFlight("", "", "", "")
            }

            val query = "SELECT * FROM $TABLE_NAME WHERE $COL_FlightCode = ?"
            var cursor: Cursor? = null

            try {
                cursor = db.rawQuery(query, arrayOf(flightCode))

                if (cursor.moveToFirst()) {
                    val flightId = cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightId))
                    val flightCodeResult = cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightCode))
                    val departureAirportFsCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_DepartureAirportFsCode))
                    val departureDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_DepartureDate))

                    return DbFlight(
                        flightIds = flightId,
                        flightCodes = flightCodeResult,
                        departureAirportFsCodes = departureAirportFsCode,
                        departureDates = departureDate
                    )
                } else {
                    Log.d("DB_QUERY", "No results found for flightCode: $flightCode")
                }
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Error fetching data for flightCode: $flightCode", e)
            } finally {
                cursor?.close()
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error initializing database operation", e)
        }

        return DbFlight("", "", "", "")
    }


    fun getLatestUpdate(): String? {
        val db = this.readableDatabase
        var latestExecuteDt: String? = null

        //Verify if there are rows
        val dataLogsCount = countDataLogs()
        val flightCount = countFlights()

        if (dataLogsCount > 0 || flightCount > 0)
        {
            try {

                // Query to get the latest executeDt
                val query = "SELECT * FROM $TABLE_dataLogs ORDER BY $COL_executeDt DESC LIMIT 1"

                val cursor = db.rawQuery(query, null)

                // Log the number of rows returned
                val rowCount = cursor.count
                println("Number of rows returned: $rowCount")


                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(COL_executeDt)
                    if (columnIndex != -1) {
                        latestExecuteDt = cursor.getString(columnIndex)
                    } else {
                        println("Column $COL_executeDt not found in cursor.")
                    }
                }

                cursor.close()
                db.close()
                return latestExecuteDt
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    fun countDataLogs(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_dataLogs", null)
        cursor.moveToFirst()
        val count = cursor.getInt(0)
        cursor.close()
        return count
    }

    fun countFlights(): Int {
        val db = this.readableDatabase
        var count = 0
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }




    fun deleteDatabase(context: Context) {
        context.deleteDatabase(DATABASE_NAME)
    }

}