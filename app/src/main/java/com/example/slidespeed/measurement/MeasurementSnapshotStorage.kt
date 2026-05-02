package com.example.slidespeed.measurement

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.slidespeed.location.LocationReading

class MeasurementSnapshotStorage(
    private val preferences: SharedPreferences,
) {
    fun load(): MeasurementSnapshot {
        val encodedSnapshot = preferences.getString(MEASUREMENT_SNAPSHOT_KEY, null)
        return MeasurementSnapshotCodec.decode(encodedSnapshot) ?: MeasurementSnapshot()
    }

    fun save(snapshot: MeasurementSnapshot) {
        preferences.edit {
            putString(MEASUREMENT_SNAPSHOT_KEY, MeasurementSnapshotCodec.encode(snapshot))
        }
    }

    fun clear() {
        preferences.edit {
            remove(MEASUREMENT_SNAPSHOT_KEY)
        }
    }
}

internal object MeasurementSnapshotCodec {
    fun encode(snapshot: MeasurementSnapshot): String {
        return listOf(
            FORMAT_VERSION.toString(),
            snapshot.currentSpeedMps.toString(),
            snapshot.currentSpeedKmh.toString(),
            snapshot.currentAccuracyMeters.encodeNullable(),
            snapshot.sessionStatus.name,
            snapshot.isRunning.toString(),
            snapshot.totalDistanceMeters.toString(),
            snapshot.averageSpeedKmh.toString(),
            snapshot.maxSpeedKmh.toString(),
            snapshot.sessionStartTimeMillis.encodeNullable(),
            snapshot.accumulatedPausedDurationMillis.toString(),
            snapshot.pauseStartedAtMillis.encodeNullable(),
            snapshot.lastAcceptedReading.encode(),
            snapshot.lastSessionReading.encode(),
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decode(encodedSnapshot: String?): MeasurementSnapshot? {
        if (encodedSnapshot.isNullOrBlank()) {
            return null
        }

        val fields = encodedSnapshot.split(FIELD_SEPARATOR)
        if (fields.size != SNAPSHOT_FIELD_COUNT || fields[0] != FORMAT_VERSION.toString()) {
            return null
        }

        return runCatching {
            MeasurementSnapshot(
                currentSpeedMps = fields[1].toFloat(),
                currentSpeedKmh = fields[2].toInt(),
                currentAccuracyMeters = fields[3].decodeNullableInt(),
                sessionStatus = SessionStatus.valueOf(fields[4]),
                isRunning = fields[5].toBooleanStrict(),
                totalDistanceMeters = fields[6].toFloat(),
                averageSpeedKmh = fields[7].toInt(),
                maxSpeedKmh = fields[8].toInt(),
                sessionStartTimeMillis = fields[9].decodeNullableLong(),
                accumulatedPausedDurationMillis = fields[10].toLong(),
                pauseStartedAtMillis = fields[11].decodeNullableLong(),
                lastAcceptedReading = fields[12].decodeLocationReading(),
                lastSessionReading = fields[13].decodeLocationReading(),
            )
        }.getOrNull()
    }
}

private fun Int?.encodeNullable(): String = this?.toString().orEmpty()

private fun Long?.encodeNullable(): String = this?.toString().orEmpty()

private fun String.decodeNullableInt(): Int? = takeIf { it.isNotEmpty() }?.toInt()

private fun String.decodeNullableLong(): Long? = takeIf { it.isNotEmpty() }?.toLong()

private fun LocationReading?.encode(): String {
    return this?.let { reading ->
        listOf(
            reading.speedKmh.toString(),
            reading.accuracyMeters?.toString().orEmpty(),
            reading.latitude.toString(),
            reading.longitude.toString(),
            reading.timestampMillis.toString(),
            reading.isMock.toString(),
        ).joinToString(READING_FIELD_SEPARATOR)
    }.orEmpty()
}

private fun String.decodeLocationReading(): LocationReading? {
    if (isEmpty()) {
        return null
    }

    val fields = split(READING_FIELD_SEPARATOR)
    if (fields.size != LOCATION_READING_FIELD_COUNT) {
        return null
    }

    return LocationReading(
        speedKmh = fields[0].toFloat(),
        accuracyMeters = fields[1].takeIf { it.isNotEmpty() }?.toFloat(),
        latitude = fields[2].toDouble(),
        longitude = fields[3].toDouble(),
        timestampMillis = fields[4].toLong(),
        isMock = fields[5].toBooleanStrict(),
    )
}

private const val MEASUREMENT_SNAPSHOT_KEY = "measurement_snapshot"
private const val FORMAT_VERSION = 1
private const val SNAPSHOT_FIELD_COUNT = 14
private const val LOCATION_READING_FIELD_COUNT = 6
private const val FIELD_SEPARATOR = "|"
private const val READING_FIELD_SEPARATOR = ","
