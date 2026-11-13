package com.example.forpetapp.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.forpetapp.data.model.BookingItem

@Composable
fun PetHandOffSignatureDialog(booking: BookingItem, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✍️ 안심 인계 서명 및 인증", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("아이 픽업 및 인계 확인 서명을 진행합니다.")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm("서명완료"); onDismiss() }) {
                Text("인계 완료 확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
