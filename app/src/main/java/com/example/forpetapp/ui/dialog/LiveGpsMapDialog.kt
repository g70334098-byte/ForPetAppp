package com.example.forpetapp.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.forpetapp.data.model.BookingItem

@Composable
fun LiveGpsMapDialog(booking: BookingItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🗺️ 실시간 GPS 관제 모니터링", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("이송 차량: ${booking.vehicleInfo}")
                Text("목적지: ${booking.hospitalName}")
                Text("현재 상태: 이동 중 (안심 레이 케어카)")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("확인")
            }
        }
    )
}
