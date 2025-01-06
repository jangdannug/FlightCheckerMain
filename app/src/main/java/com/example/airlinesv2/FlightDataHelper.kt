package com.example.airlinesv2

import java.time.LocalDateTime

class Flights {
    var flightIds: List<Int>  // List of integers for flightIds
    var carrierFsCode: List<String>  // List of strings for flightCodes
    var flightNumber: List<String>  // List of strings for departureAirportFsCodes
    var departureDates: List<String>  // List of strings for departureDates
    var queryDates: List<LocalDateTime>
    var codeShare: List<Pair<Long, String>>
    var batchType: List<String>

    constructor(
        flightIds: List<Int>,
        carrierFsCode: List<String>,
        flightNumber: List<String>,
        departureDates: List<Any>,
        queryDate: List<LocalDateTime>,
        codeShare: List<Pair<Long, String>>,
        batchType: List<String>

    ) {
        this.flightIds = flightIds
        this.carrierFsCode = carrierFsCode
        this.flightNumber = flightNumber
        this.departureDates = departureDates as List<String>
        this.queryDates = queryDate
        this.codeShare = codeShare
        this.batchType = batchType
    }
}


class DbFlight{
    var flightIds: String
    var carrierIata: String
    var flightNumber: String
    var departureDates: String

    constructor(
        flightIds: String, carrierIata: String,
        flightNumber: String, departureDates: String
    ) {
        this.flightIds = flightIds
        this.carrierIata = carrierIata
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

class DbFsCodes{
    var fsCode: String
    var iataCode: String

    constructor(
        fsCode: String, iataCode: String
    ){
        this.fsCode = fsCode
        this.iataCode = iataCode
    }
}


class DbCodeShare{
    var fsCode: String
    var flightNumber: String
    var relationship : String

    constructor(
        fsCode: String, flightNumber: String, relationship : String
    ){
        this.fsCode = fsCode
        this.flightNumber = flightNumber
        this.relationship = relationship
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