package com.example.slidespeed.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
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

    @SuppressLint("MissingPermission")
    fun start(onReading: (LocationReading) -> Unit) {
        if (locationCallback != null) {
            return
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onReading(location.toLocationReading())
                }
            }
        }
        locationCallback = callback

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { onReading(it.toLocationReading()) }
        }
        fusedLocationClient.requestLocationUpdates(
            LOCATION_REQUEST,
            callback,
            Looper.getMainLooper(),
        )
    }

    fun stop() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        locationCallback = null
    }

    private fun Location.toLocationReading(): LocationReading {
        return LocationReading(
            speedKmh = speed * KMH_PER_MPS,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            latitude = latitude,
            longitude = longitude,
            timestampMillis = time,
        )
    }

    private companion object {
        private const val KMH_PER_MPS = 3.6f

        val LOCATION_REQUEST: LocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            500L,
        )
            .setMinUpdateIntervalMillis(200L)
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
)
