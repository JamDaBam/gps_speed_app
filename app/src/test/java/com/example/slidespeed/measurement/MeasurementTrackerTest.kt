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
                latitude = northOf(50.0),
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
    fun validSamplesAccumulateDistanceAverageAndMaxFromFilteredSpeed() {
        var currentTimeMillis = NOW_MILLIS
        val tracker = MeasurementTracker(validator = LocationReadingValidator { currentTimeMillis })
        tracker.startSession()

        currentTimeMillis = NOW_MILLIS + 1_000L
        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 10f,
                accuracyMeters = 5f,
            ),
        )
        currentTimeMillis = NOW_MILLIS + 11_000L
        tracker.consume(
            reading(
                latitude = northOf(100.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 10_000L,
                speedKmh = 16f,
                accuracyMeters = 5f,
            ),
        )
        currentTimeMillis = NOW_MILLIS + 21_000L
        tracker.consume(
            reading(
                latitude = northOf(200.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 20_000L,
                speedKmh = 14f,
                accuracyMeters = 5f,
            ),
        )

        assertTrue(tracker.snapshot.totalDistanceMeters > 190f)
        assertEquals(13, tracker.snapshot.maxSpeedKmh)
        assertTrue(tracker.snapshot.averageSpeedKmh > 30)
    }

    @Test
    fun subThresholdSpeedsRemainZero() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 2_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 1.6f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 1.9f, accuracyMeters = 5f))

        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(0f, tracker.snapshot.currentSpeedMps, 0.001f)
    }

    @Test
    fun poorLiveAccuracyForcesZeroDisplay() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 3_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 8f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 10f, accuracyMeters = 5f))
        assertEquals(9, tracker.snapshot.currentSpeedKmh)

        tracker.consume(reading(timestampMillis = NOW_MILLIS + 2_000L, speedKmh = 12f, accuracyMeters = 30f))

        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(30, tracker.snapshot.currentAccuracyMeters)
    }

    @Test
    fun singleMovementSpikeDoesNotConfirmMotion() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 2_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 12f, accuracyMeters = 5f))

        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(0f, tracker.snapshot.currentSpeedMps, 0.001f)
    }

    @Test
    fun twoConsecutiveQualifyingSamplesConfirmMotion() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 3_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 12f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 18f, accuracyMeters = 5f))

        assertEquals(15, tracker.snapshot.currentSpeedKmh)
        assertEquals(15f / 3.6f, tracker.snapshot.currentSpeedMps, 0.001f)
    }

    @Test
    fun confirmedMovementUsesThreeSampleAverage() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 4_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 12f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 18f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 2_000L, speedKmh = 15f, accuracyMeters = 5f))

        assertEquals(15, tracker.snapshot.currentSpeedKmh)
        assertEquals(15f / 3.6f, tracker.snapshot.currentSpeedMps, 0.001f)
    }

    @Test
    fun stopSampleClearsMovementImmediately() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 4_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 12f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 18f, accuracyMeters = 5f))
        assertEquals(15, tracker.snapshot.currentSpeedKmh)

        tracker.consume(reading(timestampMillis = NOW_MILLIS + 2_000L, speedKmh = 1.5f, accuracyMeters = 5f))

        assertEquals(0, tracker.snapshot.currentSpeedKmh)
    }

    @Test
    fun resumeKeepsStatsAndExcludesPausedTimeFromAverageWhileResettingFilterState() {
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
                latitude = northOf(50.0),
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
                latitude = northOf(100.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 31_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )
        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(distanceBeforePause, tracker.snapshot.totalDistanceMeters, 0.01f)

        tracker.consume(
            reading(
                latitude = northOf(150.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 36_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )

        assertTrue(tracker.snapshot.totalDistanceMeters > distanceBeforePause)
        assertTrue(tracker.snapshot.averageSpeedKmh > 20)
        assertEquals(24, tracker.snapshot.maxSpeedKmh)
        assertEquals(16, maxBeforePause)
        assertEquals(20_000L, tracker.snapshot.accumulatedPausedDurationMillis)
    }

    @Test
    fun rejectsStaleOutOfOrderAndMockSamplesFromLiveState() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 2_000L })

        tracker.consume(reading(timestampMillis = NOW_MILLIS - 20_000L, speedKmh = 12f, accuracyMeters = 5f))
        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(0f, tracker.snapshot.currentSpeedMps, 0.001f)

        tracker.consume(reading(timestampMillis = NOW_MILLIS, speedKmh = 14f, accuracyMeters = 5f))
        tracker.consume(reading(timestampMillis = NOW_MILLIS + 1_000L, speedKmh = 14f, accuracyMeters = 5f))
        assertEquals(14, tracker.snapshot.currentSpeedKmh)
        assertEquals(14f / 3.6f, tracker.snapshot.currentSpeedMps, 0.001f)

        tracker.consume(reading(timestampMillis = NOW_MILLIS - 1L, speedKmh = 20f, accuracyMeters = 5f))
        assertEquals(14, tracker.snapshot.currentSpeedKmh)
        assertEquals(14f / 3.6f, tracker.snapshot.currentSpeedMps, 0.001f)

        tracker.consume(
            reading(
                timestampMillis = NOW_MILLIS + 2_000L,
                speedKmh = 20f,
                accuracyMeters = 5f,
                isMock = true,
            ),
        )
        assertEquals(14, tracker.snapshot.currentSpeedKmh)
        assertEquals(14f / 3.6f, tracker.snapshot.currentSpeedMps, 0.001f)
    }

    @Test
    fun stationaryDriftDoesNotIncreaseDistance() {
        val tracker = MeasurementTracker(validator = LocationReadingValidator { NOW_MILLIS + 6_000L })
        tracker.startSession()

        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 1.2f,
                accuracyMeters = 5f,
            ),
        )
        tracker.consume(
            reading(
                latitude = northOf(3.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 5_000L,
                speedKmh = 1.5f,
                accuracyMeters = 5f,
            ),
        )

        assertEquals(0f, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(0, tracker.snapshot.averageSpeedKmh)
        assertEquals(0, tracker.snapshot.maxSpeedKmh)
    }

    @Test
    fun averageAndMaxIgnoreFilteredOutSamples() {
        var currentTimeMillis = NOW_MILLIS
        val tracker = MeasurementTracker(validator = LocationReadingValidator { currentTimeMillis })
        tracker.startSession()

        currentTimeMillis = NOW_MILLIS + 1_000L
        tracker.consume(
            reading(
                latitude = 52.52,
                longitude = 13.405,
                timestampMillis = NOW_MILLIS,
                speedKmh = 10f,
                accuracyMeters = 5f,
            ),
        )
        currentTimeMillis = NOW_MILLIS + 6_000L
        tracker.consume(
            reading(
                latitude = northOf(50.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 5_000L,
                speedKmh = 22f,
                accuracyMeters = 5f,
            ),
        )
        val confirmedDistanceMeters = tracker.snapshot.totalDistanceMeters
        val confirmedAverageKmh = tracker.snapshot.averageSpeedKmh
        val confirmedMaxKmh = tracker.snapshot.maxSpeedKmh

        currentTimeMillis = NOW_MILLIS + 11_000L
        tracker.consume(
            reading(
                latitude = northOf(100.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 10_000L,
                speedKmh = 25f,
                accuracyMeters = 40f,
            ),
        )
        currentTimeMillis = NOW_MILLIS + 16_000L
        tracker.consume(
            reading(
                latitude = northOf(150.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 15_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )

        assertEquals(0, tracker.snapshot.currentSpeedKmh)
        assertEquals(confirmedDistanceMeters, tracker.snapshot.totalDistanceMeters, 0.01f)
        assertEquals(confirmedAverageKmh, tracker.snapshot.averageSpeedKmh)
        assertEquals(confirmedMaxKmh, tracker.snapshot.maxSpeedKmh)

        currentTimeMillis = NOW_MILLIS + 21_000L
        tracker.consume(
            reading(
                latitude = northOf(200.0),
                longitude = 13.405,
                timestampMillis = NOW_MILLIS + 20_000L,
                speedKmh = 24f,
                accuracyMeters = 5f,
            ),
        )

        assertTrue(tracker.snapshot.totalDistanceMeters > confirmedDistanceMeters)
        assertEquals(24, tracker.snapshot.maxSpeedKmh)
        assertTrue(tracker.snapshot.averageSpeedKmh > 0)
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

private fun northOf(meters: Double): Double = 52.52 + meters / METERS_PER_LATITUDE_DEGREE

private const val NOW_MILLIS = 1_700_000_000_000L
private const val METERS_PER_LATITUDE_DEGREE = 111_111.0
