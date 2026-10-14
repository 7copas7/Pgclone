package com.pgclone.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.*
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.*

class MockLocationService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationManager: LocationManager

    var currentLat: Double = -23.5505
    var currentLng: Double = -46.6333
    var currentAlt: Double = 10.0
    var currentSpeed: Float = 0f
    var updateInterval: Long = 1000L

    private val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        "fused"
    )

    companion object {
        const val CHANNEL_ID = "mock_gps_channel"
        const val NOTIF_ID = 1
        const val ACTION_UPDATE = "UPDATE_COORDS"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_SPEED = "speed"
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        registerProviders()
        startPushing()
    }

    private fun registerProviders() {
        providers.forEach { provider ->
            try {
                locationManager.addTestProvider(
                    provider,
                    false, false, false, false,
                    true, true, true,
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
                )
                locationManager.setTestProviderEnabled(provider, true)
            } catch (e: Exception) { }
        }
    }

    private fun startPushing() {
        scope.launch {
            while (isActive) {
                pushLocation(currentLat, currentLng, currentAlt, currentSpeed)
                delay(updateInterval)
            }
        }
    }

    fun pushLocation(lat: Double, lng: Double, alt: Double, speed: Float) {
        providers.forEach { provider ->
            try {
                val loc = Location(provider).apply {
                    latitude = lat
                    longitude = lng
                    altitude = alt
                    accuracy = 3f
                    this.speed = speed
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    if (Build.VERSION.SDK_INT >= 26) {
                        verticalAccuracyMeters = 1f
                        speedAccuracyMetersPerSecond = 0.1f
                        bearingAccuracyDegrees = 0.1f
                    }
                }
                locationManager.setTestProviderLocation(provider, loc)
            } catch (e: Exception) { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE) {
            currentLat = intent.getDoubleExtra(EXTRA_LAT, currentLat)
            currentLng = intent.getDoubleExtra(EXTRA_LNG, currentLng)
            currentSpeed = intent.getFloatExtra(EXTRA_SPEED, 0f)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        providers.forEach { provider ->
            try {
                locationManager.setTestProviderEnabled(provider, false)
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) { }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Spoof Ativo",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS ativo")
            .setContentText("Localização fictícia em execução")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
}
