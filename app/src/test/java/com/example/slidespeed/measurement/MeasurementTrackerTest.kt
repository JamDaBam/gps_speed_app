package com.example.slidespeed.measurement

import com.example.slidespeed.location.LocationReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementTrackerTest {
    @Test
    fun startStopResumeAndResetControlSessionLifecycle() {
        var currentTimeMillis = NOW_MILLIS
        val tracker = MeasurementTracker(
            validator = LocationReadingValidator { currentTimeMillis + 1_000L },
            timeMillisProvider = { currentTimeMillis },
        )

        tracker.startSession()
        assertEquals(SessionStatus.Running, tracker.snapshot.sessionStatus)
        assertTrue(tracker.snapshot.isRunning)

        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                accuracyMeters = 5f,
                speedKmh = 8f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.5205,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 5_000L,
                accuracyMeters = 5f,
                speedKmh = 18f,
            ),
        )

        val pausedDistanceMeters = tracker.snapshot.totalDistanceMeters
        val pausedAverageSpeedKmh = tracker.snapshot.averageSpeedKmh
        val pausedMaxSpeedKmh = tracker.snapshot.maxSpeedKmh

        currentTimeMillis = NOW_MILLIS + 6_000L
        tracker.stopSession()
        assertEquals(SessionStatus.Stopped, tracker.snapshot.sessionStatus)
        assertEquals(false, tracker.snapshot.isRunning)
        assertTrue(tracker.snapshot.pauseStartedAtMillis != null)

        currentTimeMillis = NOW_MILLIS + 26_000L
        tracker.startSession()
        assertEquals(SessionStatus.Running, tracker.snapshot.sessionStatus)
        assertTrue(tracker.snapshot.isRunning)
        assertEquals(pausedDistanceMeters, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(pausedAverageSpeedKmh, tracker.snapshot.averageSpeedKmh)
        assertEquals(pausedMaxSpeedKmh, tracker.snapshot.maxSpeedKmh)
        assertNull(tracker.snapshot.pauseStartedAtMillis)

        tracker.resetSession()
        assertEquals(SessionStatus.Ready, tracker.snapshot.sessionStatus)
        assertEquals(0f, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(0, tracker.snapshot.averageSpeedKmh)
        assertEquals(0, tracker.snapshot.maxSpeedKmh)
        assertNull(tracker.snapshot.sessionStartTimeMillis)
        assertEquals(0L, tracker.snapshot.accumulatedPausedDurationMillis)
        assertNull(tracker.snapshot.pauseStartedAtMillis)
        assertNull(tracker.snapshot.lastSessionReading)
    }

    @Test
    fun validSamplesAccumulateDistanceAverageAndMax() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 11_000L })
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
    fun resumeKeepsStatsAndExcludesPausedTimeFromAverage() {
        var currentTimeMillis = NOW_MILLIS
        val tracker = MeasurementTracker(
            validator = LocationReadingValidator { currentTimeMillis + 1_000L },
            timeMillisProvider = { currentTimeMillis },
        )
        tracker.startSession()

        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 12f,
                accuracyMeters = 5f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.52045,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 5_000L,
                speedKmh = 20f,
                accuracyMeters = 5f,
            ),
        )

        val distanceBeforePause = tracker.snapshot.totalDistanceMeters
        val maxBeforePause = tracker.snapshot.maxSpeedKmh

        currentTimeMillis = NOW_MILLIS + 6_000L
        tracker.stopSession()
        currentTimeMillis = NOW_MILLIS + 26_000L
        tracker.startSession()

        tracker.consume(
            reading(
                latitude = 52.5209,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 31_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )
        tracker.consume(
            reading(
                latitude = 52.52135,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 36_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )

        assertTrue(tracker.snapshot.totalDistanceMeters > distanceBeforePause)
        assertTrue(tracker.snapshot.averageSpeedKmh > 20)
        assertEquals(24, tracker.snapshot.maxSpeedKmh)
        assertEquals(maxBeforePause, 20)
        assertEquals(20_000L, tracker.snapshot.accumulatedPausedDurationMillis)
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
