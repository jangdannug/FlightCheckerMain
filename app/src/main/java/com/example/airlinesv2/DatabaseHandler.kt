package com.example.airlinesv2

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val DATABASE_NAME = "FLIGHTS"
const val TABLE_FlightStatuses = "FlightStatuses"
const val COL_FlightId = "flightIds"
const val COL_CarrierIata = "carrierIata"
const val COL_FlightNumber = "flightNumber"
const val COL_DepartureDateTime = "departureDateTime"
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
                "$COL_DepartureDateTime DATETIME," +
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
                flights.flightIds.size != flights.batchType.size
            ) {
                Log.e("DB_INSERT", "List sizes are inconsistent")
                return
            }

            var successCount = 0
            var failureCount = 0

            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            var formattedDepartureDate = mutableListOf<String>()
            for (dateTimeString in flights.departureDates) {
                val localDateTime = LocalDateTime.parse(dateTimeString, inputFormatter)
                val formattedDateString = localDateTime.format(outputFormatter)
                formattedDepartureDate.add(formattedDateString)
            }


            for (i in flights.flightIds.indices) {

                val values = ContentValues().apply {
                    put(COL_FlightId, flights.flightIds[i])
                    put(COL_CarrierIata, flights.carrierFsCode[i])
                    put(COL_FlightNumber, flights.flightNumber[i])
                    put(COL_DepartureDateTime, flights.departureDates[i].format(DateTimeFormatter.ISO_DATE_TIME))
                    put(COL_DepartureDate,formattedDepartureDate[i])
                    put(
                        COL_QueryDate,
                        flights.queryDates[i].format(DateTimeFormatter.ISO_LOCAL_DATE)
                    )
                    put(COL_BatchType, flights.batchType[i])

                    val codeShare = flights.codeShare[i]
                    put(COL_CodeShareId, codeShare.first)
                    put(COL_CodeShareData, codeShare.second)

                }


                val result = db.insert(
                    TABLE_FlightStatuses, null, values
                )

                if (result != -1L) {
                    successCount++
                } else {
                    failureCount++
                    Log.e(
                        "DB_INSERT",
                        "Failed to insert data for flightId: ${flights.flightIds[i]} due to conflict or other issue."
                    )
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
            for ( i in dbLogs.execType.indices) {
                // ContentValues for inserting new data
                val values = ContentValues().apply {
                    put(COL_executeDt, dbLogs.executeDt[i]) // No need for "$" here
                    put(COL_dataSize, dbLogs.dataSize[i]) // Ensure this is the correct type
                    put(COL_execType, dbLogs.execType[i])
                }
                val newRowId = db.insert(TABLE_dataLogs, null, values)
                if (newRowId != -1L) {
                    Log.d("DB_INSERT", "Data inserted successfully with row ID: $newRowId")
                } else {
                    Log.e("DB_INSERT", "Failed to insert new data.")
                }
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
                        val existingIata =
                            cursor.getString(cursor.getColumnIndex(COL_Iata)) // Get existing IATA code
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

            val ticketDate =
                barcodeData.flightDate // Assuming ticketDate is in the same format as departureDate
            // Check if the table exists
            if (!checkIfTableExists(db, TABLE_FlightStatuses)) {
                Log.e("DB_ERROR", "Table $TABLE_FlightStatuses does not exist in the database")
                return DbFlight("", "", "", "")
            }
            barcodeData.flightIata
            barcodeData.flightNumber

            val checkDateTime = LocalDateTime.now()

            val query = """
                            SELECT * 
                            FROM $TABLE_FlightStatuses 
                            WHERE $COL_FlightNumber = ?
                            AND $COL_CarrierIata = ?
                            AND $COL_QueryDate = ?
                            AND $COL_DepartureDate = ?
                            AND $COL_DepartureDateTime >= DATETIME('now', '-3 hours')
                            ORDER BY departureDateTime DESC LIMIT 1
                        """
            var cursor: Cursor? = null

            var flightNumber = barcodeData.flightNumber

            if (flightNumber.startsWith("000")) {
                flightNumber = flightNumber.substring(3)
            } else if (flightNumber.startsWith("00")) {
                flightNumber = flightNumber.substring(2)
            } else if (flightNumber.startsWith("0")) {
                flightNumber = flightNumber.substring(1)
            }

            try {

                cursor = db.rawQuery(
                    query,
                    arrayOf(flightNumber, barcodeData.Iata, barcodeData.flightDate.toString(), barcodeData.flightDate.toString())
                )
                var cnt = cursor.count

                if (cnt != 1) {
                    val codeShareData =
                        "SELECT * FROM FlightStatuses WHERE  FlightNumber = ?  and queryDate = ? and departureDate = ? and codeShareData LIKE  ? AND codeShareData LIKE ?"
                    val flightNumberWildcard = "%\"flightNumber\": \"$flightNumber\"%"
                    val iataWildcard = "%${barcodeData.Iata}%"
                    cursor = db.rawQuery(codeShareData, arrayOf(flightNumber,barcodeData.flightDate.toString(), barcodeData.flightDate.toString(),flightNumberWildcard,iataWildcard))
                    cnt = cursor.count
                }

               if (cnt != 1){
                  val delayedFlightQuery =
                       "SELECT * FROM flightstatuses where flightNumber = ? and  carrierIata = ? and queryDate = ? order by departureDateTime desc limit 1"
                   cursor = db.rawQuery(delayedFlightQuery, arrayOf(flightNumber,barcodeData.Iata,barcodeData.flightDate.toString()))
                }
                cnt = cursor.count

                if (cnt != 1){
                    val nearestFlight =
                        "SELECT * FROM flightstatuses where flightNumber = ? and  carrierIata = ? order by departureDateTime desc limit 1"
                    cursor = db.rawQuery(nearestFlight, arrayOf(flightNumber,barcodeData.Iata))
                }
                cnt = cursor.count

                val currentDateTime = LocalDateTime.now() // Get current date and time
                var preferredFlight: DbFlight? = null
                var closestFlight: DbFlight? = null

                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")


                if (cursor.moveToFirst()) {
                    val flightStat_flightId =
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightId))

                    var flightStat_carrierIata =
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CarrierIata))
                    val flightStat_flightNumber =
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FlightNumber))
                    val flightStat_departureDate =
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DepartureDateTime))
                    val codeShare_codeShareData =
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CodeShareData))

                    return DbFlight(
                        flightIds = flightStat_flightId,
                        carrierIata = flightStat_carrierIata,
                        flightNumber = flightStat_flightNumber,
                        departureDates = flightStat_departureDate
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "DB_ERROR",
                    "Error fetching data for flightCode: ${barcodeData.flightIata}",
                    e
                )
            } finally {
                cursor?.close()
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error initializing database operation", e)
        }

        return DbFlight("", "", "", "")
    }


    fun getLatestUpdate(): DbDataLogsReturn {
        val db = this.readableDatabase

        //Verify if there are rows
        val dataLogsCount = countDataLogs()
        val flightCount = countFlights()

        if (dataLogsCount > 0 || flightCount > 0) {
            try {

                // Query to get the latest executeDt
                val query = "SELECT * FROM $TABLE_dataLogs ORDER BY $COL_execType DESC LIMIT 1"

                val cursor = db.rawQuery(query, null)

                // Log the number of rows returned
                val rowCount = cursor.count
                println("Number of rows returned: $rowCount")

                var latestExecuteDt = ""
                var dataSize = ""
                var execType = ""

                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(COL_executeDt)
                    if (columnIndex != -1) {
                        latestExecuteDt = cursor.getString(cursor.getColumnIndexOrThrow(
                           COL_executeDt))
                         dataSize = cursor.getString(cursor.getColumnIndexOrThrow(COL_dataSize))
                         execType = cursor.getString(cursor.getColumnIndexOrThrow(COL_execType))
                    } else {
                        println("Column $COL_executeDt not found in cursor.")
                    }
                }
                val dbData =  DbDataLogsReturn(
                    executeDt = latestExecuteDt,
                    dataSize = dataSize,
                    execType = execType
                )

                cursor.close()
                db.close()
                return dbData
            } catch (e: Exception) {
                return DbDataLogsReturn("","","")
            }
        }
        return DbDataLogsReturn("","","")
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


    fun deleteFlightData() {
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

    fun deleteDataLogsData() {
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

    fun deleteFlightCodesData() {
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