package com.tideglass.surf.provider.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarineSnapshotTest {
    @Test
    fun parsesPublishedSchemaVersionOne() {
        val snapshot = MarineSnapshot.fromPublishedJson(
            """{
              "schemaVersion":1,
              "spot":{"id":"mundaka","name":"MUNDAKA","latitude":43.4075,"longitude":-2.6988},
              "generatedAt":"2026-09-01T12:00:00Z","validAt":"2026-09-01T12:00:00Z",
              "tide":{"heightMeters":1.4,"trend":"RISING","next":{"type":"HIGH","at":"2026-09-01T14:00:00Z","heightMeters":2.1}},
              "swell":{"heightMeters":1.6,"periodSeconds":12.0,"directionDegrees":310.0},
              "wind":{"speedKnots":8.0,"directionDegrees":90.0},
              "waterTemperatureCelsius":null,
              "attribution":["EOT20 / CC BY 4.0","Copernicus Marine Service","MET Norway"]
            }""".trimIndent(),
        )
        assertEquals("mundaka", snapshot.spotId)
        assertEquals(TideTrend.RISING, snapshot.tideTrend)
        assertEquals(TideEventType.HIGH, snapshot.nextTideType)
        assertEquals(12.0, snapshot.swellPeriodSeconds, 0.0)
        assertNull(snapshot.waterTemperatureCelsius)
        assertEquals(3, snapshot.attributions.size)
    }
}
