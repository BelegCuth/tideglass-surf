package com.tideglass.surf.provider.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.tideglass.surf.provider.MainActivity
import com.tideglass.surf.provider.R
import com.tideglass.surf.provider.data.MarineRepository
import com.tideglass.surf.provider.data.MarineSnapshot
import com.tideglass.surf.provider.data.TideEventType
import com.tideglass.surf.provider.data.TideGraphPoint
import com.tideglass.surf.provider.data.TideGraphRenderer
import com.tideglass.surf.provider.data.TideTrend
import com.tideglass.surf.provider.data.cardinalDirection
import com.tideglass.surf.provider.data.oneDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

abstract class BaseMarineComplicationService : SuspendingComplicationDataSourceService() {
    protected abstract val previewValue: String
    protected abstract fun previewDescription(): String
    protected abstract fun value(snapshot: MarineSnapshot): String
    protected abstract fun description(snapshot: MarineSnapshot): String

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return NoDataComplicationData()
        val snapshot = runCatching { MarineRepository.snapshot(this) }.getOrNull()
            ?: return NoDataComplicationData()
        return shortText(value(snapshot), description(snapshot))
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        if (type == ComplicationType.SHORT_TEXT) shortText(previewValue, previewDescription())
        else NoDataComplicationData()

    private fun shortText(text: String, contentDescription: String): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build(),
        )
            .setTapAction(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
}

class SpotComplicationService : BaseMarineComplicationService() {
    override val previewValue = "MUNDAKA"
    override fun previewDescription() = getString(R.string.description_spot, "Mundaka")
    override fun value(snapshot: MarineSnapshot) = snapshot.spotName.take(10)
    override fun description(snapshot: MarineSnapshot) = getString(R.string.description_spot, snapshot.spotName)
}

class TideComplicationService : BaseMarineComplicationService() {
    override val previewValue = "79%↑"
    override fun previewDescription() = getString(R.string.description_tide, "79", getString(R.string.trend_rising))
    override fun value(snapshot: MarineSnapshot): String {
        val arrow = when (snapshot.tideTrend) {
            TideTrend.RISING -> "↑"
            TideTrend.FALLING -> "↓"
            TideTrend.STEADY -> "→"
        }
        return "${snapshot.tideLevelPercent}%$arrow"
    }

    override fun description(snapshot: MarineSnapshot): String {
        val trend = when (snapshot.tideTrend) {
            TideTrend.RISING -> R.string.trend_rising
            TideTrend.FALLING -> R.string.trend_falling
            TideTrend.STEADY -> R.string.trend_steady
        }
        return getString(R.string.description_tide, snapshot.tideLevelPercent.toString(), getString(trend))
    }
}

class NextTideComplicationService : BaseMarineComplicationService() {
    override val previewValue = "H20:38"
    override fun previewDescription() = getString(R.string.description_next_tide, getString(R.string.tide_high), "20:38")
    override fun value(snapshot: MarineSnapshot): String {
        val prefix = getString(if (snapshot.nextTideType == TideEventType.HIGH) R.string.tide_high_short else R.string.tide_low_short)
        return snapshot.nextTideEpochMillis?.let { "$prefix${formatTime(it)}" } ?: "--"
    }

    override fun description(snapshot: MarineSnapshot): String {
        val type = when (snapshot.nextTideType) {
            TideEventType.HIGH -> getString(R.string.tide_high)
            TideEventType.LOW -> getString(R.string.tide_low)
            null -> getString(R.string.unknown)
        }
        val time = snapshot.nextTideEpochMillis?.let(::formatTime) ?: getString(R.string.unknown)
        return getString(R.string.description_next_tide, type, time)
    }
}

class SurfComplicationService : BaseMarineComplicationService() {
    override val previewValue = "0.4·6 NW"
    override fun previewDescription() = resources.getQuantityString(R.plurals.description_swell, 6, "0.4", 6, "NW")
    override fun value(snapshot: MarineSnapshot): String =
        "${snapshot.swellHeightMeters.oneDecimal()}·${snapshot.swellPeriodSeconds.roundToInt()} ${cardinalDirection(snapshot.swellDirectionDegrees)}"

    override fun description(snapshot: MarineSnapshot): String {
        val period = snapshot.swellPeriodSeconds.roundToInt()
        return resources.getQuantityString(
            R.plurals.description_swell, period, snapshot.swellHeightMeters.oneDecimal(), period,
            cardinalDirection(snapshot.swellDirectionDegrees),
        )
    }
}

class WindComplicationService : BaseMarineComplicationService() {
    override val previewValue = "8kn NE"
    override fun previewDescription() = resources.getQuantityString(R.plurals.description_wind, 8, 8, "NE")
    override fun value(snapshot: MarineSnapshot): String =
        "${snapshot.windSpeedKnots.roundToInt()}kn ${cardinalDirection(snapshot.windDirectionDegrees)}"

    override fun description(snapshot: MarineSnapshot): String {
        val speed = snapshot.windSpeedKnots.roundToInt()
        return resources.getQuantityString(
            R.plurals.description_wind, speed, speed, cardinalDirection(snapshot.windDirectionDegrees),
        )
    }
}

class TideGraphComplicationService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.SMALL_IMAGE) return NoDataComplicationData()
        val snapshot = runCatching { MarineRepository.snapshot(this) }.getOrNull()
            ?: return NoDataComplicationData()
        if (snapshot.tideSeries.size < 2) return NoDataComplicationData()
        return graphData(snapshot.tideSeries, System.currentTimeMillis())
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        if (type != ComplicationType.SMALL_IMAGE) return NoDataComplicationData()
        val now = System.currentTimeMillis()
        val levels = listOf(22, 35, 57, 82, 96, 88, 65, 39, 18, 8, 19, 43, 70)
        val preview = levels.mapIndexed { index, level ->
            TideGraphPoint(now + (index - 4) * 60L * 60L * 1000L, level)
        }
        return graphData(preview, now)
    }

    private fun graphData(points: List<TideGraphPoint>, nowMillis: Long): ComplicationData {
        val bitmap = TideGraphRenderer.render(points, nowMillis)
        val image = SmallImage.Builder(Icon.createWithBitmap(bitmap), SmallImageType.PHOTO).build()
        return SmallImageComplicationData.Builder(
            smallImage = image,
            contentDescription = PlainComplicationText.Builder(getString(R.string.description_tide_graph)).build(),
        )
            .setTapAction(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
    }
}

object ComplicationUpdater {
    private val providerClasses = listOf(
        SpotComplicationService::class.java,
        TideComplicationService::class.java,
        NextTideComplicationService::class.java,
        SurfComplicationService::class.java,
        WindComplicationService::class.java,
        TideGraphComplicationService::class.java,
    )

    fun requestAll(context: Context) {
        providerClasses.forEach { providerClass ->
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, providerClass),
            ).requestUpdateAll()
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
