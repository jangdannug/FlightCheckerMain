import com.example.airlinesv2.DbFsCodes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun getAppendix(jsonData: JsonObject?): List<DbFsCodes> {
    // Get the appendix as a JsonObject
    val appendix = jsonData?.get("appendix")?.jsonObject

    // Get the airlines as a JsonArray
    val airlines: JsonArray? = if (appendix != null && appendix.containsKey("airlines")) {
        appendix["airlines"]?.jsonArray
    } else {
        null // or you can initialize it to an empty JsonArray if preferred
    }

    // Initialize a list to hold the DbFsCodes objects
    val dbFsCodesList = mutableListOf<DbFsCodes>()

    // If airlines is not null, extract fs and iata values
    if (airlines != null) {
        for (airline in airlines) {
            // Each airline is a JsonObject
            val airlineObject = airline.jsonObject
            val fs = airlineObject["fs"]?.jsonPrimitive?.content?.replace("*", "") // Get fs
            val iata = airlineObject["iata"]?.jsonPrimitive?.content?.replace("*", "") // Get iata

            // Create DbFsCodes object and add it to the list
            if (fs != null && iata != null) {
                dbFsCodesList.add(DbFsCodes(fs, iata))
            }
        }
    }

    return dbFsCodesList // Return the list of DbFsCodes objects
}