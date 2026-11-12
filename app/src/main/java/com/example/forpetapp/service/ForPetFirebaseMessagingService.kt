package com.example.forpetapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.example.forpetapp.MainActivity
import com.example.forpetapp.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * 🔔 발자국동행 실시간 FCM 푸시 알림 수신 서비스
 * - 오더 상태 변경 (픽업 완료, 병원 도착, 진료 완료 등) 실시간 헤드업 알림 수신
 * - 관리자 긴급 공지 및 가드 실시간 배차 알림 처리
 */
class ForPetFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("ForPetFCM", "New FCM registration token: $token")
        // 디바이스 로컬 설정에 FCM 토큰 저장
        val prefs = getSharedPreferences("BaljagukAppDiskPrefs", MODE_PRIVATE)
        prefs.edit { putString("fcm_device_token", token) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        android.util.Log.d("ForPetFCM", "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "🐾 발자국동행 알림"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "새로운 케어 배차 정보가 업데이트되었습니다."

        sendHeadsUpNotification(title, body)
    }

    private fun sendHeadsUpNotification(title: String, body: String) {
        val channelId = "forpet_urgent_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "발자국동행 케어 및 배차 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "1:1 반려동물 이동 및 진료 대행 실시간 진행 상태 알림"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        try {
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notificationBuilder.build())
        } catch (_: Exception) {}
    }

    companion object {
        fun sendLocalNotification(context: Context, title: String, body: String) {
            val channelId = "forpet_urgent_channel"
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "발자국동행 케어 및 배차 긴급 알림",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "1:1 반려동물 이동 및 진료 대행 실시간 진행 상태 알림"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .build()

            try {
                notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            } catch (_: Exception) {}
        }
    }
}
