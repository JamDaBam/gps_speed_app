package com.example.slidespeed.measurement

import com.example.slidespeed.location.LocationReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementTrackerTest {
    @Test
    fun startStopAndResetControlSessionLifecycle() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS })

        tracker.startSession()
        assertTrue(tracker.snapshot.isRunning)

        tracker.consume(reading(timestampMillis = NOW_MILLIS, accuracyMeters = 5f, speedKmh = 8f))
        tracker.stopSession()
        assertEquals(false, tracker.snapshot.isRunning)

        tracker.resetSession()
        assertEquals(0f, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(0, tracker.snapshot.averageSpeedKmh)
        assertEquals(0, tracker.snapshot.maxSpeedKmh)
        assertNull(tracker.snapshot.sessionStartTimeMillis)
        assertNull(tracker.snapshot.lastSessionReading)
    }

    @Test
    fun validSamplesAccumulateDistanceAverageAndMax() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 1_000L })
        tracker.startSession()

        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 10f,
                accuracyMeters = 5f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.5209,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 10_000L,
                speedKmh = 16f,
                accuracyMeters = 5f,
            ),
        )

        assertTrue(tracker.snapshot.totalDistanceMeters > 90f)
        assertEquals(16, tracker.snapshot.maxSpeedKmh)
        assertTrue(tracker.snapshot.averageSpeedKmh > 30)
    }

    @Test
    fun rejectsStaleOutOfOrderAndMockSamplesFromLiveState() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS })

        tracker.consume(reading(timestampMillis = NOW_MILLIS - 20_000L, speedKmh = 12f, accuracyMeters = 5f))
        assertEquals(0, tracker.snapshot.currentSpeedKmh)

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 14f, accuracyMeters = 5f))
        assertEquals(14, tracker.snapshot.currentSpeedKmh)

        tracker.consume(reading(timestampMillis = NOW_MILLIS - 1L, speedKmh = 20f, accuracyMeters = 5f))
        assertEquals(14, tracker.snapshot.currentSpeedKmh)

        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1L, speedKmh = 20f, accuracyMeters = 5f, isMock = true))
        assertEquals(14, tracker.snapshot.currentSpeedKmh)
    }

    @Test
    fun rejectsPoorAccuracyAndImplausibleJumpsFromSessionStats() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 1_000L })
        tracker.startSession()

        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 9f,
                accuracyMeters = 5f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.5203,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 5_000L,
                speedKmh = 50f,
                accuracyMeters = 80f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.54,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 6_000L,
                speedKmh = 50f,
                accuracyMeters = 5f,
            ),
        )

        assertEquals(0f, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(9, tracker.snapshot.maxSpeedKmh)
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

private const val NOW_MILLIS = 1_700_000_000_000L
