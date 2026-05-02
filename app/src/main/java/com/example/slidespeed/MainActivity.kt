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
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "GPS ready",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start),
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Slide Speed",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "0 km/h",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = "Permission is granted and GPS is enabled. Tracking starts in the next step.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
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

private const val PERMISSION_PREFS_NAME = "slide_speed_permissions"
private const val HAS_REQUESTED_LOCATION_PERMISSION_KEY = "has_requested_location_permission"
