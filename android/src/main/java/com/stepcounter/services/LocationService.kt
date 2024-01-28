package com.stepcounter.services
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class LocationService(private val context: Context, private val sensorListenService: SensorListenService) {
    private val fusedLocationClient: FusedLocationProviderClient

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun getCurrentLocation(promise: Promise) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((context as Activity), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 5000)
            return
        }
        val locationRequest: com.google.android.gms.location.LocationRequest = com.google.android.gms.location.LocationRequest.create()
        locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(1000L)
        locationRequest.setFastestInterval(1000L)
        locationRequest.setSmallestDisplacement(1f)
        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    val location: Location = locationResult.getLastLocation()
                    val locationMap = createLocationMap(location)
                    sensorListenService.updateLocationMap(locationMap)
                    promise.resolve(locationMap)
                } else {
                    promise.reject("LOCATION_UNAVAILABLE", "Konum alınamadı")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun createLocationMap(location: Location): WritableMap {
        val locationMap = Arguments.createMap()
        locationMap.putDouble("latitude", location.latitude)
        locationMap.putDouble("longitude", location.longitude)
        locationMap.putDouble("accuracy", location.accuracy.toDouble())
        locationMap.putDouble("altitude", location.altitude)
        locationMap.putDouble("speed", location.speed.toDouble())
        locationMap.putDouble("timestamp", location.time.toDouble())
        return locationMap
    }
}
