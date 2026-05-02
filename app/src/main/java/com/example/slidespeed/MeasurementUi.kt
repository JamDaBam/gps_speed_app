package com.example.slidespeed

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.slidespeed.location.GpsStatusSnapshot
import com.example.slidespeed.measurement.MeasurementSnapshot
import com.example.slidespeed.measurement.SessionStatus
import java.util.Locale
import kotlin.math.roundToInt

data class MeasurementUiState(
    val currentSpeedLabel: UiText,
    val distanceValueLabel: UiText,
    val averageSpeedValueLabel: UiText,
    val maxSpeedValueLabel: UiText,
    val statusLabel: UiText,
    val statusBarAccuracyLabel: UiText,
    val currentAccuracyMeters: Int?,
    val isRunning: Boolean,
)

fun MeasurementSnapshot.toUiState(): MeasurementUiState = MeasurementUiState(
    currentSpeedLabel = currentSpeedKmh.toSpeedLabel(),
    distanceValueLabel = totalDistanceMeters.toDistanceValueLabel(),
    averageSpeedValueLabel = averageSpeedKmh.toSpeedValueLabel(),
    maxSpeedValueLabel = maxSpeedKmh.toSpeedValueLabel(),
    statusLabel = when (sessionStatus) {
        SessionStatus.Ready -> UiText.Resource(R.string.session_ready)
        SessionStatus.Running -> UiText.Resource(R.string.session_running)
        SessionStatus.Stopped -> UiText.Resource(R.string.session_stopped)
    },
    statusBarAccuracyLabel = currentAccuracyMeters?.let { R.string.status_accuracy_value.toText(it) }
        ?: UiText.Resource(R.string.status_accuracy_unavailable),
    currentAccuracyMeters = currentAccuracyMeters,
    isRunning = isRunning,
)

sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@param:StringRes val resId: Int) : UiText
    data class Formatted(@param:StringRes val resId: Int, val args: List<Any>) : UiText
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId)
    is UiText.Formatted -> stringResource(resId, *args.toTypedArray())
}

fun Int.toSpeedLabel(): UiText = UiText.Dynamic("$this km/h")

fun Int.toSpeedValueLabel(): UiText = UiText.Dynamic("$this km/h")

fun GpsStatusSnapshot.toSatellitesLabel(): UiText = when {
    usedSatellites != null && visibleSatellites != null ->
        R.string.gps_satellites_used_visible.toText(usedSatellites, visibleSatellites)
    visibleSatellites != null ->
        R.string.gps_satellites_visible_only.toText(visibleSatellites)
    else -> UiText.Resource(R.string.gps_satellites_unavailable)
}

fun GpsStatusSnapshot.toQualityLabel(currentAccuracyMeters: Int?): UiText {
    val qualityResId = when {
        !hasFix -> R.string.gps_quality_no_fix
        currentAccuracyMeters != null && currentAccuracyMeters <= 10 && (usedSatellites ?: 0) >= 4 ->
            R.string.gps_quality_good
        currentAccuracyMeters != null && currentAccuracyMeters <= 20 && (usedSatellites ?: 0) >= 3 ->
            R.string.gps_quality_medium
        usedSatellites != null && usedSatellites >= 4 -> R.string.gps_quality_medium
        else -> R.string.gps_quality_poor
    }
    return UiText.Resource(qualityResId)
}

fun Float.toDistanceValueLabel(): UiText {
    val roundedMeters = roundToInt().coerceAtLeast(0)
    return if (roundedMeters >= 1000) {
        UiText.Dynamic(
            "${String.format(Locale.GERMANY, "%.2f", roundedMeters / METERS_PER_KILOMETER)} km",
        )
    } else {
        UiText.Dynamic("$roundedMeters m")
    }
}

fun Int.toText(vararg args: Any): UiText = UiText.Formatted(this, args.toList())

private const val METERS_PER_KILOMETER = 1_000f
