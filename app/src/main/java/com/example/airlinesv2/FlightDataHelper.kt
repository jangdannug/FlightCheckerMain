package com.example.airlinesv2

class Flights {
    var flightIds: List<Int>  // List of integers for flightIds
    var flightCodes: List<String>  // List of strings for flightCodes
    var departureAirportFsCodes: List<String>  // List of strings for departureAirportFsCodes
    var departureDates: List<String>  // List of strings for departureDates

    constructor(
        flightIds: List<Int>, flightCodes: List<String>,
        departureAirportFsCodes: List<String>, departureDates: List<Any>

    ) {
        this.flightIds = flightIds
        this.flightCodes = flightCodes
        this.departureAirportFsCodes = departureAirportFsCodes
        this.departureDates = departureDates as List<String>
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

class DbFlightNull{
    var flightIds: String
    var flightCodes: String
    var departureAirportFsCodes: String
    var departureDates: String

    constructor(
        flightIds: String, flightCodes: String,
        departureAirportFsCodes: String, departureDates: String
    ) {
        this.flightIds = ""
        this.flightCodes = ""
        this.departureAirportFsCodes = ""
        this.departureDates = ""
    }
}