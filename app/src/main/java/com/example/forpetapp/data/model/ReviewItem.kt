package com.example.forpetapp.data.model

import android.net.Uri

// ⭐ 이용 후기 데이터 클래스
data class ReviewItem(
    val id: String = System.currentTimeMillis().toString(),
    val authorName: String,
    val petInfo: String,
    val rating: Int = 5,
    val serviceType: String,
    val content: String,
    val imageUri: Uri? = null,
    val dateText: String = "방금 전"
)
