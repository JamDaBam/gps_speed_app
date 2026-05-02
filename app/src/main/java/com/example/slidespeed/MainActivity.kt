package com.example.slidespeed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberLauncherForActivityResult
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
    var permissionState by rememberSaveable {
        mutableStateOf(getLocationPermissionState(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        permissionState = getLocationPermissionState(context)
    }

    LaunchedEffect(Unit) {
        permissionState = getLocationPermissionState(context)
    }

    when (permissionState) {
        LocationPermissionState.Granted -> ReadyPlaceholder()
        LocationPermissionState.Denied -> PermissionRequiredContent(
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
        LocationPermissionState.PermanentlyDenied -> PermissionRequiredContent(
            title = context.getString(R.string.permission_title),
            message = context.getString(R.string.permission_rationale),
            buttonLabel = context.getString(R.string.permission_settings_cta),
            onAction = { context.openAppSettings() },
        )
    }
}

@Composable
private fun ReadyPlaceholder() {
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
                text = "Permission granted. GPS readiness handling is the next piece.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
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

private enum class LocationPermissionState {
    Granted,
    Denied,
    PermanentlyDenied,
}

private fun getLocationPermissionState(context: Context): LocationPermissionState {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (hasFineLocation) {
        return LocationPermissionState.Granted
    }

    val activity = context as? ComponentActivity
    val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) ?: false
    val hasRequestedPermission = context.permissionPreferences()
        .getBoolean(HAS_REQUESTED_LOCATION_PERMISSION_KEY, false)

    return if (shouldShowRationale) {
        LocationPermissionState.Denied
    } else if (hasRequestedPermission) {
        LocationPermissionState.PermanentlyDenied
    } else {
        LocationPermissionState.Denied
    }
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    startActivity(intent)
}

private fun Context.permissionPreferences(): SharedPreferences =
    getSharedPreferences(PERMISSION_PREFS_NAME, Context.MODE_PRIVATE)

private const val PERMISSION_PREFS_NAME = "slide_speed_permissions"
private const val HAS_REQUESTED_LOCATION_PERMISSION_KEY = "has_requested_location_permission"
