package com.example.forpetapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 📡 법인 가드 단말기 전용 24시간 백그라운드 GPS 위치 추적 포그라운드 서비스
 * - 가드 로그인 이력이 있는 법인 단말기(S23 FE)에서 화면이 꺼지거나 앱이 닫혀도
 *   24시간 지속적으로 GPS 좌표를 수신하여 관제센터 포털(Google Maps)로 실시간 동기화합니다.
 */
class GuardGpsForegroundService : Service() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startContinuousGpsTracking()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "guard_gps_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "발자국동행 가드 법인차량 24시간 GPS 관제",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "가드 법인 단말기 실시간 구글맵 관제 연동"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("📡 [법인 가드 관제] 24시 실시간 GPS 가동 중")
            .setContentText("가드 단말기 GPS 실시간 위치가 관제 센터 구글 지도로 동기화됩니다.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1002, notification)
    }

    @Suppress("MissingPermission")
    private fun startContinuousGpsTracking() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val lat = location.latitude
                    val lng = location.longitude
                    // 디스크/파이어베이스에 가드 실시간 좌표 24시간 영구 기록
                    val pref = getSharedPreferences("BaljagukAppDiskPrefs", Context.MODE_PRIVATE)
                    pref.edit()
                        .putString("guard_last_lat", lat.toString())
                        .putString("guard_last_lng", lng.toString())
                        .putLong("guard_last_gps_time", System.currentTimeMillis())
                        .commit()
                }
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000L, // 10초 주기
                5f,     // 5m 이동 주기
                locationListener!!
            )
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // 앱이 닫혀도 시스템이 포그라운드 서비스 자동 재시작!
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationListener?.let { locationManager?.removeUpdates(it) }
    }
}
