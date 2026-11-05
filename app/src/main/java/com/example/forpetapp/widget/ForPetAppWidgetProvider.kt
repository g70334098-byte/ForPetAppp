package com.example.forpetapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.forpetapp.MainActivity
import com.example.forpetapp.PersistentStorageManager
import com.example.forpetapp.R

/**
 * 🐾 발자국동행 스마트폰 홈 화면 실시간 안심 케어 관제 위젯
 */
class ForPetAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_forpet_care)

            val bookingList = PersistentStorageManager.loadBookingList(context)
            val latestBooking = bookingList.firstOrNull { !it.isCompleted } ?: bookingList.firstOrNull()

            if (latestBooking != null) {
                views.setTextViewText(R.id.widget_tv_status, "${latestBooking.petName} ${latestBooking.status}")
                views.setTextViewText(R.id.widget_tv_destination, "목적지: ${latestBooking.hospitalName}")
                views.setTextViewText(R.id.widget_tv_vehicle, "🚗 ${latestBooking.vehicleInfo}")
                views.setTextViewText(R.id.widget_tv_badge, "🟢 LIVE GPS")
            } else {
                views.setTextViewText(R.id.widget_tv_status, "예약된 안심 동행 일정이 없습니다.")
                views.setTextViewText(R.id.widget_tv_destination, "발자국동행 앱에서 24시 케어카를 예약하세요.")
                views.setTextViewText(R.id.widget_tv_vehicle, "🚗 24시 응급/예약 항시 대기 중")
                views.setTextViewText(R.id.widget_tv_badge, "대기 중")
            }

            // 위젯 탭 시 앱 메인화면 바로가기
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, ForPetAppWidgetProvider::class.java),
                )
                if (ids.isNotEmpty()) {
                    for (id in ids) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
