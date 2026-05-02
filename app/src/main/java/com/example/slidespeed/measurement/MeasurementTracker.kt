package com.example.slidespeed.measurement

import com.example.slidespeed.location.LocationReading
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class MeasurementSnapshot(
    val currentSpeedKmh: Int = 0,
    val currentAccuracyMeters: Int? = null,
    val sessionStatus: SessionStatus = SessionStatus.Ready,
    val isRunning: Boolean = false,
    val totalDistanceMeters: Float = 0f,
    val averageSpeedKmh: Int = 0,
    val maxSpeedKmh: Int = 0,
    val sessionStartTimeMillis: Long? = null,
    val accumulatedPausedDurationMillis: Long = 0L,
    val pauseStartedAtMillis: Long? = null,
    val lastAcceptedReading: LocationReading? = null,
    val lastSessionReading: LocationReading? = null,
)

class MeasurementTracker(
    private val validator: LocationReadingValidator = LocationReadingValidator(),
    private val timeMillisProvider: () -> Long = System::currentTimeMillis,
    initialSnapshot: MeasurementSnapshot = MeasurementSnapshot(),
) {
    var snapshot: MeasurementSnapshot = initialSnapshot
        private set

    fun startSession() {
        snapshot = when (snapshot.sessionStatus) {
            SessionStatus.Ready -> MeasurementSnapshot(
                currentSpeedKmh = snapshot.currentSpeedKmh,
                currentAccuracyMeters = snapshot.currentAccuracyMeters,
                sessionStatus = SessionStatus.Running,
                isRunning = true,
                lastAcceptedReading = snapshot.lastAcceptedReading,
            )
            SessionStatus.Stopped -> snapshot.copy(
                sessionStatus = SessionStatus.Running,
                isRunning = true,
                accumulatedPausedDurationMillis = snapshot.accumulatedPausedDurationMillis +
                    pausedDurationMillis(),
                pauseStartedAtMillis = null,
                lastSessionReading = null,
            )
            SessionStatus.Running -> snapshot
        }
    }

    fun stopSession() {
        if (!snapshot.isRunning) {
            return
        }
        snapshot = snapshot.copy(
            sessionStatus = SessionStatus.Stopped,
            isRunning = false,
            pauseStartedAtMillis = timeMillisProvider(),
            lastSessionReading = null,
        )
    }

    fun resetSession() {
        snapshot = snapshot.copy(
            sessionStatus = SessionStatus.Ready,
            isRunning = false,
            totalDistanceMeters = 0f,
            averageSpeedKmh = 0,
            maxSpeedKmh = 0,
            sessionStartTimeMillis = null,
            accumulatedPausedDurationMillis = 0L,
            pauseStartedAtMillis = null,
            lastSessionReading = null,
        )
    }

    fun consume(reading: LocationReading) {
        if (!validator.shouldUseForLiveDisplay(reading, snapshot.lastAcceptedReading)) {
            return
        }

        val roundedAccuracy = reading.accuracyMeters
            ?.coerceAtLeast(0f)
            ?.roundToInt()
        snapshot = snapshot.copy(
            currentSpeedKmh = reading.speedKmh.roundToInt().coerceAtLeast(0),
            currentAccuracyMeters = roundedAccuracy,
            lastAcceptedReading = reading,
        )

        if (!snapshot.isRunning) {
            return
        }

        if (!validator.shouldUseForSession(reading, snapshot.lastSessionReading)) {
            return
        }

        val nextStartTimeMillis = snapshot.sessionStartTimeMillis ?: reading.timestampMillis
        val distanceIncrementMeters = snapshot.lastSessionReading
            ?.let { previous -> distanceMetersBetween(previous, reading) }
            ?: 0f
        val nextDistanceMeters = snapshot.totalDistanceMeters + distanceIncrementMeters
        val nextMaxSpeedKmh = max(snapshot.maxSpeedKmh.toFloat(), reading.speedKmh.coerceAtLeast(0f))
        val durationMillis = (
            reading.timestampMillis -
                nextStartTimeMillis -
                snapshot.accumulatedPausedDurationMillis
            ).coerceAtLeast(0L)
        val averageSpeedKmh = if (durationMillis > 0L) {
            nextDistanceMeters / METERS_PER_KILOMETER / (durationMillis / MILLIS_PER_HOUR)
        } else {
            0f
        }

        snapshot = snapshot.copy(
            totalDistanceMeters = nextDistanceMeters,
            averageSpeedKmh = averageSpeedKmh.roundToInt().coerceAtLeast(0),
            maxSpeedKmh = nextMaxSpeedKmh.roundToInt().coerceAtLeast(0),
            sessionStartTimeMillis = nextStartTimeMillis,
            lastSessionReading = reading,
        )
    }

    private fun pausedDurationMillis(): Long {
        val pauseStartedAtMillis = snapshot.pauseStartedAtMillis ?: return 0L
        return (timeMillisProvider() - pauseStartedAtMillis).coerceAtLeast(0L)
    }
}

enum class SessionStatus {
    Ready,
    Running,
    Stopped,
}

class LocationReadingValidator(
    private val nowMillisProvider: () -> Long = System::currentTimeMillis,
) {
    fun shouldUseForLiveDisplay(
        reading: LocationReading,
        previousReading: LocationReading?,
    ): Boolean {
        if (!reading.hasValidCoordinates()) {
            return false
        }
        if (reading.timestampMillis <= 0L) {
            return false
        }
        if (reading.isMock) {
            return false
        }
        if (nowMillisProvider() - reading.timestampMillis > MAX_SAMPLE_AGE_MILLIS) {
            return false
        }
        if (previousReading != null && reading.timestampMillis <= previousReading.timestampMillis) {
            return false
        }
        return true
    }

    fun shouldUseForSession(
        reading: LocationReading,
        previousSessionReading: LocationReading?,
    ): Boolean {
        val accuracyMeters = reading.accuracyMeters ?: return false
        if (accuracyMeters > MAX_DISTANCE_ACCURACY_METERS) {
            return false
        }
        if (previousSessionReading == null) {
            return true
        }

        val elapsedMillis = reading.timestampMillis - previousSessionReading.timestampMillis
        if (elapsedMillis <= 0L) {
            return false
        }

        val distanceMeters = distanceMetersBetween(previousSessionReading, reading)
        val segmentSpeedKmh = distanceMeters / METERS_PER_KILOMETER / (elapsedMillis / MILLIS_PER_HOUR)
        return segmentSpeedKmh <= MAX_SEGMENT_SPEED_KMH
    }
}

private fun LocationReading.hasValidCoordinates(): Boolean {
    return latitude in MIN_LATITUDE..MAX_LATITUDE && longitude in MIN_LONGITUDE..MAX_LONGITUDE
}

internal fun distanceMetersBetween(start: LocationReading, end: LocationReading): Float {
    val lat1 = start.latitude.toRadians()
    val lat2 = end.latitude.toRadians()
    val deltaLat = (end.latitude - start.latitude).toRadians()
    val deltaLon = (end.longitude - start.longitude).toRadians()
    val haversine = sin(deltaLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
    val centralAngle = 2 * asin(min(1.0, sqrt(haversine)))
    return (EARTH_RADIUS_METERS * centralAngle).toFloat()
}

private fun Double.toRadians(): Double = this / 180.0 * PI

private const val METERS_PER_KILOMETER = 1_000f
private const val MILLIS_PER_HOUR = 3_600_000f
private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0
private const val MAX_SAMPLE_AGE_MILLIS = 15_000L
private const val MAX_DISTANCE_ACCURACY_METERS = 25f
private const val MAX_SEGMENT_SPEED_KMH = 80f
