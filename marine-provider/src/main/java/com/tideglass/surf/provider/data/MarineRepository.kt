package com.tideglass.surf.provider.data

import android.content.Context
import androidx.core.content.edit
import com.tideglass.surf.provider.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object MarineRepository {
    private const val PREFERENCES = "tideglass_marine"
    private const val KEY_SNAPSHOT = "snapshot"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_SPOT_ID = "spot_id"
    private const val CACHE_MAX_AGE_MILLIS = 30 * 60 * 1000L
    private const val DEFAULT_LATITUDE = 43.4075
    private const val DEFAULT_LONGITUDE = -2.6988
    private val refreshMutex = Mutex()

    fun cached(context: Context): MarineSnapshot? {
        val raw = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null) ?: return null
        return MarineSnapshot.fromJson(raw)
    }

    fun saveLocation(context: Context, latitude: Double, longitude: Double) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_LATITUDE, latitude.toString())
            putString(KEY_LONGITUDE, longitude.toString())
            remove(KEY_SPOT_ID)
        }
    }

    fun saveSpot(context: Context, spotId: String) {
        requireNotNull(SpotCatalog.byId(spotId)) { "Unknown Tideglass spot: $spotId" }
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putString(KEY_SPOT_ID, spotId)
        }
    }

    suspend fun snapshot(context: Context, force: Boolean = false): MarineSnapshot = refreshMutex.withLock {
        val cached = cached(context)
        if (!force && cached != null && System.currentTimeMillis() - cached.updatedAtMillis < CACHE_MAX_AGE_MILLIS) {
            return@withLock cached
        }
        runCatching { fetch(context) }.getOrElse { error -> cached ?: throw error }
    }

    private suspend fun fetch(context: Context): MarineSnapshot = withContext(Dispatchers.IO) {
        val baseUrl = BuildConfig.DATA_BASE_URL.trim().trimEnd('/')
        require(baseUrl.startsWith("https://")) { "Tideglass data URL is not configured" }
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val spot = preferences.getString(KEY_SPOT_ID, null)?.let(SpotCatalog::byId) ?: run {
            val latitude = preferences.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: DEFAULT_LATITUDE
            val longitude = preferences.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: DEFAULT_LONGITUDE
            SpotCatalog.nearest(latitude, longitude)
        }
        val snapshot = MarineSnapshot.fromPublishedJson(request("$baseUrl/v1/spots/${spot.id}.json"))
        require(snapshot.spotId == spot.id) { "Marine-data spot mismatch" }
        preferences.edit { putString(KEY_SNAPSHOT, snapshot.toJson()) }
        snapshot
    }

    private fun request(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "TideglassSurf/${BuildConfig.VERSION_NAME}")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Marine service returned HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
