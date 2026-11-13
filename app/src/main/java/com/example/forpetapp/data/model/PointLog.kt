package com.example.forpetapp.data.model

// ⭐ 포인트 내역 데이터 클래스 (PersistentStorageManager와 연동)
data class PointLog(
    val id: String,
    val title: String,
    val amount: Int, // 양수면 적립, 음수면 사용
    val dateText: String,
    val expireDateText: String = "", // 사용내역이면 공백
    val isEarned: Boolean
)
