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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.example.slidespeed.ui.theme.SpeedBlue
import com.example.slidespeed.ui.theme.SpeedCream
import com.example.slidespeed.ui.theme.SpeedInk
import com.example.slidespeed.ui.theme.SpeedOrange
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 18.dp),
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
                    onStart = measurementStateHolder::startSession,
                    onStop = measurementStateHolder::stopSession,
                    onReset = measurementStateHolder::resetSession,
                )
            }
        }
        // Text(
        //
        // text = stringResource(R.string.measurement_note),
        // style = MaterialTheme.typography.bodySmall,
        // modifier = Modifier.fillMaxWidth(),
        // )
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.05f)
                .clip(shape)
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.4f), shape = shape),
        ) {
            val width = size.width
            val height = size.height

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SpeedBlue,
                        Color(0xFF2FB5F2),
                        Color(0xFFF7E45D),
                    ),
                    start = Offset.Zero,
                    end = Offset(width, height),
                ),
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF9BF), Color.Transparent),
                    center = Offset(width * 0.8f, height * 0.44f),
                    radius = width * 0.38f,
                ),
                radius = width * 0.38f,
                center = Offset(width * 0.8f, height * 0.44f),
            )

            val hillPath = Path().apply {
                moveTo(-width * 0.05f, height * 0.7f)
                cubicTo(
                    width * 0.15f,
                    height * 0.48f,
                    width * 0.52f,
                    height * 1.02f,
                    width * 1.06f,
                    height * 0.56f,
                )
                lineTo(width * 1.08f, height * 1.1f)
                lineTo(-width * 0.05f, height * 1.1f)
                close()
            }
            drawPath(
                path = hillPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1B6E0F),
                        Color(0xFF83C700),
                        Color(0xFFD7F014),
                    ),
                    start = Offset(0f, height),
                    end = Offset(width, height * 0.45f),
                ),
            )

            drawPath(
                path = Path().apply {
                    moveTo(-width * 0.02f, height * 0.76f)
                    cubicTo(
                        width * 0.22f,
                        height * 0.54f,
                        width * 0.55f,
                        height * 1.02f,
                        width * 1.04f,
                        height * 0.62f,
                    )
                },
                color = Color.White.copy(alpha = 0.88f),
                style = Stroke(width = height * 0.035f, cap = StrokeCap.Round),
            )

            repeat(4) { index ->
                val offsetX = width * (0.05f + index * 0.12f)
                drawLine(
                    color = Color.White.copy(alpha = 0.45f - index * 0.07f),
                    start = Offset(offsetX, height * (0.18f + index * 0.1f)),
                    end = Offset(offsetX + width * 0.2f, height * (0.46f + index * 0.12f)),
                    strokeWidth = height * (0.05f - index * 0.006f),
                    cap = StrokeCap.Round,
                )
            }

            val bodyCenter = Offset(width * 0.52f, height * 0.62f)
            val fluffColor = SpeedCream
            val fluffShade = Color(0xFFE8DCC6)
            val headColor = Color(0xFF2D2F37)
            val limbColor = SpeedInk
            val innerEarColor = Color(0xFFC86A49)

            val fluffOffsets = listOf(
                Offset(-0.16f, -0.05f),
                Offset(-0.06f, -0.18f),
                Offset(0.08f, -0.2f),
                Offset(0.2f, -0.1f),
                Offset(-0.2f, 0.08f),
                Offset(-0.06f, 0.12f),
                Offset(0.1f, 0.14f),
                Offset(0.24f, 0.08f),
                Offset(-0.02f, 0.28f),
            )
            fluffOffsets.forEachIndexed { index, offset ->
                drawCircle(
                    color = if (index % 3 == 0) fluffShade else fluffColor,
                    radius = width * (0.052f + (index % 2) * 0.008f),
                    center = Offset(
                        bodyCenter.x + width * offset.x,
                        bodyCenter.y + height * offset.y,
                    ),
                )
            }

            rotate(-18f, pivot = Offset(width * 0.68f, height * 0.57f)) {
                drawRoundRect(
                    color = limbColor.copy(alpha = 0.92f),
                    topLeft = Offset(width * 0.63f, height * 0.54f),
                    size = Size(width * 0.19f, height * 0.08f),
                    cornerRadius = CornerRadius(height * 0.05f, height * 0.05f),
                )
            }
            rotate(25f, pivot = Offset(width * 0.26f, height * 0.63f)) {
                drawRoundRect(
                    color = limbColor.copy(alpha = 0.9f),
                    topLeft = Offset(width * 0.19f, height * 0.6f),
                    size = Size(width * 0.18f, height * 0.08f),
                    cornerRadius = CornerRadius(height * 0.05f, height * 0.05f),
                )
            }

            rotate(34f, pivot = Offset(width * 0.62f, height * 0.87f)) {
                drawRoundRect(
                    color = limbColor,
                    topLeft = Offset(width * 0.56f, height * 0.8f),
                    size = Size(width * 0.11f, height * 0.1f),
                    cornerRadius = CornerRadius(height * 0.05f, height * 0.05f),
                )
            }
            rotate(16f, pivot = Offset(width * 0.77f, height * 0.84f)) {
                drawRoundRect(
                    color = limbColor,
                    topLeft = Offset(width * 0.72f, height * 0.77f),
                    size = Size(width * 0.11f, height * 0.1f),
                    cornerRadius = CornerRadius(height * 0.05f, height * 0.05f),
                )
            }

            rotate(-9f, pivot = Offset(width * 0.55f, height * 0.47f)) {
                drawOval(
                    color = headColor,
                    topLeft = Offset(width * 0.42f, height * 0.29f),
                    size = Size(width * 0.26f, height * 0.33f),
                )
            }

            rotate(-18f, pivot = Offset(width * 0.36f, height * 0.42f)) {
                drawOval(
                    color = headColor,
                    topLeft = Offset(width * 0.28f, height * 0.37f),
                    size = Size(width * 0.12f, height * 0.08f),
                )
                drawOval(
                    color = innerEarColor,
                    topLeft = Offset(width * 0.3f, height * 0.39f),
                    size = Size(width * 0.075f, height * 0.045f),
                )
            }
            rotate(12f, pivot = Offset(width * 0.67f, height * 0.4f)) {
                drawOval(
                    color = headColor,
                    topLeft = Offset(width * 0.62f, height * 0.34f),
                    size = Size(width * 0.12f, height * 0.08f),
                )
                drawOval(
                    color = innerEarColor,
                    topLeft = Offset(width * 0.645f, height * 0.36f),
                    size = Size(width * 0.075f, height * 0.045f),
                )
            }

            val headFluffCenters = listOf(
                Offset(width * 0.43f, height * 0.33f),
                Offset(width * 0.5f, height * 0.26f),
                Offset(width * 0.59f, height * 0.28f),
                Offset(width * 0.66f, height * 0.35f),
            )
            headFluffCenters.forEachIndexed { index, center ->
                drawCircle(
                    color = if (index == 0) fluffShade else fluffColor,
                    radius = width * (0.048f - index * 0.002f),
                    center = center,
                )
            }

            drawOval(
                color = Color.White,
                topLeft = Offset(width * 0.44f, height * 0.39f),
                size = Size(width * 0.05f, height * 0.12f),
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(width * 0.53f, height * 0.41f),
                size = Size(width * 0.05f, height * 0.12f),
            )
            drawOval(
                color = Color.Black,
                topLeft = Offset(width * 0.455f, height * 0.44f),
                size = Size(width * 0.028f, height * 0.07f),
            )
            drawOval(
                color = Color.Black,
                topLeft = Offset(width * 0.545f, height * 0.45f),
                size = Size(width * 0.028f, height * 0.07f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = width * 0.008f,
                center = Offset(width * 0.474f, height * 0.455f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = width * 0.008f,
                center = Offset(width * 0.564f, height * 0.466f),
            )

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(width * 0.505f, height * 0.53f),
                size = Size(width * 0.055f, height * 0.02f),
                cornerRadius = CornerRadius(height * 0.01f, height * 0.01f),
            )
            drawArc(
                color = Color.Black,
                startAngle = 14f,
                sweepAngle = 118f,
                useCenter = false,
                topLeft = Offset(width * 0.47f, height * 0.5f),
                size = Size(width * 0.14f, height * 0.12f),
                style = Stroke(width = height * 0.023f, cap = StrokeCap.Round),
            )

            drawArc(
                color = SpeedOrange.copy(alpha = 0.98f),
                startAngle = 4f,
                sweepAngle = 146f,
                useCenter = true,
                topLeft = Offset(width * 0.49f, height * 0.54f),
                size = Size(width * 0.08f, height * 0.11f),
            )
            drawArc(
                color = Color(0xFFFF5A68),
                startAngle = 318f,
                sweepAngle = 96f,
                useCenter = true,
                topLeft = Offset(width * 0.53f, height * 0.59f),
                size = Size(width * 0.05f, height * 0.06f),
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = width * 0.42f,
                center = Offset(width * 0.03f, height * 0.95f),
                blendMode = BlendMode.Screen,
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.distance_label),
            primaryValue = measurementState.distanceValueLabel.resolve(),
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.average_speed_label),
            primaryValue = measurementState.averageSpeedPrimaryLabel.resolve(),
            secondaryValue = measurementState.averageSpeedSecondaryLabel.resolve(),
        )
        StatTile(
            modifier = Modifier.weight(1f),
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
