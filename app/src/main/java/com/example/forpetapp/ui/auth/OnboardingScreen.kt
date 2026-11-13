package com.example.forpetapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        Pair("🚗 1:1 안심 픽업", "반려동물 전용 특화 차량으로\n보호자 없이도 안전하게 픽업합니다."),
        Pair("🏥 동물병원 안심 동행", "전문 지식을 갖춘 펫 가드가\n동물병원 진료를 밀착 대행합니다."),
        Pair("📄 상세 진료 리포트", "진료 소견과 영수증, 처방전을\n스마트폰으로 즉시 전송해 드립니다."),
        Pair("📸 귀가 안심 인증", "진료 후 아이를 집안에 인계하고\n안전 귀가 인증샷을 보내드립니다.")
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFE5D5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = pages[page].first,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = pages[page].second,
                    fontSize = 16.sp,
                    color = Color(0xFF78350F),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Page Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF92400E) else Color(0xFFD4D4D8))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
        ) {
            Text(
                if (pagerState.currentPage < pages.lastIndex) "다음" else "시작하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
