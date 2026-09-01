package com.tideglass.surf.provider.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.Locale

data class MarineSnapshot(
    val spotId: String,
    val spotName: String,
    val latitude: Double,
    val longitude: Double,
    val tideHeightMeters: Double,
    val tideTrend: TideTrend,
    val nextTideType: TideEventType?,
    val nextTideEpochMillis: Long?,
    val swellHeightMeters: Double,
    val swellPeriodSeconds: Double,
    val swellDirectionDegrees: Double,
    val windSpeedKnots: Double,
    val windDirectionDegrees: Double,
    val waterTemperatureCelsius: Double?,
    val validAtMillis: Long,
    val updatedAtMillis: Long,
    val attributions: List<String>,
) {
    fun toJson(): String = JSONObject()
        .put("spotId", spotId).put("spot", spotName).put("lat", latitude).put("lon", longitude)
        .put("tideHeight", tideHeightMeters).put("tideTrend", tideTrend.name)
        .put("nextTideType", nextTideType?.name).put("nextTideAt", nextTideEpochMillis)
        .put("swellHeight", swellHeightMeters).put("swellPeriod", swellPeriodSeconds)
        .put("swellDirection", swellDirectionDegrees).put("windSpeed", windSpeedKnots)
        .put("windDirection", windDirectionDegrees).put("waterTemperature", waterTemperatureCelsius)
        .put("validAt", validAtMillis).put("updatedAt", updatedAtMillis)
        .put("attributions", JSONArray(attributions)).toString()

    companion object {
        fun fromJson(raw: String): MarineSnapshot? = runCatching {
            val json = JSONObject(raw)
            MarineSnapshot(
                spotId = json.optString("spotId", "legacy"),
                spotName = json.getString("spot"), latitude = json.getDouble("lat"), longitude = json.getDouble("lon"),
                tideHeightMeters = json.getDouble("tideHeight"), tideTrend = TideTrend.valueOf(json.getString("tideTrend")),
                nextTideType = json.nullableEnum("nextTideType", TideEventType::valueOf),
                nextTideEpochMillis = json.optLong("nextTideAt").takeIf { it > 0 },
                swellHeightMeters = json.getDouble("swellHeight"), swellPeriodSeconds = json.getDouble("swellPeriod"),
                swellDirectionDegrees = json.getDouble("swellDirection"), windSpeedKnots = json.getDouble("windSpeed"),
                windDirectionDegrees = json.getDouble("windDirection"),
                waterTemperatureCelsius = json.nullableDouble("waterTemperature"),
                validAtMillis = json.optLong("validAt", json.getLong("updatedAt")), updatedAtMillis = json.getLong("updatedAt"),
                attributions = json.optJSONArray("attributions")?.strings().orEmpty(),
            )
        }.getOrNull()

        fun fromPublishedJson(raw: String): MarineSnapshot = runCatching {
            val root = JSONObject(raw)
            require(root.getInt("schemaVersion") == 1) { "Unsupported marine-data schema" }
            val spot = root.getJSONObject("spot")
            val tide = root.getJSONObject("tide")
            val swell = root.getJSONObject("swell")
            val wind = root.getJSONObject("wind")
            val next = tide.optJSONObject("next")
            MarineSnapshot(
                spotId = spot.getString("id"), spotName = spot.getString("name"),
                latitude = spot.getDouble("latitude"), longitude = spot.getDouble("longitude"),
                tideHeightMeters = tide.getDouble("heightMeters"), tideTrend = TideTrend.valueOf(tide.getString("trend")),
                nextTideType = next?.getString("type")?.let(TideEventType::valueOf),
                nextTideEpochMillis = next?.getString("at")?.let { Instant.parse(it).toEpochMilli() },
                swellHeightMeters = swell.getDouble("heightMeters"), swellPeriodSeconds = swell.getDouble("periodSeconds"),
                swellDirectionDegrees = swell.getDouble("directionDegrees"), windSpeedKnots = wind.getDouble("speedKnots"),
                windDirectionDegrees = wind.getDouble("directionDegrees"),
                waterTemperatureCelsius = root.nullableDouble("waterTemperatureCelsius"),
                validAtMillis = Instant.parse(root.getString("validAt")).toEpochMilli(),
                updatedAtMillis = Instant.parse(root.getString("generatedAt")).toEpochMilli(),
                attributions = root.optJSONArray("attribution")?.strings().orEmpty(),
            )
        }.getOrElse { throw IllegalArgumentException("Invalid published marine data", it) }
    }
}

private fun JSONObject.nullableDouble(key: String): Double? =
    if (!has(key) || isNull(key)) null else getDouble(key)

private fun <T> JSONObject.nullableEnum(key: String, parser: (String) -> T): T? =
    if (!has(key) || isNull(key) || optString(key).isBlank()) null else parser(getString(key))

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

fun Double.oneDecimal(): String = String.format(Locale.US, "%.1f", this)

fun cardinalDirection(degrees: Double): String {
    val labels = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val normalized = ((degrees % 360) + 360) % 360
    return labels[((normalized + 22.5) / 45.0).toInt() % labels.size]
}
