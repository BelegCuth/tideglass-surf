package com.tideglass.surf.provider.data

import kotlin.math.abs

data class TidePoint(val epochMillis: Long, val heightMeters: Double)

data class TideGraphPoint(val epochMillis: Long, val levelPercent: Int)

enum class TideTrend { RISING, FALLING, STEADY }

enum class TideEventType { HIGH, LOW }

data class TideEvent(
    val type: TideEventType,
    val epochMillis: Long,
    val heightMeters: Double,
)

object TideAnalyzer {
    fun closest(points: List<TidePoint>, nowMillis: Long): TidePoint? =
        points.minByOrNull { abs(it.epochMillis - nowMillis) }

    fun trend(points: List<TidePoint>, nowMillis: Long): TideTrend {
        if (points.size < 2) return TideTrend.STEADY
        val sorted = points.sortedBy { it.epochMillis }
        val currentIndex = sorted.indices.minBy { abs(sorted[it].epochMillis - nowMillis) }
        val previous = sorted[(currentIndex - 1).coerceAtLeast(0)]
        val next = sorted[(currentIndex + 1).coerceAtMost(sorted.lastIndex)]
        val delta = next.heightMeters - previous.heightMeters
        return when {
            delta > 0.015 -> TideTrend.RISING
            delta < -0.015 -> TideTrend.FALLING
            else -> TideTrend.STEADY
        }
    }

    fun nextEvent(points: List<TidePoint>, nowMillis: Long): TideEvent? {
        val sorted = points.sortedBy { it.epochMillis }
        for (index in 1 until sorted.lastIndex) {
            val current = sorted[index]
            if (current.epochMillis <= nowMillis) continue
            val previous = sorted[index - 1].heightMeters
            val next = sorted[index + 1].heightMeters
            if (current.heightMeters > previous && current.heightMeters >= next) {
                return TideEvent(TideEventType.HIGH, current.epochMillis, current.heightMeters)
            }
            if (current.heightMeters < previous && current.heightMeters <= next) {
                return TideEvent(TideEventType.LOW, current.epochMillis, current.heightMeters)
            }
        }
        return null
    }
}
