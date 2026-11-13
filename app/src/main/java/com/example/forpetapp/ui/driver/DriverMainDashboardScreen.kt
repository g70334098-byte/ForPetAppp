package com.example.forpetapp.ui.driver

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.example.forpetapp.ui.dialog.DriverChatDialog
import com.example.forpetapp.ui.dialog.LiveGpsMapDialog
import com.example.forpetapp.PersistentStorageManager
import com.example.forpetapp.ui.dialog.PetHandOffSignatureDialog
import com.example.forpetapp.data.firebase.FirebaseSyncManager
import com.example.forpetapp.data.model.BookingItem
import com.example.forpetapp.data.model.ChatMessage
import com.example.forpetapp.sendSystemPushNotification
import com.example.forpetapp.service.ForPetFirebaseMessagingService
import java.util.Locale

// 👨‍✈️ 담당 가드 전용 메인 대시보드 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMainDashboardScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var isBodycamActive by remember { mutableStateOf(true) }
    var selectedOrderForGpsMap by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForChat by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForReport by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForReturnPhoto by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForHandOff by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForQuickNotify by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForBodycamUpload by remember { mutableStateOf<BookingItem?>(null) }
    var unlockedDoorlockId by remember { mutableStateOf<String?>(null) }

    val driverOrders = remember {
        mutableStateListOf<BookingItem>().apply {
            val savedList = PersistentStorageManager.loadBookingList(context, "DRIVER_ALL")
            if (savedList.isNotEmpty()) {
                addAll(savedList)
            } else {
                add(
                    BookingItem(
                        id = "order_01",
                        serviceType = "🏥 병원 진료 대행 (병원 예약 완료건)",
                        species = "강아지",
                        petName = "초코",
                        ownerName = "홍길동 (010-1234-5678)",
                        hospitalName = "명지24시온누리메디컬센터",
                        time = "오늘 오전 10:00",
                        status = "2. 자택 도착 및 아이 안심 픽업 완료 🐶",
                        vehicleInfo = "발자국동행 기아 레이 펫전용차 (부산 38가 5394)",
                        symptomNote = "슬개골 검진 및 귀 세정 처방 요청 / 출입: 도어락 비밀번호",
                        estimatedPrice = "35,000원 (가결제)"
                    )
                )
            }
        }
    }

    val bodycamLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val order = selectedOrderForBodycamUpload
        if (uri != null && order != null) {
            val updated = order.copy(bodycamPhotoUri = uri.toString())
            val idx = driverOrders.indexOfFirst { it.id == order.id }
            if (idx != -1) {
                driverOrders[idx] = updated
            }
            PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
            FirebaseSyncManager.syncOrderToCloud(updated, context)
            Toast.makeText(context, "📹 [발자국동행] 실시간 바디캠 캡처 사진이 관제 서버 및 보호자 앱으로 전송되었습니다!", Toast.LENGTH_LONG).show()
            selectedOrderForBodycamUpload = null
        }
    }

    // ☁️ 실시간 Cloud Firestore 오더 동기화 리스너 (가드 워크스페이스)
    DisposableEffect(Unit) {
        val listener = FirebaseSyncManager.listenToCloudOrders { cloudOrders ->
            if (cloudOrders.isNotEmpty()) {
                driverOrders.clear()
                driverOrders.addAll(cloudOrders)
                PersistentStorageManager.saveBookingList(context, cloudOrders, "DRIVER_ALL")
            }
        }
        onDispose {
            listener?.remove()
        }
    }

    var currentGuardTab by remember { mutableIntStateOf(0) }
    var lastGuardBackPressTime by remember { mutableLongStateOf(0L) }

    // 뒤로가기 제스처 방어: 관제 지도 탭에서 뒤로가기 누르면 오더 관리로 복귀 (앱 종료/튕김 방지)
    BackHandler(enabled = (currentGuardTab != 0)) {
        currentGuardTab = 0
    }

    // 오더 관리 탭에서 뒤로가기 누르면 2초 내 재입력 시 안전하게 로그아웃
    BackHandler(enabled = (currentGuardTab == 0)) {
        val now = System.currentTimeMillis()
        if (now - lastGuardBackPressTime < 2000L) {
            onLogout()
        } else {
            lastGuardBackPressTime = now
            Toast.makeText(context, "한 번 더 누르면 로그아웃됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("👨‍✈️ 담당 가드 워크스페이스", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF0F172A))
                        Text("박찬헌 가드 (차량: 부산 38가 5394)", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("로그아웃", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFE5D5))
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFEFE5D5),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = (currentGuardTab == 0),
                    onClick = { currentGuardTab = 0 },
                    icon = { Text("📋", fontSize = 18.sp) },
                    label = { Text("오더 관리", fontWeight = if (currentGuardTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0284C7),
                        selectedTextColor = Color(0xFF0284C7),
                        indicatorColor = Color(0xFFE0F2FE)
                    )
                )
                NavigationBarItem(
                    selected = (currentGuardTab == 1),
                    onClick = { currentGuardTab = 1 },
                    icon = { Text("🗺️", fontSize = 18.sp) },
                    label = { Text("관제 지도", fontWeight = if (currentGuardTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0284C7),
                        selectedTextColor = Color(0xFF0284C7),
                        indicatorColor = Color(0xFFE0F2FE)
                    )
                )
            }
        }
    ) { paddingValues ->
        if (currentGuardTab == 0) {
            val completedCount = driverOrders.count { it.isCompleted }
            val totalRevenue = driverOrders.filter { it.isCompleted }.sumOf {
                it.estimatedPrice.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 35000
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFEFE5D5))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

            // 0. 수익 정산 대시보드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💰 이번 달 누적 정산금", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(java.text.NumberFormat.getNumberInstance(Locale.KOREA).format(totalRevenue) + "원", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("완료된 동행 건수", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text("${completedCount}건", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7))
                    }
                }
            }

            // 1. 드라이버 상태 및 바디캠 스위치 컨트롤 배너
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📹", fontSize = 20.sp)
                            Text("바디캠 라이브 스트리밍", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        Switch(
                            checked = isBodycamActive,
                            onCheckedChange = {
                                isBodycamActive = it
                                Toast.makeText(context, if (it) "📹 바디캠 송출이 시작되었습니다." else "📹 바디캠 송출이 일시 정지되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("📍 실시간 GPS: 정상 (부산 강서구 명지동)", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        Text("🌡️ 차내 온도: 22.5℃", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                }
            }

            Text("🚘 담당 배차 오더 및 동선 관리 (${driverOrders.size}건)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(driverOrders.size) { index ->
                    val order = driverOrders[index]
                    val isDoorlockUnlocked = unlockedDoorlockId == order.id

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Text(
                                        text = order.serviceType,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = order.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }

                            HorizontalDivider()

                            // 📹 실시간 바디캠 캡처 전송 버튼 (가드 전용)
                            Button(
                                onClick = {
                                    selectedOrderForBodycamUpload = order
                                    bodycamLauncher.launch("image/*")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("📹 실시간 바디캠(현장 캡처) 전송", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }

                            if (order.status.contains("승낙 대기") || order.status.contains("접수 대기") || order.status.contains("대기")) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("⏳ 새로운 예약 승낙 대기 중", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                                            Text("가드가 승낙하기 전까지 보호자가 직접 취소 가능합니다.", fontSize = 10.sp, color = Color(0xFF92400E))
                                        }
                                        Button(
                                            onClick = {
                                                val updatedOrder = order.copy(status = "배차 확정 (가드 수락 완료)")
                                                driverOrders[index] = updatedOrder
                                                PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                                                FirebaseSyncManager.updateOrderStatus(context, order.id, "배차 확정 (가드 수락 완료)", order.petName)
                                                FirebaseSyncManager.syncOrderToCloud(updatedOrder)
                                                sendSystemPushNotification(context, "✅ [발자국동행] 가드 배차 수락", "[${order.petName}] 담당 가드가 오더를 승낙 및 배차 확정하였습니다.")
                                                Toast.makeText(context, "✅ [${order.petName}] 오더를 승낙하였습니다.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("⚡ 오더 승낙", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // 고객 및 환묘/환견 정보
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(if (order.species == "강아지") "🐶" else "🐱", fontSize = 24.sp)
                                    Column {
                                        Text("${order.petName} (보호자: ${order.ownerName})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                        Text("🏥 목적지: ${order.hospitalName}", fontSize = 12.sp, color = Color(0xFF334155))
                                        Text("⏰ 예약 시간: ${order.time}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                // 내비게이션 연동 버튼
                                IconButton(
                                    onClick = {
                                        try {
                                            // 카카오내비 목적지 길안내 호출 (없으면 플레이스토어로)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("kakaomap://route?ep=${order.hospitalName}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "카카오맵/카카오내비 앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.background(Color(0xFFFACC15), CircleShape).size(40.dp)
                                ) {
                                    Text("🚙", fontSize = 18.sp)
                                }
                            }

                            // 🔑 자택 도어락/출입키 보안 열람 카드
                            val isDoorlockPurged = order.doorlockPurged || order.isCompleted || order.status.contains("안전 귀가 완료")
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDoorlockPurged) Color(0xFFF1F5F9) else if (isDoorlockUnlocked) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDoorlockPurged) Color(0xFFE2E8F0) else if (isDoorlockUnlocked) Color(0xFFFDE68A) else Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(if (isDoorlockPurged) "🔒" else "🔑", fontSize = 14.sp)
                                            Text(
                                                if (isDoorlockPurged) "자택 출입키 정보 (영구 파기됨)" else "자택 출입 & 도어락 보안 정보",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isDoorlockPurged) Color(0xFF64748B) else Color(0xFF92400E)
                                            )
                                        }
                                        if (!isDoorlockPurged) {
                                            TextButton(
                                                onClick = {
                                                    unlockedDoorlockId = if (isDoorlockUnlocked) null else order.id
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(if (isDoorlockUnlocked) "🔒 보안 잠금" else "🔓 출입키 암호 해제", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                            }
                                        }
                                    }

                                    if (isDoorlockPurged) {
                                        Text("✅ 케어 완료로 비밀번호 및 현관 사진이 DB에서 영구 파기(삭제)되었습니다. (개인정보보호법 준수)", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                                    } else if (isDoorlockUnlocked) {
                                        val decryptedPassword = remember(order.encryptedDoorlock) {
                                            if (!order.encryptedDoorlock.isNullOrBlank()) {
                                                com.example.forpetapp.security.DoorlockSecurityManager.decrypt(order.encryptedDoorlock)
                                            } else null
                                        }

                                        if (!decryptedPassword.isNullOrBlank()) {
                                            Text("• 복호화된 자택 도어락 비번: $decryptedPassword", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                        } else {
                                            Text("• 세대 출입: 보호자 직접 인계 또는 사전 전달된 출입키", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        }
                                        if (!order.doorlockPhotoUri.isNullOrBlank()) {
                                            Text("• 현관 위치 사진 첨부됨 (인계 서명 시 확인 가능)", fontSize = 11.sp, color = Color(0xFF0284C7))
                                        }
                                        Text("※ 출동 중에만 암호 해제되며 안심 귀가 완료 시 클라우드에서 즉시 영구 파기(삭제)됩니다.", fontSize = 10.sp, color = Color.Gray)

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(
                                                onClick = {
                                                    val updatedOrder = order.copy(encryptedDoorlock = null, doorlockPhotoUri = null, doorlockPurged = true)
                                                    driverOrders[index] = updatedOrder
                                                    PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                                                    FirebaseSyncManager.purgeDoorlockFromCloud(order.id)
                                                    FirebaseSyncManager.syncOrderToCloud(updatedOrder)
                                                    Toast.makeText(context, "🗑️ 고객님의 자택 도어락 정보가 즉시 영구 파기(삭제)되었습니다.", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("🗑️ 도어락 정보 즉시 수동 파기", fontSize = 10.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        Text("🔒 AES-256 금융급 암호화 보관 중 ('출입키 암호 해제' 클릭 시 확인 가능)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            // 📍 동선 단계 실시간 업데이트 버튼
                            Text("📍 실시간 동선 상태 변경 스위치", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "1. 픽업 출동 (운행 시작) 🚗",
                                    "2. 자택 도착 및 아이 안심 픽업 완료 🐶",
                                    "3. 병원 도착 및 진료 대행 접수 🏥",
                                    "4. 진료 완료 & 영수증/처방전 등록 🧾",
                                    "5. 귀가 이송 출발 🏠",
                                    "6. 안전 귀가 완료 (비번 자동 파기) 🎉"
                                ).forEach { statusStep ->
                                    val isSelected = order.status.contains(statusStep.take(2))
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (statusStep.contains("2. 자택 도착")) {
                                                selectedOrderForHandOff = order
                                            } else if (statusStep.contains("4. 진료 완료 & 영수증")) {
                                                selectedOrderForReport = order
                                            } else if (statusStep.contains("6. 안전 귀가 완료")) {
                                                val updatedOrder = order.copy(
                                                    status = "6. 안전 귀가 완료 🎉",
                                                    isCompleted = true,
                                                    doorlockPurged = true,
                                                    encryptedDoorlock = null,
                                                    doorlockPhotoUri = null
                                                )
                                                driverOrders[index] = updatedOrder
                                                PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                                                FirebaseSyncManager.updateOrderStatus(context, order.id, "6. 안전 귀가 완료 🎉", order.petName)
                                                FirebaseSyncManager.purgeDoorlockFromCloud(order.id)
                                                FirebaseSyncManager.syncOrderToCloud(updatedOrder)
                                                sendSystemPushNotification(context, "🎉 [발자국동행] 안전 귀가 완료 & 콜 마감", "[${order.petName}] 아이의 안심 이송이 완료되었습니다. 도어락 비번 등 개인정보가 100% 영구 파기되었으며 가드 바디캠이 종료되었습니다.")
                                                Toast.makeText(context, "🔒 안심 귀가 완료: 도어락 비번이 영구 파기되었으며 가드 바디캠이 자동 OFF 종료되었습니다.", Toast.LENGTH_LONG).show()
                                            } else {
                                                val updatedOrder = order.copy(status = statusStep)
                                                driverOrders[index] = updatedOrder
                                                PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                                                FirebaseSyncManager.updateOrderStatus(context, order.id, statusStep, order.petName)
                                                FirebaseSyncManager.syncOrderToCloud(updatedOrder)

                                                if (statusStep.contains("1. 픽업 출동")) {
                                                    sendSystemPushNotification(context, "🚗 [발자국동행] 픽업 출동 출발 / 운행 시작", "[${order.petName}] 아이를 만나러 보호자님 댁으로 안심 레이 케어카가 출발했습니다.")
                                                    Toast.makeText(context, "🚗 픽업 출동 및 운행 시작 알림이 보호자님께 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else if (statusStep.contains("3. 병원 도착")) {
                                                    sendSystemPushNotification(context, "🏥 [발자국동행] 동물병원 안전 도착", "[${order.petName}] ${order.hospitalName}에 안전하게 도착하여 진료 대행 접수 중입니다.")
                                                    Toast.makeText(context, "🏥 병원 도착 알림이 보호자님께 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else if (statusStep.contains("5. 귀가 이송")) {
                                                    sendSystemPushNotification(context, "🏠 [발자국동행] 귀가 이송 출발", "[${order.petName}] 진료 및 처방약 수령을 마치고 안전하게 자택으로 귀가 이동 중입니다.")
                                                    Toast.makeText(context, "🏠 귀가 이송 출발 알림이 보호자님께 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    sendSystemPushNotification(context, "📹 [발자국동행] 바디캠 자동 가동 ON", "[${order.petName}] 아이 이송 중: 가드 바디캠 라이브 송출 가동 ON")
                                                    Toast.makeText(context, "📹 오더 수행 중: 바디캠 라이브 송출 가동 ON", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        label = { Text(statusStep, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // ✍️ 안심 서명, 🗺️ 이동 지도, 💬 톡, 🧾 영수증/소견 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { selectedOrderForHandOff = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                ) {
                                    Text("✍️ 서명", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { selectedOrderForGpsMap = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("🗺️ 지도", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { selectedOrderForChat = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("💬 톡", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { selectedOrderForQuickNotify = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                ) {
                                    Text("📱 알림", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { selectedOrderForReport = order },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("🧾 영수증/소견", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color.White)
                                }
                            }
                            if (order.isCompleted && order.returnPhotoUri.isNullOrBlank()) {
                                Button(
                                    onClick = { selectedOrderForReturnPhoto = order },
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("📸 귀가 안심 인증샷 업로드", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            } else if (order.isCompleted && !order.returnPhotoUri.isNullOrBlank()) {
                                Text("✓ 귀가 안심 인증샷 업로드 완료", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GuardMapScreen()
        }
    }
}

    // 실시간 GPS 지도 다이얼로그
    selectedOrderForGpsMap?.let { order ->
        LiveGpsMapDialog(booking = order, onDismiss = { selectedOrderForGpsMap = null })
    }

    // 보호자 톡 다이얼로그 (가드 화면)
    selectedOrderForChat?.let { order ->
        DriverChatDialog(booking = order, isGuardSide = true, onDismiss = { selectedOrderForChat = null })
    }

    // 수의사 진료 소견 및 정산 영수증/처방전 작성 팝업 (가드 전용)
    selectedOrderForReport?.let { order ->
        var doctorNoteText by remember { mutableStateOf(order.doctorNote) }
        var prescriptionText by remember { mutableStateOf(order.prescription) }

        val initialReceiptUri: Uri? = order.receiptImageUri?.let { try { it.toUri() } catch(_: Exception) { null } }
        var uploadedReceiptUri by remember { mutableStateOf(initialReceiptUri) }
        val initialClinicalNoteUri: Uri? = order.clinicalNoteImageUri?.let { try { it.toUri() } catch(_: Exception) { null } }
        var uploadedClinicalNoteUri by remember { mutableStateOf(initialClinicalNoteUri) }
        val initialPrescriptionUri: Uri? = order.prescriptionImageUri?.let { try { it.toUri() } catch(_: Exception) { null } }
        var uploadedPrescriptionUri by remember { mutableStateOf(initialPrescriptionUri) }

        val receiptPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> if (uri != null) uploadedReceiptUri = uri }

        val clinicalNotePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> if (uri != null) uploadedClinicalNoteUri = uri }

        val prescriptionPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> if (uri != null) uploadedPrescriptionUri = uri }

        AlertDialog(
            onDismissRequest = { selectedOrderForReport = null },
            title = {
                Text("📄 수의사 소견 및 영수증/처방전 업로드", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("환묘/환견: ${order.petName} (${order.hospitalName})", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = doctorNoteText,
                        onValueChange = { doctorNoteText = it },
                        label = { Text("담당 수의사 종합 소견 (선택 - 사진 첨부 시 생략)") },
                        placeholder = { Text("예: 슬개골 2기 소견으로 당분간 계단 이용 자제 권장.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = prescriptionText,
                        onValueChange = { prescriptionText = it },
                        label = { Text("처방약 및 홈케어 가이드 (선택 - 사진 첨부 시 생략)") },
                        placeholder = { Text("예: 관절 영양제 30일분 처방 완료.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    HorizontalDivider()

                    // 1. 동물병원 영수증 촬영/첨부 버튼
                    Text("🧾 동물병원 정산 영수증 첨부 (보호자 펫보험 청구용)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { receiptPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🧾 영수증 사진 선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (uploadedReceiptUri != null) {
                            Text("✓ 첨부됨", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                    }

                    // 2. 수의사 진료 기록지 / 소견서 첨부 버튼
                    Text("📄 진료 기록지 / 소견서 첨부", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { clinicalNotePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📄 소견서 사진 선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (uploadedClinicalNoteUri != null) {
                            Text("✓ 첨부됨", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. 처방전 / 약 봉투 촬영/첨부 버튼
                    Text("💊 처방전 / 약 봉투 사진 첨부", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { prescriptionPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💊 처방전 사진 선택", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (uploadedPrescriptionUri != null) {
                            Text("✓ 첨부됨", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentReportOrder = selectedOrderForReport
                        if (currentReportOrder != null) {
                            val updatedOrder = currentReportOrder.copy(
                                status = "4. 진료 완료 & 수의사 리포트 전송 완료 🎉",
                                doctorNote = doctorNoteText.ifEmpty { "진료 결과 이상 소견 없이 건강한 상태입니다." },
                                prescription = prescriptionText.ifEmpty { "처방약 및 안심 홈케어 안내 전달 완료." },
                                receiptImageUri = uploadedReceiptUri?.toString() ?: currentReportOrder.receiptImageUri,
                                clinicalNoteImageUri = uploadedClinicalNoteUri?.toString() ?: currentReportOrder.clinicalNoteImageUri,
                                prescriptionImageUri = uploadedPrescriptionUri?.toString() ?: currentReportOrder.prescriptionImageUri,
                                isCompleted = true
                            )

                            // 1. 가드 대시보드 리스트 업데이트
                            val index = driverOrders.indexOfFirst { it.id == currentReportOrder.id }
                            if (index != -1) {
                                driverOrders[index] = updatedOrder
                            } else {
                                driverOrders.add(0, updatedOrder)
                            }

                            // 2. 디스크 저장소에 100% 영구 저장 및 서버 동기화!
                            PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                            FirebaseSyncManager.syncOrderToCloud(updatedOrder, context)

                            // 3. 실시간 푸시 알림 발송
                            ForPetFirebaseMessagingService.sendLocalNotification(
                                context,
                                "📄 [발자국동행] 수의사 진료 리포트 & 영수증 도착!",
                                "[${currentReportOrder.petName}] 아이의 진료 리포트 및 영수증 사진이 전송되었습니다."
                            )

                            Toast.makeText(context, "📤 수의사 진료 리포트 및 영수증/처방전 사진이 클라우드에 영구 저장되었으며 보호자 앱으로 즉시 전송되었습니다!", Toast.LENGTH_LONG).show()
                            selectedOrderForReport = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("리포트 전송 및 영구 저장 ➔", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForReport = null }) {
                    Text("취소")
                }
            }
        )
    }

    // 📸 귀가 안심 인증샷 업로드 다이얼로그 (가드용)
    selectedOrderForReturnPhoto?.let { order ->
        var uploadedReturnPhotoUri by remember { mutableStateOf<Uri?>(null) }
        val returnPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> if (uri != null) uploadedReturnPhotoUri = uri }

        AlertDialog(
            onDismissRequest = { selectedOrderForReturnPhoto = null },
            title = { Text("📸 귀가 안심 인증샷 업로드", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("아이를 안전하게 집 안에 들여보낸 후 사진을 찍어 보호자님께 전송해주세요.", fontSize = 12.sp, color = Color.Gray)
                    Button(
                        onClick = { returnPhotoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("사진 선택 / 촬영하기", fontWeight = FontWeight.Bold)
                    }
                    if (uploadedReturnPhotoUri != null) {
                        Text("✓ 사진이 첨부되었습니다.", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedOrder = order.copy(returnPhotoUri = uploadedReturnPhotoUri?.toString())
                        val index = driverOrders.indexOfFirst { it.id == order.id }
                        if (index != -1) driverOrders[index] = updatedOrder
                        PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                        FirebaseSyncManager.syncOrderToCloud(updatedOrder, context)
                        Toast.makeText(context, "📸 귀가 안심 인증샷이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                        selectedOrderForReturnPhoto = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("안심 인증샷 전송 ➔", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForReturnPhoto = null }) {
                    Text("취소")
                }
            }
        )
    }

    // ✍️ 2번: 현장 아이 안심 인계 '전자 서명' & OTP 안심 핀코드 인증 다이얼로그 호출
    selectedOrderForHandOff?.let { order ->
        PetHandOffSignatureDialog(
            booking = order,
            onConfirm = {
                val updatedStatus = "2. 자택 도착 및 아이 안심 픽업 완료 (서명 인증 됨) 🐶"
                val updatedOrder = order.copy(status = updatedStatus)
                val idx = driverOrders.indexOfFirst { it.id == order.id }
                if (idx != -1) {
                    driverOrders[idx] = updatedOrder
                }
                PersistentStorageManager.saveBookingList(context, driverOrders, "DRIVER_ALL")
                FirebaseSyncManager.updateOrderStatus(context, order.id, updatedStatus, order.petName)
                FirebaseSyncManager.syncOrderToCloud(updatedOrder)
                ForPetFirebaseMessagingService.sendLocalNotification(
                    context,
                    "✍️ [안심 인계 서명 완료] ${order.petName}",
                    "현장 전자 서명 인증이 완료되어 레이 케어카로 안전하게 탑승했습니다."
                )
                Toast.makeText(context, "✍️ '${order.petName}' 아이의 안심 인계 서명이 정상 등록되었습니다!", Toast.LENGTH_SHORT).show()
                selectedOrderForHandOff = null
            },
            onDismiss = { selectedOrderForHandOff = null }
        )
    }

    // 📱 5번: 가드 전용 '원터치 안심 문자 / 알림톡' 발송 다이얼로그 호출
    selectedOrderForQuickNotify?.let { order ->
        GuardQuickNotificationDialog(
            order = order,
            onDismiss = { selectedOrderForQuickNotify = null }
        )
    }
}

// 📱 가드 전용 '원터치 안심 문자 / 알림톡' 발송 팝업
@Composable
fun GuardQuickNotificationDialog(
    order: BookingItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val phone = remember(order.ownerName) {
        val regex = Regex("""010[- ]?\d{4}[- ]?\d{4}""")
        regex.find(order.ownerName)?.value?.replace("-", "")?.replace(" ", "") ?: "01012345678"
    }

    val templates = listOf(
        "🚗 픽업 출발" to "[발자국동행] ${order.petName} 아이 픽업을 위해 기아 레이 전용차가 자택으로 출발했습니다. (예상 15분 후 도착)",
        "🐾 픽업 완료" to "[발자국동행] ${order.petName} 아이를 안전하게 인계받아 전용 카시트에 탑승했습니다. ${order.hospitalName}(으)로 이동 중이며 앱에서 실시간 바디캠 라이브를 보실 수 있습니다.",
        "🏥 병원 도착" to "[발자국동행] ${order.hospitalName}에 안전하게 도착하여 수의사 진료 접수를 마쳤습니다. 진료 상황을 세심하게 체크하겠습니다.",
        "🩺 진료 완료" to "[발자국동행] ${order.petName} 아이의 진료 및 처방약 수령이 완료되었습니다. 상세 소견서가 앱에 등록되었으며, 곧 귀가 출발합니다.",
        "🎉 귀가 완료" to "[발자국동행] ${order.petName} 아이가 무사히 자택에 도착하여 안심 인계되었습니다. 오늘 아이와 함께할 수 있어 감사했습니다!"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📱", fontSize = 20.sp)
                    Text("원터치 안심 문자 / 알림톡", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text("수신: ${order.ownerName} ($phone)", fontSize = 11.sp, color = Color(0xFF0284C7))
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("운전 중 타이핑 없이 버튼 한 번으로 보호자에게 상황별 알림을 전송합니다.", fontSize = 11.sp, color = Color.Gray)
                }

                items(templates.size) { idx ->
                    val (stage, text) = templates[idx]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(stage, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text(text, fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 15.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val chatMsg = ChatMessage(
                                            senderName = "박찬헌 가드",
                                            isDriver = true,
                                            messageText = text,
                                            timestamp = java.text.SimpleDateFormat("a hh:mm", java.util.Locale.KOREA).format(java.util.Date())
                                        )
                                        com.example.forpetapp.data.firebase.FirebaseSyncManager.sendChatMessage(order.id, chatMsg)
                                        sendSystemPushNotification(context, "📱 [발자국동행 $stage]", text)
                                        Toast.makeText(context, "💬 보호자 안심 톡으로 자동 발송되었습니다!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Text("💬 안심 톡 전송", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:$phone")
                                                putExtra("sms_body", text)
                                            }
                                            context.startActivity(smsIntent)
                                        } catch (_: Exception) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "안심 문자 발송"))
                                        }
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                ) {
                                    Text("📱 문자(SMS) 발송", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun GuardMapScreen() {
    val context = LocalContext.current
    val pref = remember { context.getSharedPreferences("BaljagukAppDiskPrefs", android.content.Context.MODE_PRIVATE) }
    val carLat = remember {
        pref.getString("guard_last_lat", null)?.toDoubleOrNull() ?: 35.0830000
    }
    val carLng = remember {
        pref.getString("guard_last_lng", null)?.toDoubleOrNull() ?: 128.8950000
    }

    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var selectedLocationKey by remember { mutableStateOf("all") }

    // 🛡️ 컴포저블 이탈 시 WebView 메모리 해제 및 백그라운드 렌더러 크래시 100% 방지
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    removeAllViews()
                    destroy()
                }
            } catch (_: Exception) {}
            webViewRef = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 관제 헤더 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🗺️ 가드 전용 4대 병원 실시간 관제 지도", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text("🟢 관리자 지도 100% 연동", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Text("관리자 PC 관제 포털과 동일한 Google Maps HD 타일 및 4대 거점 동물병원 실시간 좌표가 동기화됩니다.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }

        // 빠른 핀 이동 선택 칩
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = (selectedLocationKey == "car"),
                onClick = {
                    selectedLocationKey = "car"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('car');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🚙 레이 케어카", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp1"),
                onClick = {
                    selectedLocationKey = "hosp1"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp1');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 덕천 동행", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp2"),
                onClick = {
                    selectedLocationKey = "hosp2"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp2');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 연산 연산", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp3"),
                onClick = {
                    selectedLocationKey = "hosp3"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp3');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 하단 이솔", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp4"),
                onClick = {
                    selectedLocationKey = "hosp4"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp4');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 하단오거리", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp5"),
                onClick = {
                    selectedLocationKey = "hosp5"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp5');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 명지 이로운", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp6"),
                onClick = {
                    selectedLocationKey = "hosp6"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp6');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 양산 에스", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp7"),
                onClick = {
                    selectedLocationKey = "hosp7"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp7');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 양산 힐스", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "hosp8"),
                onClick = {
                    selectedLocationKey = "hosp8"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('hosp8');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🏥 동래 리본", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = (selectedLocationKey == "all"),
                onClick = {
                    selectedLocationKey = "all"
                    try {
                        webViewRef?.evaluateJavascript("focusLocation('all');", null)
                    } catch (_: Exception) {}
                },
                label = { Text("🗺️ 전체 보기", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // 고화질 실시간 관제 구글 지도 뷰어
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            @android.annotation.SuppressLint("SetJavaScriptEnabled")
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                android.util.Log.d("GuardMapWebView", "${consoleMessage?.message()} (Line ${consoleMessage?.lineNumber()})")
                                return true
                            }
                        }
                        webViewClient = object : android.webkit.WebViewClient() {
                            // 🛡️ 크로미움 렌더러 프로세스 비정상 종료 시 앱 전체 튕김 100% 방어
                            override fun onRenderProcessGone(view: android.webkit.WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                try {
                                    view?.let {
                                        (it.parent as? android.view.ViewGroup)?.removeView(it)
                                        it.destroy()
                                    }
                                } catch (_: Exception) {}
                                return true
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                try {
                                    view?.evaluateJavascript("if (typeof initMapEngine === 'function') { initMapEngine(); } updateCarGps($carLat, $carLng);", null)
                                } catch (_: Exception) {}
                            }

                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (url.startsWith("tel:")) {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, url.toUri())
                                        ctx.startActivity(dialIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(ctx, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl("file:///android_asset/guard_map.html")
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
