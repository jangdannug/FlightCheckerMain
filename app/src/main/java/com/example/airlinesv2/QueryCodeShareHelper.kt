import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


fun getCodeshares(flightStatusesData: JsonArray?): List<Pair<Long, String>> {
    val codeshareList = mutableListOf<Pair<Long, String>>()

    // Check if flightStatusesData is not null
    flightStatusesData?.forEach { flightStatusElement ->
        // Each element is a JsonObject
        val flightStatusObject = flightStatusElement.jsonObject

        // Get the flightId
        val flightId = flightStatusObject["flightId"]?.jsonPrimitive?.content?.toLongOrNull()

        // Get the codeshares array
        val codeshares: JsonArray? = flightStatusObject["codeshares"]?.jsonArray

        // If flightId is valid and codeshares is not null
        if (flightId != null && codeshares != null) {
            // Create a string representation of the codeshares
            val codeshareString = codeshares.joinToString(", ") { codeshareElement ->
                val codeshareObject = codeshareElement.jsonObject
                val carrier = codeshareObject["carrier"]?.jsonObject
                val fsCode = carrier?.get("fs")?.jsonPrimitive?.content ?: ""
                val iata = carrier?.get("iata")?.jsonPrimitive?.content ?: ""
                val icao = carrier?.get("icao")?.jsonPrimitive?.content ?: ""
                val name = carrier?.get("name")?.jsonPrimitive?.content ?: ""
                val active = carrier?.get("active")?.jsonPrimitive?.content ?: false
                val flightNumber = codeshareObject["flightNumber"]?.jsonPrimitive?.content ?: ""
                val relationship = codeshareObject["relationship"]?.jsonPrimitive?.content ?: ""

                // Format the codeshare as desired
                """
                {
                    "carrier": {
                        "fs": "$fsCode",
                        "iata": "$iata",
                        "icao": "$icao",
                        "name": "$name",
                        "active": $active
                    },
                    "flightNumber": "$flightNumber",
                    "relationship": "$relationship"
                }
                """.trimIndent()
            }

            // Add the flightId and codeshare string to the list
            codeshareList.add(Pair(flightId, codeshareString))
        }
    }

    return codeshareList
}