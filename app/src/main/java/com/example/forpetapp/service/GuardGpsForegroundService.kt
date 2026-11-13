@file:Suppress("ApplySharedPref", "UseSharedPreferencesEdit", "SpellCheckingInspection")

package com.example.forpetapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * 📡 법인 가드 단말기 전용 백그라운드 GPS 위치 추적 포그라운드 서비스
 * - 오직 본사 승인을 받은 정식 가드(드라이버) 계정으로 인증된 기기에서만 가동됩니다.
 * - 일반 보호자 또는 미인증 사용자 환경에서는 시작 즉시 자체 종료(stopSelf)하여 배터리 및 개인정보를 보호합니다.
 */
class GuardGpsForegroundService : Service() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    override fun onCreate() {
        super.onCreate()
        try {
            // 🛡️ 승인된 가드 계정 검증: SharedPreferences에 guard_verified_auth 플래그 확인
            val pref = getSharedPreferences("BaljagukAppDiskPrefs", MODE_PRIVATE)
            val isGuardAuthorized = pref.getBoolean("is_guard_authorized_device", false)
            if (!isGuardAuthorized) {
                android.util.Log.w("GuardGpsService", "Unauthorized GPS attempt detected. Stopping service.")
                stopSelf()
                return
            }

            val hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                android.util.Log.w("GuardGpsService", "Location permission missing. Stopping service safely.")
                stopSelf()
                return
            }

            startForegroundServiceNotification()
            startContinuousGpsTracking()
        } catch (e: Exception) {
            android.util.Log.e("GuardGpsService", "Service onCreate safe error catch: ${e.message}")
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "guard_gps_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) && (notificationManager != null)) {
            val channel = NotificationChannel(
                channelId,
                "발자국동행 가드 법인차량 24시간 GPS 관제",
                NotificationManager.IMPORTANCE_LOW,
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

        try {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && (hasFineLocation || hasCoarseLocation)) {
                startForeground(
                    1002,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(1002, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardGpsService", "startForeground safely caught: ${e.message}")
        }
    }

    @Suppress("MissingPermission")
    private fun startContinuousGpsTracking() {
        try {
            val hasFineLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasFineLocation && !hasCoarseLocation) {
                return
            }

            locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            locationListener = LocationListener { location ->
                val lat = location.latitude
                val lng = location.longitude
                // 디스크/파이어베이스에 가드 실시간 좌표 24시간 영구 기록
                val pref = getSharedPreferences("BaljagukAppDiskPrefs", MODE_PRIVATE)
                pref.edit(commit = true) {
                    putString("guard_last_lat", lat.toString())
                    putString("guard_last_lng", lng.toString())
                    putLong("guard_last_gps_time", System.currentTimeMillis())
                }
                try {
                    com.example.forpetapp.data.firebase.FirebaseSyncManager.updateGuardLiveGps(lat, lng)
                } catch (_: Exception) {}
            }

            locationListener?.let { listener ->
                if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        3000L, // 3초 초고속 동기화 주기
                        0f,    // 0m (실내/이동 시 초고속 갱신)
                        listener,
                    )
                }
                if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                    locationManager?.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        3000L,
                        0f,
                        listener,
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // 앱이 닫혀도 시스템이 포그라운드 서비스 자동 재시작!
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationListener?.let { locationManager?.removeUpdates(it) }
        } catch (_: Exception) {}
    }
}
