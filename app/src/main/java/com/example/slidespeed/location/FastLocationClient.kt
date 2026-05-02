package com.example.slidespeed.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.core.location.LocationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Wraps fused location updates with an aggressive high-accuracy request tuned for short rides.
 */
class FastLocationClient(context: Context) {
    private val appContext = context.applicationContext
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
    private var locationCallback: LocationCallback? = null
    private var lastDeliveredLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun start(
        onReading: (LocationReading) -> Unit,
        onFailure: () -> Unit,
    ) {
        if (locationCallback != null || !appContext.hasLocationPermission()) {
            if (!appContext.hasLocationPermission()) {
                onFailure()
            }
            return
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations.sortedBy { it.time }) {
                    onReading(location.toLocationReading(lastDeliveredLocation))
                    lastDeliveredLocation = location
                }
            }
        }
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        onReading(it.toLocationReading(lastDeliveredLocation))
                        lastDeliveredLocation = it
                    }
                }
                .addOnFailureListener {
                    onFailure()
                }
            fusedLocationClient.requestLocationUpdates(
                LOCATION_REQUEST,
                callback,
                Looper.getMainLooper(),
            )
            locationCallback = callback
        } catch (_: SecurityException) {
            locationCallback = null
            onFailure()
        } catch (_: IllegalStateException) {
            locationCallback = null
            onFailure()
        }
    }

    fun stop() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
        lastDeliveredLocation = null
    }

    private fun Location.toLocationReading(previousLocation: Location?): LocationReading {
        return LocationReading(
            speedKmh = preferredSpeedKmh(previousLocation),
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            latitude = latitude,
            longitude = longitude,
            timestampMillis = time,
            isMock = LocationCompat.isMock(this),
        )
    }

    private fun Location.preferredSpeedKmh(previousLocation: Location?): Float {
        val directSpeedKmh = if (hasSpeed()) speed * KMH_PER_MPS else null
        if (directSpeedKmh != null && directSpeedKmh.isFinite() && directSpeedKmh >= 0f) {
            return directSpeedKmh
        }

        if (previousLocation == null) {
            return 0f
        }

        val elapsedSeconds = (time - previousLocation.time) / 1_000f
        if (!elapsedSeconds.isFinite() || elapsedSeconds <= 0f) {
            return 0f
        }

        val distanceMeters = previousLocation.distanceTo(this)
        val calculatedSpeedKmh = distanceMeters / elapsedSeconds * KMH_PER_MPS
        return calculatedSpeedKmh.takeIf { it.isFinite() && it >= 0f } ?: 0f
    }

    private companion object {
        private const val KMH_PER_MPS = 3.6f

        val LOCATION_REQUEST: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            250L,
        )
            .setMinUpdateIntervalMillis(100L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(0L)
            .build()
    }
}

data class LocationReading(
    val speedKmh: Float,
    val accuracyMeters: Float?,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val isMock: Boolean = false,
)
