package com.example.slidespeed.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat

/**
 * Observes GNSS satellite visibility and fix usage for the lightweight GPS status bar.
 */
class GpsStatusMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private var callback: GnssStatusCompat.Callback? = null

    @SuppressLint("MissingPermission")
    fun start(onStatusChanged: (GpsStatusSnapshot) -> Unit) {
        if (callback != null || locationManager == null) {
            return
        }

        val gnssCallback = object : GnssStatusCompat.Callback() {
            override fun onStarted() {
                onStatusChanged(GpsStatusSnapshot(hasFix = false))
            }

            override fun onStopped() {
                onStatusChanged(GpsStatusSnapshot(hasFix = false))
            }

            override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
                var visibleSatellites = 0
                var usedSatellites = 0

                for (index in 0 until status.satelliteCount) {
                    visibleSatellites += 1
                    if (status.usedInFix(index)) {
                        usedSatellites += 1
                    }
                }

                onStatusChanged(
                    GpsStatusSnapshot(
                        visibleSatellites = visibleSatellites,
                        usedSatellites = usedSatellites,
                        hasFix = usedSatellites > 0,
                    ),
                )
            }
        }

        val registered = LocationManagerCompat.registerGnssStatusCallback(
            locationManager,
            ContextCompat.getMainExecutor(appContext),
            gnssCallback,
        )

        if (registered) {
            callback = gnssCallback
        } else {
            onStatusChanged(GpsStatusSnapshot(hasFix = false))
        }
    }

    fun stop() {
        val locationManager = locationManager ?: return
        callback?.let { gnssCallback ->
            LocationManagerCompat.unregisterGnssStatusCallback(locationManager, gnssCallback)
        }
        callback = null
    }
}

data class GpsStatusSnapshot(
    val visibleSatellites: Int? = null,
    val usedSatellites: Int? = null,
    val hasFix: Boolean = false,
)
