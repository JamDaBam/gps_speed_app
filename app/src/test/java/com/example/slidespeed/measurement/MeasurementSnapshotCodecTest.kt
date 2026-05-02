package com.example.slidespeed.measurement

import com.example.slidespeed.location.LocationReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeasurementSnapshotCodecTest {
    @Test
    fun roundTripPreservesSnapshotState() {
        val snapshot = MeasurementSnapshot(
            currentSpeedMps = 7.5f,
            currentSpeedKmh = 27,
            currentAccuracyMeters = 4,
            sessionStatus = SessionStatus.Stopped,
            isRunning = false,
            totalDistanceMeters = 1234.5f,
            averageSpeedKmh = 19,
            maxSpeedKmh = 33,
            sessionStartTimeMillis = 1_700_000_000_000L,
            accumulatedPausedDurationMillis = 18_000L,
            pauseStartedAtMillis = 1_700_000_030_000L,
            lastAcceptedReading = reading(
                timestampMillis = 1_700_000_031_000L,
                speedKmh = 27f,
                accuracyMeters = 4f,
            ),
            lastSessionReading = reading(
                latitude = 52.5208,
                longitude = 13.4052,
                timestampMillis = 1_700_000_029_000L,
                speedKmh = 21f,
                accuracyMeters = 6f,
            ),
        )

        val restoredSnapshot = MeasurementSnapshotCodec.decode(
            MeasurementSnapshotCodec.encode(snapshot),
        )

        assertEquals(snapshot, restoredSnapshot)
    }

    @Test
    fun invalidPayloadReturnsNull() {
        assertNull(MeasurementSnapshotCodec.decode("not-a-valid-snapshot"))
    }
}

private fun reading(
    latitude: Double = 52.52,
    longitude: Double = 13.405,
    timestampMillis: Long,
    speedKmh: Float,
    accuracyMeters: Float?,
    isMock: Boolean = false,
): LocationReading {
    return LocationReading(
        speedKmh = speedKmh,
        accuracyMeters = accuracyMeters,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = timestampMillis,
        isMock = isMock,
    )
}
