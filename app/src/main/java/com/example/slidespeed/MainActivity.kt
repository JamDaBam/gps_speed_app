package com.example.slidespeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.slidespeed.location.FastLocationClient
import com.example.slidespeed.location.GpsStatusMonitor
import com.example.slidespeed.location.GpsStatusSnapshot
import com.example.slidespeed.location.LocationReading
import com.example.slidespeed.ui.theme.SlideSpeedTheme
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SlideSpeedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SlideSpeedApp()
                }
            }
        }
    }
}

@Composable
private fun SlideSpeedApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var screenState by rememberSaveable { mutableStateOf(getScreenState(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        screenState = getScreenState(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                screenState = getScreenState(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (screenState) {
        ScreenState.Ready -> ReadyContent()
        ScreenState.PermissionDenied -> PermissionRequiredContent(
            title = context.getString(R.string.permission_title),
            message = context.getString(R.string.permission_message),
            buttonLabel = context.getString(R.string.permission_cta),
            onAction = {
                context.permissionPreferences().edit()
                    .putBoolean(HAS_REQUESTED_LOCATION_PERMISSION_KEY, true)
                    .apply()
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
        )
        ScreenState.PermissionPermanentlyDenied -> PermissionRequiredContent(
            title = context.getString(R.string.permission_title),
            message = context.getString(R.string.permission_rationale),
            buttonLabel = context.getString(R.string.permission_settings_cta),
            onAction = { context.openAppSettings() },
        )
        ScreenState.GpsDisabled -> PermissionRequiredContent(
            title = context.getString(R.string.gps_disabled_title),
            message = context.getString(R.string.gps_disabled_message),
            buttonLabel = context.getString(R.string.gps_settings_cta),
            onAction = { context.openLocationSettings() },
        )
    }
}

@Composable
private fun ReadyContent() {
    val measurementState = rememberMeasurementState()
    val uiState = measurementState.uiState
    val gpsStatus = measurementState.gpsStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GpsStatusBar(
            gpsStatus = gpsStatus,
            accuracyLabel = uiState.statusBarAccuracyLabel.resolve(),
            qualityLabel = gpsStatus.toQualityLabel(uiState.currentAccuracyMeters).resolve(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.statusLabel.resolve(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = uiState.currentSpeedLabel.resolve(),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = stringResource(R.string.current_speed_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = uiState.statusBarAccuracyLabel.resolve(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                StatsSection(measurementState = uiState)
                Spacer(modifier = Modifier.height(24.dp))
                SessionControls(
                    isRunning = uiState.isRunning,
                    onStart = measurementState::startSession,
                    onStop = measurementState::stopSession,
                    onReset = measurementState::resetSession,
                )
            }
        }
        Text(
            text = stringResource(R.string.measurement_note),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GpsStatusBar(
    gpsStatus: GpsStatusSnapshot,
    accuracyLabel: String,
    qualityLabel: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = gpsStatus.toSatellitesLabel().resolve(),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = qualityLabel,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = accuracyLabel,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StatsSection(measurementState: MeasurementUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.distance_label),
            value = measurementState.distanceValueLabel.resolve(),
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.average_speed_label),
            value = measurementState.averageSpeedValueLabel.resolve(),
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.max_speed_label),
            value = measurementState.maxSpeedValueLabel.resolve(),
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SessionControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onStart,
            enabled = !isRunning,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.start_button))
        }
        Button(
            onClick = onStop,
            enabled = isRunning,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.stop_button))
        }
        Button(
            onClick = onReset,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.reset_button))
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    title: String,
    message: String,
    buttonLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Button(
            onClick = onAction,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(buttonLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SlideSpeedAppPreview() {
    SlideSpeedTheme {
        SlideSpeedApp()
    }
}

private enum class ScreenState {
    Ready,
    PermissionDenied,
    PermissionPermanentlyDenied,
    GpsDisabled,
}

@Composable
private fun rememberMeasurementState(): MeasurementStateHolder {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationClient = remember(context) { FastLocationClient(context) }
    val gpsStatusMonitor = remember(context) { GpsStatusMonitor(context) }
    val measurementState = remember(context) {
        MeasurementStateHolder(context.defaultMeasurementUiState())
    }

    DisposableEffect(lifecycleOwner, locationClient, gpsStatusMonitor) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    locationClient.start(measurementState::consume)
                    gpsStatusMonitor.start(measurementState::updateGpsStatus)
                }
                Lifecycle.Event.ON_STOP -> {
                    locationClient.stop()
                    gpsStatusMonitor.stop()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            locationClient.start(measurementState::consume)
            gpsStatusMonitor.start(measurementState::updateGpsStatus)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationClient.stop()
            gpsStatusMonitor.stop()
        }
    }

    return measurementState
}

private class MeasurementStateHolder(initialState: MeasurementUiState) {
    var uiState by mutableStateOf(initialState)
        private set
    var gpsStatus by mutableStateOf(GpsStatusSnapshot())
        private set

    fun startSession() {
        uiState = uiState.startSession()
    }

    fun stopSession() {
        uiState = uiState.stopSession()
    }

    fun resetSession() {
        uiState = uiState.resetSession()
    }

    fun consume(reading: LocationReading) {
        uiState = uiState.consume(reading)
    }

    fun updateGpsStatus(status: GpsStatusSnapshot) {
        gpsStatus = status
    }
}

/**
 * Derives the prerequisite state for the app before any GPS measurement starts.
 * Step 2 only cares about permission and whether location services are enabled.
 */
private fun getScreenState(context: Context): ScreenState {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasFineLocation) {
        val activity = context as? ComponentActivity
        val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) ?: false
        val hasRequestedPermission = context.permissionPreferences()
            .getBoolean(HAS_REQUESTED_LOCATION_PERMISSION_KEY, false)

        return if (shouldShowRationale) {
            ScreenState.PermissionDenied
        } else if (hasRequestedPermission) {
            ScreenState.PermissionPermanentlyDenied
        } else {
            ScreenState.PermissionDenied
        }
    }

    return if (context.isLocationEnabled()) {
        ScreenState.Ready
    } else {
        ScreenState.GpsDisabled
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    startActivity(intent)
}

private fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

private fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return false
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

private fun Context.permissionPreferences(): SharedPreferences =
    getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)

private data class MeasurementUiState(
    val currentSpeedLabel: UiText,
    val distanceValueLabel: UiText,
    val averageSpeedValueLabel: UiText,
    val maxSpeedValueLabel: UiText,
    val statusLabel: UiText,
    val statusBarAccuracyLabel: UiText,
    val currentAccuracyMeters: Int?,
    val isRunning: Boolean,
    val totalDistanceMeters: Float,
    val maxSpeedKmh: Float,
    val sessionStartTimeMillis: Long?,
    val lastSample: LocationReading?,
) {
    fun startSession(): MeasurementUiState = copy(
        distanceValueLabel = 0f.toDistanceValueLabel(),
        averageSpeedValueLabel = 0.toSpeedValueLabel(),
        maxSpeedValueLabel = 0.toSpeedValueLabel(),
        statusLabel = UiText.Resource(R.string.session_running),
        isRunning = true,
        totalDistanceMeters = 0f,
        maxSpeedKmh = 0f,
        sessionStartTimeMillis = null,
        lastSample = null,
    )

    fun stopSession(): MeasurementUiState = copy(
        statusLabel = UiText.Resource(R.string.session_stopped),
        isRunning = false,
        lastSample = null,
    )

    fun resetSession(): MeasurementUiState = copy(
        distanceValueLabel = 0f.toDistanceValueLabel(),
        averageSpeedValueLabel = 0.toSpeedValueLabel(),
        maxSpeedValueLabel = 0.toSpeedValueLabel(),
        statusLabel = UiText.Resource(R.string.session_ready),
        isRunning = false,
        totalDistanceMeters = 0f,
        maxSpeedKmh = 0f,
        sessionStartTimeMillis = null,
        lastSample = null,
    )

    /**
     * Keeps the live speed visible at all times and only aggregates session metrics while running.
     */
    fun consume(reading: LocationReading): MeasurementUiState {
        val roundedSpeed = reading.speedKmh.roundToInt().coerceAtLeast(0)
        val roundedAccuracy = reading.accuracyMeters?.roundToInt()
        val liveState = copy(
            currentSpeedLabel = roundedSpeed.toSpeedLabel(),
            statusBarAccuracyLabel = roundedAccuracy?.let { R.string.status_accuracy_value.toText(it) }
                ?: UiText.Resource(R.string.status_accuracy_unavailable),
            currentAccuracyMeters = roundedAccuracy,
        )

        if (!isRunning) {
            return liveState
        }

        val nextStartTimeMillis = sessionStartTimeMillis ?: reading.timestampMillis
        val distanceIncrementMeters = lastSample
            ?.takeIf { previous -> isTrustedForDistance(previous) && isTrustedForDistance(reading) }
            ?.distanceTo(reading)
            ?: 0f
        val nextDistanceMeters = totalDistanceMeters + distanceIncrementMeters
        val nextMaxSpeedKmh = maxOf(maxSpeedKmh, reading.speedKmh.coerceAtLeast(0f))
        val durationMillis = (reading.timestampMillis - nextStartTimeMillis).coerceAtLeast(0L)
        val averageSpeedKmh = if (durationMillis > 0L) {
            nextDistanceMeters / METERS_PER_KILOMETER / (durationMillis / MILLIS_PER_HOUR)
        } else {
            0f
        }

        return liveState.copy(
            distanceValueLabel = nextDistanceMeters.toDistanceValueLabel(),
            averageSpeedValueLabel = averageSpeedKmh.roundToInt().coerceAtLeast(0).toSpeedValueLabel(),
            maxSpeedValueLabel = nextMaxSpeedKmh.roundToInt().coerceAtLeast(0).toSpeedValueLabel(),
            statusLabel = UiText.Resource(R.string.session_running),
            totalDistanceMeters = nextDistanceMeters,
            maxSpeedKmh = nextMaxSpeedKmh,
            sessionStartTimeMillis = nextStartTimeMillis,
            lastSample = reading,
        )
    }
}

private sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    data class Resource(@StringRes val resId: Int) : UiText
    data class Formatted(@StringRes val resId: Int, val args: List<Any>) : UiText
}

@Composable
private fun UiText.resolve(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId)
    is UiText.Formatted -> stringResource(resId, *args.toTypedArray())
}

private fun Int.toSpeedLabel(): UiText = UiText.Dynamic("$this km/h")

private fun Int.toSpeedValueLabel(): UiText = UiText.Dynamic("$this km/h")

private fun GpsStatusSnapshot.toSatellitesLabel(): UiText = when {
    usedSatellites != null && visibleSatellites != null ->
        R.string.gps_satellites_used_visible.toText(usedSatellites, visibleSatellites)
    visibleSatellites != null ->
        R.string.gps_satellites_visible_only.toText(visibleSatellites)
    else -> UiText.Resource(R.string.gps_satellites_unavailable)
}

private fun GpsStatusSnapshot.toQualityLabel(currentAccuracyMeters: Int?): UiText {
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

private fun Float.toDistanceValueLabel(): UiText {
    val roundedMeters = roundToInt().coerceAtLeast(0)
    return if (roundedMeters >= 1000) {
        UiText.Dynamic(
            "${String.format(Locale.GERMANY, "%.2f", roundedMeters / METERS_PER_KILOMETER)} km",
        )
    } else {
        UiText.Dynamic("$roundedMeters m")
    }
}

private fun Context.defaultMeasurementUiState(): MeasurementUiState = MeasurementUiState(
    currentSpeedLabel = 0.toSpeedLabel(),
    distanceValueLabel = 0f.toDistanceValueLabel(),
    averageSpeedValueLabel = 0.toSpeedValueLabel(),
    maxSpeedValueLabel = 0.toSpeedValueLabel(),
    statusLabel = UiText.Resource(R.string.session_ready),
    statusBarAccuracyLabel = UiText.Resource(R.string.status_accuracy_unavailable),
    currentAccuracyMeters = null,
    isRunning = false,
    totalDistanceMeters = 0f,
    maxSpeedKmh = 0f,
    sessionStartTimeMillis = null,
    lastSample = null,
)

private fun Int.toText(vararg args: Any): UiText = UiText.Formatted(this, args.toList())

private fun isTrustedForDistance(reading: LocationReading): Boolean {
    val accuracyMeters = reading.accuracyMeters ?: return false
    return accuracyMeters <= MAX_DISTANCE_ACCURACY_METERS
}

private fun LocationReading.distanceTo(other: LocationReading): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        latitude,
        longitude,
        other.latitude,
        other.longitude,
        result,
    )
    return result[0]
}

private const val PERMISSION_PREFS_NAME = "slide_speed_permissions"
private const val HAS_REQUESTED_LOCATION_PERMISSION_KEY = "has_requested_location_permission"
private const val MAX_DISTANCE_ACCURACY_METERS = 25f
private const val METERS_PER_KILOMETER = 1_000f
private const val MILLIS_PER_HOUR = 3_600_000f
