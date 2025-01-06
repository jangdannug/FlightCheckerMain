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
import java.time.format.DateTimeFormatter

const val DATABASE_NAME = "FLIGHTS"
const val TABLE_FlightStatuses = "FlightStatuses"
const val COL_FlightId = "flightIds"
const val COL_CarrierIata = "carrierIata"
const val COL_FlightNumber = "flightNumber"
const val COL_DepartureDate = "departureDate"
const val  COL_QueryDate = "queryDate"
const val  COL_CodeShareData = "codeShareData"
const val  COL_CodeShareId = "codeShareId"
const val  COL_BatchType = "batchType"

const val  TABLE_dataLogs = "DataLogs"
const val COL_executeDt = "executeDt"
const val COL_dataSize = "dataSize"
const val COL_execType = "execType"

const val TABLE_Codes = "FlightCodes"
const val COL_FsCode = "fsCode"
const val COL_Iata = "iata"

class DataBaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME,null,1) {

    init {
        // Delete the existing database file
        //context.deleteDatabase(DATABASE_NAME)
    }


    override fun onCreate(db: SQLiteDatabase?) {

        val createTable = " CREATE TABLE $TABLE_FlightStatuses (" +
                "$COL_FlightId INTEGER PRIMARY KEY," +
                "$COL_CarrierIata TEXT," +
                "$COL_FlightNumber TEXT," +
                "$COL_DepartureDate TEXT," +
                "$COL_BatchType TEXT," +
                "$COL_QueryDate TEXT," +
                "$COL_CodeShareId TEXT," +
                "$COL_CodeShareData TEXT)"
        db?.execSQL(createTable)

        // Create DataLogs table
        val createTableDataLogs = "CREATE TABLE $TABLE_dataLogs (" +
                "$COL_executeDt TEXT," +
                "$COL_dataSize TEXT," +
                "$COL_execType TEXT)"

        db?.execSQL(createTableDataLogs)

        val createTableCodes = "CREATE TABLE $TABLE_Codes (" +
                "$COL_FsCode TEXT," +
                "$COL_Iata TEXT)"

        db?.execSQL(createTableCodes)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FlightStatuses")  // Drop the existing table if it exists
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_dataLogs")  // Drop another table if it exists
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_Codes")  // Drop another table if it exists

        onCreate(db)  // Recreate the tables as defined in the onCreate method
    }



    fun insertFlights(flights: Flights) {
        val db = this.writableDatabase
        db.beginTransaction()

        val dateUpdate = LocalDateTime.now()

        val test = flights.codeShare

        try {
            // Validate list sizes
            if (flights.flightIds.size != flights.carrierFsCode.size ||
                flights.flightIds.size != flights.flightNumber.size ||
                flights.flightIds.size != flights.departureDates.size ||
                flights.flightIds.size != flights.queryDates.size ||
                flights.flightIds.size != flights.codeShare.size ||
                flights.flightIds.size != flights.batchType.size){
                Log.e("DB_INSERT", "List sizes are inconsistent")
                return
            }

            var successCount = 0
            var failureCount = 0

            for (i in flights.flightIds.indices) {
                val values = ContentValues().apply {
                    put(COL_FlightId, flights.flightIds[i])
                    put(COL_CarrierIata, flights.carrierFsCode[i])
                    put(COL_FlightNumber, flights.flightNumber[i])
                    put(COL_DepartureDate, flights.departureDates[i])
                    put(COL_QueryDate, flights.queryDates[i].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    put(COL_BatchType,flights.batchType[i])

                    val  codeShare = flights.codeShare[i]
                    put(COL_CodeShareId, codeShare.first)
                    put(COL_CodeShareData, codeShare.second)

                }


                val result = db.insert(
                    TABLE_FlightStatuses, null, values)

                if (result != -1L) {
                    successCount++
                } else {
                    failureCount++
                    Log.e("DB_INSERT", "Failed to insert data for flightId: ${flights.flightIds[i]} due to conflict or other issue.")
                }
            }

            // Mark transaction as successful
            db.setTransactionSuccessful()
            Log.d("DB_INSERT", "Inserted: $successCount, Failed: $failureCount")
        } catch (e: Exception) {
            Log.e("DB_INSERT", "Error during DB insert: ${e.message}", e)
        } finally {
            db.endTransaction() // End the transaction
            db.close() // Ensure the database is closed
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


    @SuppressLint("SuspiciousIndentation", "Range")
    fun insertCodes(fsCodesList: List<DbFsCodes>) {
        val db = this.writableDatabase
        db.beginTransaction()

        try {
            for (fsCodes in fsCodesList) {
                // Check if the data already exists
                val cursor = db.query(
                    TABLE_Codes,
                    null,
                    "$COL_FsCode = ?", // Use the correct column name
                    arrayOf(fsCodes.fsCode), // Ensure fsCode is passed correctly
                    null,
                    null,
                    null
                )

                if (cursor != null) {
                    if (cursor.count > 0) {
                        cursor.moveToFirst()
                        val existingIata = cursor.getString(cursor.getColumnIndex(COL_Iata)) // Get existing IATA code
                        if (existingIata != fsCodes.iataCode) {
                            // Update the data if it has changed
                            val values = ContentValues().apply {
                                put(COL_Iata, fsCodes.iataCode)
                            }
                            val rowsAffected = db.update(
                                TABLE_Codes,
                                values,
                                "$COL_FsCode = ?", // Use the correct column name
                                arrayOf(fsCodes.fsCode) // Ensure fsCode is passed correctly
                            )
                            if (rowsAffected > 0) {
                                Log.d("DB_UPDATE", "Data updated successfully.")
                            } else {
                                Log.e("DB_UPDATE", "Failed to update data.")
                            }
                        } else {
                            Log.d("DB_NO_CHANGE", "No changes detected.")
                        }
                    } else {
                        // Insert new data
                        val values = ContentValues().apply {
                            put(COL_FsCode, fsCodes.fsCode)
                            put(COL_Iata, fsCodes.iataCode)
                        }
                        val newRowId = db.insert(TABLE_Codes, null, values)
                        if (newRowId != -1L) {
                            Log.d("DB_INSERT", "Data inserted successfully with row ID: $newRowId")
                        } else {
                            Log.e("DB_INSERT", "Failed to insert new data.")
                        }
                    }
                    cursor.close() // Close the cursor after use
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error inserting or updating data", e)
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



    fun getDataByFlightCode(barcodeData: BarcodeData): DbFlight? {


        try {
            val db = this.readableDatabase

            val ticketDate = barcodeData.flightDate // Assuming ticketDate is in the same format as departureDate
            // Check if the table exists
            if (!checkIfTableExists(db, TABLE_FlightStatuses)) {
                Log.e("DB_ERROR", "Table $TABLE_FlightStatuses does not exist in the database")
                return DbFlight("", "", "", "")
            }
            barcodeData.flightIata
            barcodeData.flightNumber


            val query = """
    SELECT * 
    FROM $TABLE_FlightStatuses 
    WHERE $COL_FlightNumber = ?
"""
            var cursor: Cursor? = null

            var flightNumber = barcodeData.flightNumber

            if (flightNumber.startsWith("000")) {
                flightNumber = flightNumber.substring(3)
            }else if (flightNumber.startsWith("00")){
                flightNumber = flightNumber.substring(2)
            }else if (flightNumber.startsWith("0")){
                flightNumber = flightNumber.substring(1)
            }

            try {
                cursor = db.rawQuery(query, arrayOf(flightNumber))

                //cursor = db.rawQuery(query, arrayOf("279"))
                //cursor = db.rawQuery(query, arrayOf("681", "IX"))



                val currentDateTime = LocalDateTime.now() // Get current date and time
                var preferredFlight: DbFlight? = null
                var closestFlight: DbFlight? = null

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")


                if (cursor.moveToFirst()) {
                    do {
                        val flightStat_flightId =
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightId))

                        var flightStat_carrierIata =
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CarrierIata))
                        val flightStat_flightNumber =
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightNumber))
                        val flightStat_departureDate =
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_DepartureDate))
                        val codeShare_codeShareData =
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CodeShareData))


                        //Instantiate params

                        var isContainsIataFlightNumber = false

                        if (codeShare_codeShareData.contains(barcodeData.Iata) &&
                            codeShare_codeShareData.contains(barcodeData.flightNumber)) {
                            isContainsIataFlightNumber = true
                        }

                        val departureDateTime = LocalDateTime.parse(flightStat_departureDate)

                        val id = flightStat_flightId
                        if (flightStat_carrierIata == barcodeData.Iata  ) {
                        val test = "success"
                        }

                        if (isContainsIataFlightNumber)
                        {
                            val test = "success"
                        }

                        if ( departureDateTime.toLocalDate() == LocalDate.parse(ticketDate.toString()))
                        {
                            val  test = "Success"
                        }

                        // Check if the departure date matches the ticket date
                        if ( (barcodeData.Iata == flightStat_carrierIata || isContainsIataFlightNumber) &&
                            departureDateTime.toLocalDate() == LocalDate.parse(ticketDate.toString())) {
                            // If it matches, set it as preferredFlight
                            if (departureDateTime.isBefore(currentDateTime) &&
                                (preferredFlight == null ||
                                        departureDateTime.isBefore(LocalDateTime.parse(preferredFlight.departureDates)))) {
                                preferredFlight = DbFlight(
                                    flightIds = flightStat_flightId,
                                    carrierIata = flightStat_carrierIata,
                                    flightNumber = flightNumber,
                                    departureDates = flightStat_departureDate
                                )
                            }
                            //break // Exit the loop since we found a preferred flight
                        }
                        // Track the closest flight regardless of whether it's in the past or future
                        if (departureDateTime.isBefore(LocalDateTime.parse(closestFlight?.departureDates)) &&
                            (flightStat_carrierIata == barcodeData.Iata || !isContainsIataFlightNumber)) {
                            closestFlight = DbFlight(
                                flightIds = flightStat_flightId,
                                carrierIata = flightStat_carrierIata,
                                flightNumber = flightNumber,
                                departureDates = flightStat_departureDate
                            )
                        }
                    } while (cursor.moveToNext())
                }

                val sixHoursAgo = currentDateTime.minusHours(6)
                val preferredDepartureDateTime = LocalDateTime.parse(preferredFlight?.departureDates)
                if (preferredDepartureDateTime.isBefore(sixHoursAgo) &&
                    preferredFlight?.carrierIata == barcodeData.Iata)
                {
                    return preferredFlight
                }else
                {
                    return closestFlight
                }
                // Return the preferred flight if found, otherwise return the next flight
                return preferredFlight ?: closestFlight ?: DbFlight("", "", "", "")
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Error fetching data for flightCode: ${barcodeData.flightIata}", e)
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
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_FlightStatuses", null)
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }


    fun  deleteFlightData()
    {
        val db = this.writableDatabase

        try {
            db.beginTransaction()

            db.execSQL("DELETE FROM $TABLE_FlightStatuses")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB_INSERT", "Error during DB insert:")
        } finally {
            db.endTransaction() // End the transaction
            db.close() // Ensure the database is closed
        }

    }

    fun  deleteDataLogsData()
    {
        val db = this.writableDatabase

        try {
            db.beginTransaction()

            db.execSQL("DELETE FROM $TABLE_dataLogs")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB_INSERT", "Error during DB insert:")
        } finally {
            db.endTransaction() // End the transaction
            db.close() // Ensure the database is closed
        }

    }

    fun  deleteFlightCodesData()
    {
        val db = this.writableDatabase

        try {
            db.beginTransaction()

            db.execSQL("DELETE FROM $TABLE_Codes")
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DB_INSERT", "Error during DB insert:")
        } finally {
            db.endTransaction() // End the transaction
            db.close() // Ensure the database is closed
        }

    }

    fun deleteDatabase(context: Context) {
        context.deleteDatabase(DATABASE_NAME)
    }


}