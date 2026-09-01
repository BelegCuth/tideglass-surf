package com.tideglass.surf.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.tideglass.surf.provider.complications.ComplicationUpdater
import com.tideglass.surf.provider.data.MarineRepository
import com.tideglass.surf.provider.data.MarineSnapshot
import com.tideglass.surf.provider.data.TideEventType
import com.tideglass.surf.provider.data.TideTrend
import com.tideglass.surf.provider.data.cardinalDirection
import com.tideglass.surf.provider.data.oneDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private val Ink = Color(0xFF071011)
private val Panel = Color(0xFF101C1D)
private val Foam = Color(0xFF77DDD6)
private val Coral = Color(0xFFFF786D)
private val Sand = Color(0xFFF0EEE6)
private val Muted = Color(0xFF829394)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TideglassApp() }
    }
}

private enum class LoadState { LOADING, READY, ERROR }

@Composable
private fun TideglassApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(MarineRepository.cached(context)) }
    var loadState by remember { mutableStateOf(if (snapshot == null) LoadState.LOADING else LoadState.READY) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh(force: Boolean) {
        scope.launch {
            loadState = LoadState.LOADING
            runCatching { MarineRepository.snapshot(context, force) }
                .onSuccess {
                    snapshot = it
                    loadState = LoadState.READY
                    ComplicationUpdater.requestAll(context)
                }
                .onFailure {
                    loadState = LoadState.ERROR
                    message = context.getString(R.string.status_error)
                }
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch {
                val location = currentLocation(context)
                if (location == null) {
                    message = context.getString(R.string.location_failed)
                } else {
                    MarineRepository.saveLocation(context, location.latitude, location.longitude)
                    message = context.getString(R.string.location_saved)
                    refresh(force = true)
                }
            }
        } else {
            message = context.getString(R.string.permission_denied)
        }
    }

    LaunchedEffect(Unit) { refresh(force = false) }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.screen_title),
                color = Sand,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.2.sp,
            )
            Text(
                text = context.getString(R.string.screen_subtitle).uppercase(),
                color = Foam,
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(10.dp))
            StatusPill(loadState)
            Spacer(Modifier.height(14.dp))

            if (snapshot == null) {
                EmptyPanel(loadState)
            } else {
                SnapshotPanel(snapshot!!)
            }

            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (loadState == LoadState.ERROR) Coral else Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))
            ActionButton(
                text = context.getString(R.string.action_location),
                prominent = true,
            ) {
                val fineGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
                if (fineGranted) {
                    scope.launch {
                        val location = currentLocation(context)
                        if (location != null) {
                            MarineRepository.saveLocation(context, location.latitude, location.longitude)
                            refresh(force = true)
                        } else message = context.getString(R.string.location_failed)
                    }
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            ActionButton(
                text = context.getString(if (loadState == LoadState.ERROR) R.string.action_retry else R.string.action_refresh),
                prominent = false,
            ) { refresh(force = true) }
            Spacer(Modifier.height(10.dp))
            Text(
                text = context.getString(R.string.accuracy_notice).uppercase(),
                color = Muted,
                fontSize = 8.sp,
                letterSpacing = .7.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusPill(state: LoadState) {
    val context = LocalContext.current
    val (label, color) = when (state) {
        LoadState.LOADING -> context.getString(R.string.status_loading) to Foam
        LoadState.READY -> context.getString(R.string.status_ready) to Foam
        LoadState.ERROR -> context.getString(R.string.status_error) to Coral
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(label, color = color, fontSize = 9.sp, letterSpacing = 1.1.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun EmptyPanel(state: LoadState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF1D3031), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (state == LoadState.ERROR) "— ${stringResource(R.string.offline)} —" else "≈  ≈  ≈",
            color = if (state == LoadState.ERROR) Coral else Foam,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun SnapshotPanel(data: MarineSnapshot) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, Color(0xFF1D3031), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(data.spotName.uppercase(), color = Sand, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Text("${data.latitude.oneDecimal()}° · ${data.longitude.oneDecimal()}°", color = Muted, fontSize = 8.sp)
        Spacer(Modifier.height(11.dp))
        val trend = when (data.tideTrend) {
            TideTrend.RISING -> "↑"
            TideTrend.FALLING -> "↓"
            TideTrend.STEADY -> "→"
        }
        MetricRow(stringResource(R.string.metric_tide), "${data.tideHeightMeters.oneDecimal()} m $trend", Foam)
        MetricRow(stringResource(R.string.metric_next), nextTide(data), Sand)
        MetricRow(
            stringResource(R.string.metric_surf),
            "${data.swellHeightMeters.oneDecimal()} m · ${data.swellPeriodSeconds.roundToInt()} s ${cardinalDirection(data.swellDirectionDegrees)}",
            Sand,
        )
        MetricRow(stringResource(R.string.metric_wind), "${data.windSpeedKnots.roundToInt()} kn ${cardinalDirection(data.windDirectionDegrees)}", Sand)
        MetricRow(stringResource(R.string.metric_water), data.waterTemperatureCelsius?.let { "${it.oneDecimal()}°C" } ?: "—", Sand)
        Spacer(Modifier.height(7.dp))
        Text(
            LocalContext.current.getString(R.string.last_updated, formatDateTime(data.updatedAtMillis)),
            color = Muted,
            fontSize = 8.sp,
        )
        Text(stringResource(R.string.attribution_short), color = Muted, fontSize = 7.sp)
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Coral, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(
            value,
            color = valueColor,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun ActionButton(text: String, prominent: Boolean, onClick: () -> Unit) {
    val background = if (prominent) Foam else Color.Transparent
    val foreground = if (prominent) Ink else Foam
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(21.dp))
            .background(background)
            .border(1.dp, Foam, RoundedCornerShape(21.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), color = foreground, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
    }
}

@Composable
private fun nextTide(data: MarineSnapshot): String {
    val event = when (data.nextTideType) {
        TideEventType.HIGH -> stringResource(R.string.tide_high)
        TideEventType.LOW -> stringResource(R.string.tide_low)
        null -> "—"
    }
    val time = data.nextTideEpochMillis?.let(::formatTime) ?: "—"
    return "$event $time"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm")

private fun formatTime(epochMillis: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private fun formatDateTime(epochMillis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private suspend fun currentLocation(context: Context): Location? = withContext(Dispatchers.Main) {
    val manager = context.getSystemService(LocationManager::class.java)
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return@withContext null
    }
    if (
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) return@withContext null

    suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
            if (continuation.isActive) continuation.resume(location)
        }
    }
}
