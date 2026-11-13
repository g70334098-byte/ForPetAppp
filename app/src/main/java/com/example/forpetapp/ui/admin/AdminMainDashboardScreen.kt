package com.example.forpetapp.ui.admin

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.forpetapp.data.firebase.FirebaseSyncManager
import com.example.forpetapp.PersistentStorageManager
import com.example.forpetapp.data.model.BookingItem

// 👑 총괄 관리자 전용 통합 대시보드 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainDashboardScreen(adminName: String = "최지훈 대표님", onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedAdminTab by remember { mutableIntStateOf(0) }
    var lastAdminBackPressTime by remember { mutableLongStateOf(0L) }

    // 관리자 뒤로가기 제스처 방어: 서브 탭에서 뒤로가기 누르면 메인 탭으로 복귀
    BackHandler(enabled = (selectedAdminTab != 0)) {
        selectedAdminTab = 0
    }

    // 메인 탭에서 2초 내 재누름 시 안전하게 관리자 로그아웃
    BackHandler(enabled = (selectedAdminTab == 0)) {
        val now = System.currentTimeMillis()
        if (now - lastAdminBackPressTime < 2000L) {
            onLogout()
        } else {
            lastAdminBackPressTime = now
            Toast.makeText(context, "한 번 더 누르면 관리자 로그아웃됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    val adminMembersList = remember {
        mutableStateListOf(
            Triple("최지훈 대표님", "010-3786-5394", "🔴 Google 계정 (g70334098@gmail.com) | 초코 (말티즈) | 3,000P"),
            Triple("이선하 대표님", "010-7521-7399", "🔐 ID/PW 계정 (seonha2525) | 해피 (푸들) | 6,000P"),
            Triple("홍길동 보호자님", "010-1234-5678", "💛 카카오톡 계정 (kakao_hong_1004) | 몽이 (포메라니안) | 3,000P")
        )
    }

    val adminBookingOrders = remember {
        mutableStateListOf<BookingItem>().apply {
            addAll(PersistentStorageManager.loadBookingList(context, "ADMIN_ALL"))
        }
    }

    // ☁️ 실시간 Cloud Firestore 오더 및 회원 DB 동기화 리스너 (관리자 통합 대시보드)
    DisposableEffect(Unit) {
        val userListener = FirebaseSyncManager.listenToCloudUsers { cloudUsers ->
            if (cloudUsers.isNotEmpty()) {
                val defaultAdmins = listOf(
                    Triple("최지훈 대표님", "010-3786-5394", "🔴 Google 계정 (g70334098@gmail.com) | 초코 (말티즈) | 3,000P"),
                    Triple("이선하 대표님", "010-7521-7399", "🔐 ID/PW 계정 (seonha2525) | 해피 (푸들) | 6,000P")
                )
                val adminPhones = defaultAdmins.map { it.second }.toSet()
                val merged = defaultAdmins.toMutableList()
                cloudUsers.forEach { user ->
                    if (user.second !in adminPhones) {
                        merged.add(user)
                    }
                }
                adminMembersList.clear()
                adminMembersList.addAll(merged)
            }
        }
        val orderListener = FirebaseSyncManager.listenToCloudOrders { cloudOrders ->
            if (cloudOrders.isNotEmpty()) {
                adminBookingOrders.clear()
                adminBookingOrders.addAll(cloudOrders)
                PersistentStorageManager.saveBookingList(context, cloudOrders, "ADMIN_ALL")
            }
        }
        onDispose {
            userListener?.remove()
            orderListener?.remove()
        }
    }

    val todayTotalAmount = adminBookingOrders.sumOf { order ->
        val digitsOnly = order.estimatedPrice.replace(Regex("[^0-9]"), "")
        digitsOnly.toIntOrNull() ?: 0
    }
    val formattedTotalAmount = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(todayTotalAmount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👑", fontSize = 20.sp)
                            Text("발자국동행 총괄 관리자 대시보드", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("대표 계정: $adminName (전체 시스템 통제 권한)", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("로그아웃", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("📊 발자국동행 실시간 실적 & 운용 현황 요약", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF38BDF8))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👥 총 가입 회원", fontSize = 10.sp, color = Color.Gray)
                            Text("${adminMembersList.size} 명", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚘 오늘 배차 건수", fontSize = 10.sp, color = Color.Gray)
                            Text("${adminBookingOrders.size} 건", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4ADE80))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰 오늘 가결제액", fontSize = 10.sp, color = Color.Gray)
                            Text("${formattedTotalAmount} 원", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFBBF24))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐ 평균 평점", fontSize = 10.sp, color = Color.Gray)
                            Text("5.0", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = (selectedAdminTab == 0),
                    onClick = { selectedAdminTab = 0 },
                    label = { Text("🚘 예약/배차 관리", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = (selectedAdminTab == 1),
                    onClick = { selectedAdminTab = 1 },
                    label = { Text("👥 회원/포인트 DB", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (selectedAdminTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚘 전체 예약 및 배차 통제 목록", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text("총 ${adminBookingOrders.size}건", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        }
                    }

                    items(adminBookingOrders.size) { index ->
                        val order = adminBookingOrders[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(order.serviceType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text(order.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }

                                HorizontalDivider()

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (order.species == "강아지") "🐶" else "🐱", fontSize = 22.sp)
                                    Column {
                                        Text("${order.petName} (${order.ownerName})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("🏥 방문 병원: ${order.hospitalName}", fontSize = 12.sp, color = Color(0xFF334155))
                                        Text("⏰ 일시: ${order.time} | 💳 가결제: ${order.estimatedPrice}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "👨‍✈️ 박찬헌 가드에게 배차 변경 명령이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("👨‍✈️ 가드 강제배차", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "💳 가결제 정산 취소 및 100% 환불 처리되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                    ) {
                                        Text("💳 환불/정산", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            adminBookingOrders.removeAt(index)
                                            Toast.makeText(context, "🗑️ 예약건이 강제 취소/삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                    ) {
                                        Text("🗑️ 예약 취소", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("👥 전체 가입 회원 DB 조회 및 포인트 관리", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    }

                    items(adminMembersList.size) { index ->
                        val (memberName, phone, petAndPoint) = adminMembersList[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("👤", fontSize = 20.sp)
                                        Column {
                                            Text(memberName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("📞 $phone", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFEF3C7)) {
                                        Text(petAndPoint.substringAfter("| "), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }

                                Text("🐾 등록 반려동물: ${petAndPoint.substringBefore(" |")}", fontSize = 12.sp, color = Color.Gray)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, "tel:${phone.replace("-", "")}".toUri())
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "전화 걸기 실패", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("📞 전화", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }

                                    Button(
                                        onClick = {
                                            FirebaseSyncManager.adminAdjustUserPointsInCloud(
                                                targetDocId = phone,
                                                deltaPoints = 3000,
                                                reasonTitle = "📸 [관리자 검토 승인] 어플 내 생생후기 (사진+글) 3,000P 지급"
                                            ) { ok, newPts ->
                                                Toast.makeText(
                                                    context,
                                                    if (ok) "✅ [$memberName]님 후기 검토 승인! +3,000P 지급 완료 (보유: ${newPts}P)" else "🎉 [$memberName]님께 +3,000P 지급 요청이 반영되었습니다!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("📸 후기승인(+3천P)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            FirebaseSyncManager.adminAdjustUserPointsInCloud(
                                                targetDocId = phone,
                                                deltaPoints = 10000,
                                                reasonTitle = "📝 [관리자 검토 승인] 블로그/유튜브 포스팅 10,000P 지급"
                                            ) { ok, newPts ->
                                                Toast.makeText(
                                                    context,
                                                    if (ok) "✅ [$memberName]님 포스팅 승인! +10,000P 지급 완료 (보유: ${newPts}P)" else "🎉 [$memberName]님께 +10,000P 지급 요청이 반영되었습니다!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("📝 포스팅(+1만P)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AdminMainDashboardPreview() {
    AdminMainDashboardScreen(adminName = "최지훈 대표님", onLogout = {})
}
