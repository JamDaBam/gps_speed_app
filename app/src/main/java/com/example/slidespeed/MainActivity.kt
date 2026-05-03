package com.example.slidespeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.slidespeed.location.FastLocationClient
import com.example.slidespeed.location.GpsStatusMonitor
import com.example.slidespeed.location.GpsStatusSnapshot
import com.example.slidespeed.measurement.MeasurementSnapshot
import com.example.slidespeed.measurement.MeasurementSnapshotStorage
import com.example.slidespeed.measurement.MeasurementTracker
import com.example.slidespeed.ui.theme.SlideSpeedTheme

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
                context.permissionPreferences().edit {
                    putBoolean(HAS_REQUESTED_LOCATION_PERMISSION_KEY, true)
                }
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
    val measurementStateHolder = rememberMeasurementState()
    val uiState = measurementStateHolder.uiState
    val gpsStatus = measurementStateHolder.gpsStatus
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        LandscapeReadyContent(
            uiState = uiState,
            gpsStatus = gpsStatus,
            onStart = measurementStateHolder::startSession,
            onStop = measurementStateHolder::stopSession,
            onReset = measurementStateHolder::resetSession,
        )
    } else {
        PortraitReadyContent(
            uiState = uiState,
            gpsStatus = gpsStatus,
            onStart = measurementStateHolder::startSession,
            onStop = measurementStateHolder::stopSession,
            onReset = measurementStateHolder::resetSession,
        )
    }
}

@Composable
private fun PortraitReadyContent(
    uiState: MeasurementUiState,
    gpsStatus: GpsStatusSnapshot,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
) {
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
        SlideHeroBanner(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        MeasurementDashboard(
            modifier = Modifier
                .weight(1f)
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center,
            uiState = uiState,
            onStart = onStart,
            onStop = onStop,
            onReset = onReset,
        )
    }
}

@Composable
private fun LandscapeReadyContent(
    uiState: MeasurementUiState,
    gpsStatus: GpsStatusSnapshot,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight(),
        ) {
            GpsStatusBar(
                gpsStatus = gpsStatus,
                accuracyLabel = uiState.statusBarAccuracyLabel.resolve(),
                qualityLabel = gpsStatus.toQualityLabel(uiState.currentAccuracyMeters).resolve(),
            )
            SlideHeroBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )
        }
        MeasurementDashboard(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
            uiState = uiState,
            onStart = onStart,
            onStop = onStop,
            onReset = onReset,
        )
    }
}

@Composable
private fun SlideHeroBanner(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier,
        tonalElevation = 3.dp,
        shape = shape,
    ) {
        Image(
            painter = painterResource(R.drawable.slide_hero_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.05f)
                .clip(shape)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.4f), shape = shape),
        )
    }
}

@Composable
private fun MeasurementDashboard(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    uiState: MeasurementUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = contentAlignment,
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
                text = uiState.currentSpeedPrimaryLabel.resolve(),
                fontSize = 88.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = uiState.currentSpeedSecondaryLabel.resolve(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
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
                onStart = onStart,
                onStop = onStop,
                onReset = onReset,
            )
        }
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
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = stringResource(R.string.distance_label),
            primaryValue = measurementState.distanceValueLabel.resolve(),
        )
        StatTile(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = stringResource(R.string.average_speed_label),
            primaryValue = measurementState.averageSpeedPrimaryLabel.resolve(),
            secondaryValue = measurementState.averageSpeedSecondaryLabel.resolve(),
        )
        StatTile(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            label = stringResource(R.string.max_speed_label),
            primaryValue = measurementState.maxSpeedPrimaryLabel.resolve(),
            secondaryValue = measurementState.maxSpeedSecondaryLabel.resolve(),
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    primaryValue: String,
    secondaryValue: String? = null,
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
                text = primaryValue,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (secondaryValue != null) {
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
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
    val snapshotStorage = remember(context) { MeasurementSnapshotStorage(context.measurementPreferences()) }
    val measurementStateHolder = remember {
        MeasurementStateHolder(
            initialSnapshot = snapshotStorage.load(),
            snapshotStorage = snapshotStorage,
        )
    }

    DisposableEffect(lifecycleOwner, locationClient, gpsStatusMonitor) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    locationClient.start(
                        onReading = measurementStateHolder::consume,
                        onFailure = { measurementStateHolder.updateGpsStatus(GpsStatusSnapshot(hasFix = false)) },
                    )
                    gpsStatusMonitor.start(measurementStateHolder::updateGpsStatus)
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
            locationClient.start(
                onReading = measurementStateHolder::consume,
                onFailure = { measurementStateHolder.updateGpsStatus(GpsStatusSnapshot(hasFix = false)) },
            )
            gpsStatusMonitor.start(measurementStateHolder::updateGpsStatus)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationClient.stop()
            gpsStatusMonitor.stop()
        }
    }

    return measurementStateHolder
}

private class MeasurementStateHolder(
    initialSnapshot: MeasurementSnapshot,
    private val snapshotStorage: MeasurementSnapshotStorage,
) {
    private val tracker = MeasurementTracker(initialSnapshot = initialSnapshot)

    var uiState by mutableStateOf(tracker.snapshot.toUiState())
        private set
    var gpsStatus by mutableStateOf(GpsStatusSnapshot())
        private set

    fun startSession() {
        tracker.startSession()
        syncState()
    }

    fun stopSession() {
        tracker.stopSession()
        syncState()
    }

    fun resetSession() {
        tracker.resetSession()
        snapshotStorage.clear()
        syncState()
    }

    fun consume(reading: com.example.slidespeed.location.LocationReading) {
        tracker.consume(reading)
        syncState()
    }

    fun updateGpsStatus(status: GpsStatusSnapshot) {
        gpsStatus = status
    }

    private fun syncState() {
        val snapshot = tracker.snapshot
        uiState = snapshot.toUiState()
        snapshotStorage.save(snapshot)
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

private fun Context.measurementPreferences(): SharedPreferences =
    getSharedPreferences(MEASUREMENT_PREFS_NAME, Context.MODE_PRIVATE)

private const val PERMISSION_PREFS_NAME = "slide_speed_permissions"
private const val MEASUREMENT_PREFS_NAME = "slide_speed_measurements"
private const val HAS_REQUESTED_LOCATION_PERMISSION_KEY = "has_requested_location_permission"
