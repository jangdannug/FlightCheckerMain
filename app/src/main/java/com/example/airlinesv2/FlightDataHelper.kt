package com.example.airlinesv2

import java.time.LocalDate

class Flights {
    var flightIds: List<Int>  // List of integers for flightIds
    var flightCodes: List<String>  // List of strings for flightCodes
    var departureAirportFsCodes: List<String>  // List of strings for departureAirportFsCodes
    var departureDates: List<String>  // List of strings for departureDates
    var queryDates: List<LocalDate>

    constructor(
        flightIds: List<Int>, flightCodes: List<String>,
        departureAirportFsCodes: List<String>, departureDates: List<Any>, queryDate: List<LocalDate>

    ) {
        this.flightIds = flightIds
        this.flightCodes = flightCodes
        this.departureAirportFsCodes = departureAirportFsCodes
        this.departureDates = departureDates as List<String>
        this.queryDates = queryDate
    }
}


class DbFlight{
    var flightIds: String
    var flightCodes: String
    var departureAirportFsCodes: String
    var departureDates: String

    constructor(
        flightIds: String, flightCodes: String,
        departureAirportFsCodes: String, departureDates: String
    ) {
        this.flightIds = flightIds
        this.flightCodes = flightCodes
        this.departureAirportFsCodes = departureAirportFsCodes
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