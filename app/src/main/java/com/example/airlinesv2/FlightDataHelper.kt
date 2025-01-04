package com.example.airlinesv2

import java.time.LocalDate
import java.time.LocalDateTime

class Flights {
    var flightIds: List<Int>  // List of integers for flightIds
    var carrierFsCode: List<String>  // List of strings for flightCodes
    var flightNumber: List<String>  // List of strings for departureAirportFsCodes
    var departureDates: List<String>  // List of strings for departureDates
    var queryDates: List<LocalDateTime>

    constructor(
        flightIds: List<Int>, carrierFsCode: List<String>,
        flightNumber: List<String>, departureDates: List<Any>, queryDate: List<LocalDateTime>

    ) {
        this.flightIds = flightIds
        this.carrierFsCode = carrierFsCode
        this.flightNumber = flightNumber
        this.departureDates = departureDates as List<String>
        this.queryDates = queryDate
    }
}


class DbFlight{
    var flightIds: String
    var carrierFsCode: String
    var flightNumber: String
    var departureDates: String

    constructor(
        flightIds: String, carrierFsCode: String,
        flightNumber: String, departureDates: String
    ) {
        this.flightIds = flightIds
        this.carrierFsCode = carrierFsCode
        this.flightNumber = flightNumber
        this.departureDates = departureDates
    }
}

class DbDataLogs{
    var executeDt : String
    var dataSize : String
    var execType : String

    constructor(
        executeDt: String, dataSize: String, execType: String
    ){
        this.executeDt = executeDt
        this.dataSize = dataSize
        this.execType = execType
    }
}

class DbDataFlight{
    var flightId: String
    var flightCode: String
    var departureAirportFsCode: String
    var departureDate: String

    constructor(
        flightId: String, flightCode: String,
        departureAirportFsCode: String, departureDate: String
    ) {
        this.flightId = flightId
        this.flightCode = flightCode
        this.departureAirportFsCode = departureAirportFsCode
        this.departureDate = departureDate
    }
}