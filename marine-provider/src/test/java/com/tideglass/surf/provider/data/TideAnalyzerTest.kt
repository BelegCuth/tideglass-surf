package com.tideglass.surf.provider.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TideAnalyzerTest {
    private val hour = 3_600_000L

    @Test
    fun findsNextHighTideAndRisingTrend() {
        val points = listOf(
            TidePoint(0, 0.2),
            TidePoint(hour, 0.9),
            TidePoint(hour * 2, 1.6),
            TidePoint(hour * 3, 0.8),
            TidePoint(hour * 4, 0.1),
        )

        assertEquals(TideTrend.RISING, TideAnalyzer.trend(points, hour))
        assertEquals(TideEventType.HIGH, TideAnalyzer.nextEvent(points, hour)?.type)
        assertEquals(hour * 2, TideAnalyzer.nextEvent(points, hour)?.epochMillis)
    }

    @Test
    fun findsNextLowTide() {
        val points = listOf(
            TidePoint(0, 1.4),
            TidePoint(hour, 0.8),
            TidePoint(hour * 2, 0.1),
            TidePoint(hour * 3, 0.7),
        )

        assertEquals(TideEventType.LOW, TideAnalyzer.nextEvent(points, hour)?.type)
    }

    @Test
    fun returnsNullWithoutFutureTurningPoint() {
        val points = listOf(TidePoint(0, 0.2), TidePoint(hour, 0.5), TidePoint(hour * 2, 0.8))
        assertNull(TideAnalyzer.nextEvent(points, 0))
    }
}
