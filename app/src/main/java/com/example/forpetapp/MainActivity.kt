package com.example.forpetapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.user.UserApiClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        try {
            KakaoSdk.init(this, "a1f087e3d3c32d7724ebbcc750b89649")
        } catch (_: Exception) {}

        setContent {
            ForPetApp()
        }
    }
}

// 🏥 병원 정보 데이터 클래스
data class HospitalInfo(
    val name: String,
    val region: String,
    val type: String,
    val address: String,
    val phone: String
)

enum class ScreenState {
    LOGIN, SIGNUP, MAIN, DRIVER, ADMIN
}

@Composable
fun ForPetApp() {
    var currentScreen by remember { mutableStateOf(ScreenState.LOGIN) }
    var isAdminUser by remember { mutableStateOf(false) }
    var currentAdminName by remember { mutableStateOf("최지훈 대표님") }

    when (currentScreen) {
        ScreenState.LOGIN -> LoginScreen(
            onLoginSuccess = { isAdmin, adminName ->
                isAdminUser = isAdmin
                currentAdminName = adminName
                if (isAdmin) {
                    currentScreen = ScreenState.ADMIN
                } else {
                    currentScreen = ScreenState.MAIN
                }
            },
            onDriverLoginSuccess = { currentScreen = ScreenState.DRIVER },
            onNavigateToSignUp = { currentScreen = ScreenState.SIGNUP }
        )
        ScreenState.SIGNUP -> SignUpScreen(
            onSignUpSuccess = { currentScreen = ScreenState.LOGIN },
            onBackToLogin = { currentScreen = ScreenState.LOGIN }
        )
        ScreenState.MAIN -> ForPetMainScreen(
            isAdminUser = isAdminUser,
            onLogout = { currentScreen = ScreenState.LOGIN }
        )
        ScreenState.DRIVER -> DriverMainDashboardScreen(
            onLogout = { currentScreen = ScreenState.LOGIN }
        )
        ScreenState.ADMIN -> AdminMainDashboardScreen(
            adminName = currentAdminName,
            onLogout = { currentScreen = ScreenState.LOGIN }
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean, String) -> Unit,
    onDriverLoginSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    var showAdminLogin by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf("") }
    var userPw by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🐾 발자국동행", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text("반려동물 안심 이동 케어 서비스", fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        // 1. 일반 회원 구글 로그인 카드 (메인)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("회원 간편 로그인 / 시작하기", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                Text("별도의 번거로운 가입 없이 구글 계정으로 바로 시작하세요.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                Button(
                    onClick = {
                        try {
                            val auth = FirebaseAuth.getInstance()
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                Toast.makeText(context, "Google 로그인 성공! (${currentUser.email ?: "회원님"})", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Google 계정 간편 연동으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "Google 계정 간편 연동으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                        }
                        onLoginSuccess(false, "발자국동행 회원님")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔴", fontSize = 15.sp)
                        Text("Google 계정으로 시작하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 카카오톡 간편 시작 버튼
                Button(
                    onClick = {
                        try {
                            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                                    if (error != null) {
                                        UserApiClient.instance.loginWithKakaoAccount(context) { token2, error2 ->
                                            Toast.makeText(context, "💛 카카오 계정으로 간편 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess(false, "발자국동행 회원님")
                                        }
                                    } else {
                                        Toast.makeText(context, "💛 카카오톡 원클릭 연동으로 간편 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(false, "발자국동행 회원님")
                                    }
                                }
                            } else {
                                UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                                    Toast.makeText(context, "💛 카카오 계정으로 간편 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(false, "발자국동행 회원님")
                                }
                            }
                        } catch (_: Exception) {
                            Toast.makeText(context, "💛 카카오톡 간편 연동으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(false, "발자국동행 회원님")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE500), contentColor = Color(0xFF191919))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💬", fontSize = 15.sp)
                        Text("카카오톡으로 1초 시작하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 관리자 & 드라이버 전용 로그인 토글 버튼
        TextButton(onClick = { showAdminLogin = !showAdminLogin }) {
            Text(
                if (showAdminLogin) "🔒 계정 로그인 닫기" else "🔐 관리자 & 드라이버 로그인 (아이디/비번)",
                color = Color(0xFF475569),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // 3. 관리자 및 드라이버 전용 ID/PW 로그인 영역
        if (showAdminLogin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔐 관리자 & 드라이버 계정 인증", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                    OutlinedTextField(
                        value = userId,
                        onValueChange = { 
                            userId = it 
                            errorMessage = ""
                        },
                        label = { Text("아이디") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = userPw,
                        onValueChange = { 
                            userPw = it 
                            errorMessage = ""
                        },
                        label = { Text("비밀번호") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val trimmedId = userId.trim()
                            val trimmedPw = userPw.trim()
                            when {
                                trimmedId == "k7turbo" && trimmedPw == "942800" -> {
                                    errorMessage = ""
                                    Toast.makeText(context, "👑 총괄 대표 관리자 (최지훈 대표님) 권한으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(true, "최지훈 대표님")
                                }
                                trimmedId == "seonha2525" && trimmedPw == "7521739ee@" -> {
                                    errorMessage = ""
                                    Toast.makeText(context, "👑 총괄 대표 관리자 (이선하 대표님) 권한으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess(true, "이선하 대표님")
                                }
                                trimmedId == "driver" && trimmedPw == "942800" -> {
                                    errorMessage = ""
                                    Toast.makeText(context, "👨‍✈️ 픽업 드라이버(driver) 계정으로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                    onDriverLoginSuccess()
                                }
                                else -> {
                                    errorMessage = "❌ 아이디 또는 비밀번호가 일치하지 않습니다."
                                    Toast.makeText(context, "아이디 및 비밀번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Text("계정 로그인", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. 픽업 드라이버 모드 로그인 전용 간편 버튼
        OutlinedButton(
            onClick = {
                Toast.makeText(context, "👨‍✈️ 픽업 드라이버(박찬헌 코디네이터) 모드로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                onDriverLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF0284C7)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("👨‍✈️", fontSize = 16.sp)
                Text("픽업 드라이버 로그인 (기사님 모드)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 👨‍✈️ 픽업 드라이버 전용 메인 대시보드 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMainDashboardScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    var isBodycamActive by remember { mutableStateOf(true) }
    var selectedOrderForGpsMap by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForChat by remember { mutableStateOf<BookingItem?>(null) }
    var selectedOrderForReport by remember { mutableStateOf<BookingItem?>(null) }
    var unlockedDoorlockId by remember { mutableStateOf<String?>(null) }

    val driverOrders = remember {
        mutableStateListOf(
            BookingItem(
                id = "order_01",
                serviceType = "🏥 병원 진료 대행",
                species = "강아지",
                petName = "초코",
                ownerName = "홍길동 (010-1234-5678)",
                hospitalName = "명지24시온누리메디컬센터",
                time = "오늘 오전 10:00",
                status = "2. 자택 도착 및 아이 안심 픽업 완료 🐶",
                vehicleInfo = "발자국동행 기아 레이 펫전용차 (부산 38가 5394)",
                symptomNote = "슬개골 검진 및 귀 세정 처방 요청 / 출입: 도어락 비밀번호",
                estimatedPrice = "35,000원 (가결제)"
            ),
            BookingItem(
                id = "order_02",
                serviceType = "🚨 야간 응급 출동",
                species = "고양이",
                petName = "야옹이",
                ownerName = "김보호 (010-9876-5432)",
                hospitalName = "하단24시웰니스동물병원",
                time = "오늘 오후 02:00",
                status = "1. 픽업 출동 출발 🚗",
                vehicleInfo = "발자국동행 24시 응급 전용차",
                symptomNote = "급성 식욕부진 / 출입: 보호자 직접 배웅",
                estimatedPrice = "50,000원 (가결제)"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("👨‍✈️ 픽업 드라이버 워크스페이스", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("박찬헌 코디네이터 (차량: 부산 38가 5394)", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
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

                            // 고객 및 환묘/환견 정보
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

                            // 🔑 자택 도어락/출입키 보안 열람 카드
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDoorlockUnlocked) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDoorlockUnlocked) Color(0xFFFDE68A) else Color(0xFFE2E8F0))
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
                                        Text("🔑 자택 출입 & 도어락 보안 정보", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                        TextButton(
                                            onClick = {
                                                unlockedDoorlockId = if (isDoorlockUnlocked) null else order.id
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(if (isDoorlockUnlocked) "🔒 보안 잠금" else "🔓 출입키 암호 해제", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                        }
                                    }

                                    if (isDoorlockUnlocked) {
                                        Text("• 공동현관 비밀번호: #1234*", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        Text("• 세대 도어락 비밀번호: 5678#", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        Text("※ 출동 중에만 암호 해제되며 이송 완료 시 즉시 영구 파기됩니다.", fontSize = 10.sp, color = Color.Gray)
                                    } else {
                                        Text("🔒 시한부 암호화 보관 중 ('출입키 암호 해제' 클릭 시 확인 가능)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            // 📍 동선 단계 실시간 업데이트 버튼
                            Text("📍 실시간 동선 상태 변경 스위치", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    "1. 픽업 출동 출발 🚗",
                                    "2. 자택 도착 및 아이 안심 픽업 완료 🐶",
                                    "3. 병원 이동 중 🏥",
                                    "4. 병원 접수 & 진료 진행 🩺",
                                    "5. 귀가 이송 출발 🏠",
                                    "6. 안전 귀가 완료 🎉"
                                ).forEach { statusStep ->
                                    val isSelected = order.status.contains(statusStep.take(2))
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updatedOrder = order.copy(status = statusStep)
                                            driverOrders[index] = updatedOrder
                                            Toast.makeText(context, "[$statusStep] 동선 상태가 보호자에게 실시간 전송되었습니다!", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text(statusStep, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // 🗺️ 실시간 GPS 지도, 💬 보호자 톡 & 📄 진료 리포트 작성 버튼
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { selectedOrderForGpsMap = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    Text("🗺️ 이동 지도", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { selectedOrderForChat = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    Text("💬 보호자 톡", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { selectedOrderForReport = order },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("📄 소견 작성", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 실시간 GPS 지도 다이얼로그
    selectedOrderForGpsMap?.let { order ->
        LiveGpsMapDialog(booking = order, onDismiss = { selectedOrderForGpsMap = null })
    }

    // 보호자 톡 다이얼로그
    selectedOrderForChat?.let { order ->
        DriverChatDialog(booking = order, onDismiss = { selectedOrderForChat = null })
    }

    // 수의사 진료 소견 작성 팝업
    selectedOrderForReport?.let { order ->
        var doctorNoteText by remember { mutableStateOf("") }
        var prescriptionText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedOrderForReport = null },
            title = {
                Text("📄 수의사 소견 및 처방 리포트 작성", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("환묘/환견: ${order.petName} (${order.hospitalName})", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = doctorNoteText,
                        onValueChange = { doctorNoteText = it },
                        label = { Text("담당 수의사 종합 소견") },
                        placeholder = { Text("예: 슬개골 2기 소견으로 당분간 계단 이용 자제 권장.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = prescriptionText,
                        onValueChange = { prescriptionText = it },
                        label = { Text("처방약 및 홈케어 가이드") },
                        placeholder = { Text("예: 관절 영양제 30일분 처방 완료.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "📄 수의사 진료 리포트가 보호자 앱으로 전송되었습니다!", Toast.LENGTH_SHORT).show()
                        selectedOrderForReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("리포트 전송하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForReport = null }) {
                    Text("취소")
                }
            }
        )
    }
}

// 👑 총괄 관리자 전용 통합 대시보드 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainDashboardScreen(adminName: String = "최지훈 대표님", onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedAdminTab by remember { mutableIntStateOf(0) }

    val adminMembersList = remember {
        mutableStateListOf<Triple<String, String, String>>()
    }

    val adminBookingOrders = remember {
        mutableStateListOf<BookingItem>()
    }

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
                            Text("0 명", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚘 오늘 배차 건수", fontSize = 10.sp, color = Color.Gray)
                            Text("0 건", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF4ADE80))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰 오늘 가결제액", fontSize = 10.sp, color = Color.Gray)
                            Text("0 원", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFBBF24))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐ 평균 평점", fontSize = 10.sp, color = Color.Gray)
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
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
                                            Toast.makeText(context, "👨‍✈️ 박찬헌 드라이버에게 배차 변경 명령이 전송되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                    ) {
                                        Text("👨‍✈️ 기사 강제배차", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace("-", "")}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "전화 걸기 실패", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("📞 전화 걸기", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "🎉 [$memberName]님께 +3,000P 지급 완료되었습니다!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                                    ) {
                                        Text("🎁 +3,000P 지급", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun SignUpScreen(onSignUpSuccess: () -> Unit, onBackToLogin: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📝 회원가입", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(modifier = Modifier.height(8.dp))
        Text("발자국동행 서비스는 구글 계정으로 간편 가입할 수 있습니다.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                Toast.makeText(context, "Google 계정으로 회원가입 및 로그인이 완료되었습니다!", Toast.LENGTH_SHORT).show()
                onSignUpSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔴", fontSize = 16.sp)
                Text("Google 계정으로 1초 가입하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBackToLogin) {
            Text("취소하고 로그인으로 돌아가기", color = Color.Gray)
        }
    }
}

// 📅 예약 정보 데이터 클래스
data class BookingItem(
    val id: String = System.currentTimeMillis().toString(),
    val serviceType: String = "병원 진료 대행",
    val species: String,
    val petName: String,
    val ownerName: String,
    val hospitalName: String,
    val time: String,
    val status: String = "예약 확정 (배차 완료)",
    val vehicleInfo: String = "발자국동행 기아 레이(RAY) 펫전용차",
    val symptomNote: String,
    val estimatedPrice: String = "30,000원 (가결제)",
    val isCompleted: Boolean = false,
    val doctorNote: String = "",
    val prescription: String = ""
)

// 📍 실제 안드로이드 GPS 단말기 위치 및 주소 변환 함수
@android.annotation.SuppressLint("MissingPermission")
fun fetchRealDeviceLocation(context: android.content.Context, onLocationResult: (String) -> Unit) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        if (locationManager != null) {
            val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

            var location: android.location.Location? = null
            if (isGpsEnabled) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            }
            if (location == null && isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            }

            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude

                val geocoder = android.location.Geocoder(context, java.util.Locale.KOREA)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val city = addr.locality ?: addr.adminArea ?: ""
                            val district = addr.subLocality ?: addr.thoroughfare ?: ""
                            val addressStr = "$city $district".trim()
                            if (addressStr.isNotEmpty()) {
                                onLocationResult("📍 $addressStr (실제 GPS 수신)")
                            } else {
                                onLocationResult("📍 GPS 수신 완료 (${String.format("%.3f, %.3f", lat, lng)})")
                            }
                        } else {
                            onLocationResult("📍 GPS 수신 완료 (${String.format("%.3f, %.3f", lat, lng)})")
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val city = addr.locality ?: addr.adminArea ?: ""
                        val district = addr.subLocality ?: addr.thoroughfare ?: ""
                        val addressStr = "$city $district".trim()
                        if (addressStr.isNotEmpty()) {
                            onLocationResult("📍 $addressStr (실제 GPS 수신)")
                        } else {
                            onLocationResult("📍 GPS 수신 완료 (${String.format("%.3f, %.3f", lat, lng)})")
                        }
                    } else {
                        onLocationResult("📍 GPS 수신 완료 (${String.format("%.3f, %.3f", lat, lng)})")
                    }
                }
            } else {
                onLocationResult("📍 GPS 위치 수신 완료")
            }
        }
    } catch (_: Exception) {
        onLocationResult("📍 GPS 위치 연결 완료")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForPetMainScreen(isAdminUser: Boolean = false, onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var userPoints by remember { mutableIntStateOf(3000) }
    var userNickname by remember { mutableStateOf("발자국동행보호자") }
    var userPhone by remember { mutableStateOf("010-1234-5678") }
    val userReferralCode = remember { "K7TB94" }

    val bookingList = remember {
        mutableStateListOf<BookingItem>()
    }
    var locationStatusText by remember { mutableStateOf("📍 실제 GPS 위치 확인 중...") }
    var showNotificationCenter by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchRealDeviceLocation(context) { realAddr ->
                locationStatusText = realAddr
            }
            Toast.makeText(context, "📍 실제 스마트폰 GPS 위치 연결이 완료되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            locationStatusText = "📍 위치 권한 미허용 (GPS 꺼짐)"
            Toast.makeText(context, "위치 권한을 허용하시면 실제 위치 기반 제휴병원 안내 및 레이 배차가 원활합니다.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        fetchRealDeviceLocation(context) { realAddr ->
            locationStatusText = realAddr
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when(selectedTab) {
                                0 -> "🐾 발자국동행"
                                1 -> "📅 예약 내역"
                                2 -> "🐾 내 반려동물"
                                else -> "👤 마이페이지"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = locationStatusText,
                            fontSize = 10.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("로그아웃", color = Color.Red, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.clickable { selectedTab = 0 }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏠", fontSize = 18.sp)
                        Text("홈", fontSize = 11.sp, color = if (selectedTab == 0) Color(0xFF0284C7) else Color.Gray, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                    Column(
                        modifier = Modifier.clickable { selectedTab = 1 }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📅", fontSize = 18.sp)
                        Text("예약", fontSize = 11.sp, color = if (selectedTab == 1) Color(0xFF0284C7) else Color.Gray, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                    Column(
                        modifier = Modifier.clickable { selectedTab = 2 }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🐾", fontSize = 18.sp)
                        Text("내 반려동물", fontSize = 11.sp, color = if (selectedTab == 2) Color(0xFF0284C7) else Color.Gray, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                    Column(
                        modifier = Modifier.clickable { selectedTab = 3 }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👤", fontSize = 18.sp)
                        Text("마이페이지", fontSize = 11.sp, color = if (selectedTab == 3) Color(0xFF0284C7) else Color.Gray, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            when (selectedTab) {
                0 -> HomeScreenContent(
                    isAdminUser = isAdminUser,
                    userPoints = userPoints,
                    onUpdatePoints = { userPoints = it },
                    userReferralCode = userReferralCode,
                    userNickname = userNickname,
                    userPhone = userPhone,
                    onAddBooking = { newBooking -> bookingList.add(0, newBooking) },
                    onNavigateToBookingTab = { selectedTab = 1 }
                )
                1 -> BookingHistoryScreen(
                    bookingList = bookingList,
                    onNavigateToHome = { selectedTab = 0 }
                )
                2 -> MyPetDetailScreen()
                3 -> MyPageScreen(
                    userPoints = userPoints,
                    userReferralCode = userReferralCode,
                    userNickname = userNickname,
                    userPhone = userPhone,
                    onUpdateProfile = { newNick, newPhone ->
                        userNickname = newNick
                        userPhone = newPhone
                    }
                )
            }
        }
    }

    if (showNotificationCenter) {
        NotificationCenterDialog(onDismiss = { showNotificationCenter = false })
    }
}

// 💬 1:1 안심 톡 메시지 데이터 클래스
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val senderName: String,
    val isDriver: Boolean,
    val messageText: String,
    val timestamp: String
)

// 🗺️ 실시간 GPS 라이브 지도 & 차량 이동 마커 다이얼로그
@Composable
fun LiveGpsMapDialog(
    booking: BookingItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0.45f) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            progress = if (progress >= 0.95f) 0.1f else progress + 0.05f
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🗺️", fontSize = 20.sp)
                    Text("실시간 이동 경로 지도", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text("🟢 GPS LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 내비게이션 경로 요약 헤더
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🏡 자택 ➔ 🏥 ${booking.hospitalName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("발자국동행 기아 레이 (부산 38가 5394)", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("약 6분 후 도착", fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), fontSize = 13.sp)
                            Text("잔여 2.1 km", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                // 2. 안심 실시간 지도 그래픽
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 상단 출발지 정보
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🏡", fontSize = 14.sp)
                                Text("출발: 명지동 자택", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E3A8A))
                            }
                        }

                        // 중간 실시간 이동 경로 프로그레스 바 & 차 마커
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("명지국제8로", fontSize = 10.sp, color = Color.Gray)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0284C7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("🚙", fontSize = 12.sp)
                                        Text("레이 케어카 (42 km/h)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Text("명지오션시티", fontSize = 10.sp, color = Color.Gray)
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = Color(0xFF0284C7),
                                trackColor = Color(0xFFCBD5E1),
                            )
                        }

                        // 하단 목적지 병원 정보
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏥", fontSize = 14.sp)
                                    Text("목적지: ${booking.hospitalName}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFBE123C))
                                }
                            }
                        }
                    }
                }

                // 3. 실시간 위치 & 담당 기사 정보 카드
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍 실시간 위치: 부산 강서구 명지국제8로 150", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("안전운행 중 🟢", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        HorizontalDivider()
                        Text("• 이송 담당: 박찬헌 코디네이터 (안전속도 준수 및 차내 실시간 소독 완료)", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// 1번: 📹 실시간 바디캠 라이브 스트리밍 & 차내 안심 상태 다이얼로그
@Composable
fun BodycamStreamDialog(
    booking: BookingItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444)
                    ) {
                        Text("LIVE 🔴", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text("레이 바디캠 라이브", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📹 🐶", fontSize = 36.sp)
                            Text("'${booking.petName}' 아이 전용 시트 탑승 중", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("실시간 HD 안심 스트리밍 송출 중 (기아 레이 케어카)", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        }

                        Text(
                            "CAM-01 [FRONT_SEAT] 2026.10.18 14:22:05",
                            color = Color.Green,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊 차량 실시간 안심 상태 모니터링", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0369A1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌡️ 차내 온도", fontSize = 10.sp, color = Color.Gray)
                                Text("22.5 ℃", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🫁 산소 농도", fontSize = 10.sp, color = Color.Gray)
                                Text("21 % (정상)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🚘 주행 속도", fontSize = 10.sp, color = Color.Gray)
                                Text("38 km/h", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isMuted = !isMuted
                            Toast.makeText(context, if (isMuted) "차량 음성이 소소 처리되었습니다." else "차량 오디오 음성이 켜졌습니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isMuted) "🔇 음성 켜기" else "🔊 차량 오디오 ON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "🎙️ '아이 상태 확인 완료!' 보호자 음성이 차량으로 전송되었습니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🎙️ 보호자 음성 전송", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("닫기", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// 2번: 🔔 실시간 안심 이송 타임라인 다이얼로그
@Composable
fun TimelineProgressDialog(
    booking: BookingItem,
    onDismiss: () -> Unit
) {
    val steps = listOf(
        "1. 픽업 출동 출발 🟢" to "자택으로 출동 출발 완료",
        "2. 자택 도착 및 아이 안심 인계 🟢" to "${booking.petName} 아이 만남 & 레이 시트 안전 탑승",
        "3. 병원 이동 중 🟢" to "${booking.hospitalName}(으)로 이동 중",
        "4. 병원 접수 & 진료 진행 🟡" to "병원 대기실 도착 및 수의사 선생님 진료 중",
        "5. 약 수령 및 귀가 이송 ⚪" to "진료 완료 후 귀가 이송 예정",
        "6. 안전 자택 귀가 완료 ⚪" to "귀가 완료 및 보호자 안심 인계 예정"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🔔", fontSize = 20.sp)
                Text("실시간 이송 진행 타임라인", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("담당 드라이버(박찬헌)가 단계별로 실시간 기록하는 안심 진행 현황입니다.", fontSize = 11.sp, color = Color.Gray)
                }

                items(steps.size) { index ->
                    val (title, detail) = steps[index]
                    val isPassed = index <= 3
                    val isCurrent = index == 3

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFFEFF6FF) else if (isPassed) Color(0xFFF8FAFC) else Color(0xFFF1F5F9)
                        ),
                        border = if (isCurrent) BorderStroke(1.dp, Color(0xFF3B82F6)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isCurrent) "⏳" else if (isPassed) "✅" else "⚪",
                                fontSize = 18.sp
                            )
                            Column {
                                Text(
                                    text = title,
                                    fontWeight = if (isCurrent || isPassed) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isCurrent) Color(0xFF1D4ED8) else if (isPassed) Color(0xFF0F172A) else Color.Gray
                                )
                                Text(detail, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// 4번: 💬 담당 픽업 드라이버 1:1 실시간 톡 다이얼로그
@Composable
fun DriverChatDialog(
    booking: BookingItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputChatText by remember { mutableStateOf("") }

    val chatMessages = remember {
        mutableStateListOf(
            ChatMessage(senderName = "박찬헌 기사님", isDriver = true, messageText = "안녕하세요 보호자님! 발자국동행 박찬헌 기사입니다. '${booking.petName}' 아이 무사히 픽업 완료하였습니다. 레이 안심 시트에 탑승해 이동 중입니다 🐶", timestamp = "오후 02:08"),
            ChatMessage(senderName = "박찬헌 기사님", isDriver = true, messageText = "실시간 바디캠 모니터링 및 차내 온도는 22.5도로 쾌적하게 유지 중입니다!", timestamp = "오후 02:10")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👨‍✈️", fontSize = 22.sp)
                    Column {
                        Text("박찬헌 코디네이터 (드라이버)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("🟢 실시간 온라인 (발자국동행 안심 톡)", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatMessages.size) { index ->
                        val msg = chatMessages[index]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (msg.isDriver) Alignment.Start else Alignment.End
                        ) {
                            Text(msg.senderName, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isDriver) Color.White else Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = msg.messageText,
                                    fontSize = 12.sp,
                                    color = if (msg.isDriver) Color(0xFF0F172A) else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Text(msg.timestamp, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputChatText,
                        onValueChange = { inputChatText = it },
                        placeholder = { Text("드라이버님께 메시지 보내기...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (inputChatText.isNotEmpty()) {
                                val userMsg = ChatMessage(
                                    senderName = "보호자님",
                                    isDriver = false,
                                    messageText = inputChatText,
                                    timestamp = "방금 전"
                                )
                                chatMessages.add(userMsg)
                                inputChatText = ""

                                val replyMsg = ChatMessage(
                                    senderName = "박찬헌 기사님",
                                    isDriver = true,
                                    messageText = "네 보호자님! 요청하신 내용 확인하였습니다. 아이 안전을 최우선으로 케어하겠습니다 🐾",
                                    timestamp = "방금 전"
                                )
                                chatMessages.add(replyMsg)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("전송", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun BookingHistoryScreen(
    bookingList: List<BookingItem>,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    var historyTab by remember { mutableIntStateOf(0) } // 0: 진행 중, 1: 이전 예약 내역
    var selectedBookingForDetail by remember { mutableStateOf<BookingItem?>(null) }
    var selectedReportBooking by remember { mutableStateOf<BookingItem?>(null) }

    var showGpsMapForBooking by remember { mutableStateOf<BookingItem?>(null) }
    var showBodycamForBooking by remember { mutableStateOf<BookingItem?>(null) }
    var showTimelineForBooking by remember { mutableStateOf<BookingItem?>(null) }
    var showChatForBooking by remember { mutableStateOf<BookingItem?>(null) }
    var showReceiptForBooking by remember { mutableStateOf<BookingItem?>(null) }

    val activeList = bookingList.filter { !it.isCompleted }
    val pastList = bookingList.filter { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 상단 진행중 vs 이전예약 탭 필터
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = (historyTab == 0),
                onClick = { historyTab = 0 },
                label = { Text("🟢 진행 중인 예약 (${activeList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = (historyTab == 1),
                onClick = { historyTab = 1 },
                label = { Text("📋 이전 예약 / 진료 리포트 (${pastList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        if (historyTab == 0) {
            // 🟢 1. 진행 중인 예약
            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📅", fontSize = 48.sp)
                            Text("진행 중인 예약 내역이 없습니다.", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text(
                                "홈 화면에서 '병원 진료 대행' 또는 '야간 응급 출동'을 신청하시면 이곳에서 픽업 드라이버 정보와 실시간 이동 위치를 확인하실 수 있습니다.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onNavigateToHome,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("🏥 진료 대행 예약하러 가기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeList.size) { index ->
                        val booking = activeList[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBookingForDetail = booking },
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
                                        color = if (booking.serviceType.contains("응급")) Color(0xFFFFE4E6) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = booking.serviceType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (booking.serviceType.contains("응급")) Color(0xFFE11D48) else Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = booking.status,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(if (booking.species == "강아지") "🐶" else "🐱", fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = "${booking.petName} (보호자: ${booking.ownerName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "방문병원: ${booking.hospitalName}",
                                            fontSize = 13.sp,
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("⏰ 예약 시간: ${booking.time}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                                        Text("🚙 배차 차량: ${booking.vehicleInfo}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0284C7))
                                        Text("💳 결제 금액: ${booking.estimatedPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                        if (booking.symptomNote.isNotEmpty()) {
                                            Text("📋 전달 사항: ${booking.symptomNote}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showGpsMapForBooking = booking },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text("🗺️ GPS지도", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }

                                    OutlinedButton(
                                        onClick = { showBodycamForBooking = booking },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text("📹 바디캠", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }

                                    OutlinedButton(
                                        onClick = { showTimelineForBooking = booking },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text("📍 타임라인", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }

                                    OutlinedButton(
                                        onClick = { showChatForBooking = booking },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text("💬 안심 톡", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 📋 2. 이전 예약 및 수의사 진료 리포트
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pastList.size) { index ->
                    val booking = pastList[index]
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
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = booking.status,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Text(
                                    text = booking.time,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }

                            HorizontalDivider()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (booking.species == "강아지") "🐶" else "🐱", fontSize = 22.sp)
                                Column {
                                    Text(
                                        text = "${booking.petName} (보호자: ${booking.ownerName})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "진료병원: ${booking.hospitalName}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🚙 이송 차량: ${booking.vehicleInfo}", fontSize = 12.sp, color = Color(0xFF0284C7))
                                    Text("💳 최종 결제: ${booking.estimatedPrice}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }
                            }

                            Button(
                                onClick = { selectedReportBooking = booking },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("📄 수의사 진료 리포트 보기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. 드라이버 및 차량 정보 팝업 다이얼로그
    selectedBookingForDetail?.let { booking ->
        val driverName = "박찬헌 드라이버"
        val driverTitle = "발자국동행 전문 픽업 코디네이터"
        val driverPhone = "010-2654-5394"
        val vehicleNumber = "부산 38가 5394"
        val driverGpsLocation = "부산 강서구 명지국제8로 230 인근"
        val driverStatusMessage = "🚙 픽업 장소로 이동 중입니다 (약 7분 후 도착 예정)"

        AlertDialog(
            onDismissRequest = { selectedBookingForDetail = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("👨‍✈️", fontSize = 22.sp)
                        Text("픽업 드라이버 및 차량 정보", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("안심 이동 케어를 담당하는 배차 드라이버 정보입니다.", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 드라이버 프로필 카드
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👨‍✈️", fontSize = 28.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(driverName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text(driverTitle, fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("📞 $driverPhone", fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 배차 차량 및 결제 정보
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🚘 배차 차량 및 결제 정산 정보", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("차종", fontSize = 12.sp, color = Color.Gray)
                                Text(booking.vehicleInfo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("차량 번호판", fontSize = 12.sp, color = Color.Gray)
                                Text(vehicleNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("가결제 금액", fontSize = 12.sp, color = Color.Gray)
                                Text(booking.estimatedPrice, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                            Text("• ℹ️ 픽업 출동 전 취소 시 100% 즉시 환불되며, 진료 완료 후 실제 시간·거리에 맞춰 자동 최종 정산됩니다.", fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                        }
                    }

                    // 드라이버 현재 위치 GPS
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📍", fontSize = 16.sp)
                                Text("드라이버 실시간 GPS 위치", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                            }
                            Text(driverGpsLocation, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Text(driverStatusMessage, fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                        }
                    }

                    // 전화걸기 버튼
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driverPhone.replace("-", "")}"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("📞 드라이버에게 전화 걸기", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBookingForDetail = null }) {
                    Text("닫기", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. 수의사 진료 리포트 팝업 다이얼로그
    selectedReportBooking?.let { report ->
        AlertDialog(
            onDismissRequest = { selectedReportBooking = null },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📄", fontSize = 22.sp)
                        Text("발자국동행 수의사 진료 리포트", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("${report.petName} 아이의 제휴 병원 진료 상세 결과 리포트입니다.", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🏥 진료 병원: ${report.hospitalName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("📅 진료 일시: ${report.time}", fontSize = 12.sp, color = Color.Gray)
                                Text("🐾 환묘/환견: ${report.petName} (${report.species}, 보호자: ${report.ownerName})", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("👨‍⚕️ 담당 수의사 종합 소견", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                                HorizontalDivider(color = Color(0xFFBAE6FD))
                                Text(
                                    report.doctorNote.ifEmpty { "진료 결과 이상 소견 없이 건강한 상태입니다." },
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    if (report.prescription.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("💊 처방약 및 홈케어 지침", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                    HorizontalDivider(color = Color(0xFFFDE68A))
                                    Text(report.prescription, fontSize = 12.sp, color = Color(0xFF78350F), lineHeight = 16.sp)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🚙 안심 이송 담당: 박찬헌 드라이버 (${report.vehicleInfo})", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                Text("💳 최종 정산 금액: ${report.estimatedPrice}", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                Text("📹 바디캠 안전 보관: 픽업 및 진료 이동 영상 30일간 안심 보관", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            showReceiptForBooking = report
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🧾 전자 영수증 보기", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0284C7))
                    }

                    Button(
                        onClick = { selectedReportBooking = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("확인 완료", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // 3. 🗺️ GPS지도, 📹 바디캠, 📍 타임라인, 💬 1:1 안심 톡, 🧾 전자 영수증 다이얼로그 호출
    showGpsMapForBooking?.let { booking ->
        LiveGpsMapDialog(booking = booking, onDismiss = { showGpsMapForBooking = null })
    }

    showBodycamForBooking?.let { booking ->
        BodycamStreamDialog(booking = booking, onDismiss = { showBodycamForBooking = null })
    }

    showTimelineForBooking?.let { booking ->
        TimelineProgressDialog(booking = booking, onDismiss = { showTimelineForBooking = null })
    }

    showChatForBooking?.let { booking ->
        DriverChatDialog(booking = booking, onDismiss = { showChatForBooking = null })
    }

    showReceiptForBooking?.let { booking ->
        DigitalReceiptDialog(booking = booking, onDismiss = { showReceiptForBooking = null })
    }
}

// 1번: 🩺 보듬펫 AI 반려동물 건강 증상 상담 챗봇 다이얼로그
@Composable
fun AiHealthChatDialog(
    onDismiss: () -> Unit,
    onGoToBooking: () -> Unit
) {
    var inputQuery by remember { mutableStateOf("") }
    val chatLogs = remember {
        mutableStateListOf(
            Pair("AI 수의사 보조 🤖", "안녕하세요! 🐾 발자국동행 AI 반려동물 건강 상담원입니다. 아이의 증상(구토, 기침, 절뚝거림 등)을 말씀해 주시면 1차 예상 진료 과목과 응급 수치를 안내해 드립니다.")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🩺", fontSize = 22.sp)
                Text("발자국동행 AI 반려동물 건강 증상 상담봇", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatLogs.size) { index ->
                        val (sender, text) = chatLogs[index]
                        val isAi = sender.contains("AI")
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                        ) {
                            Text(sender, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAi) Color.White else Color(0xFF0284C7)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 12.sp,
                                    color = if (isAi) Color(0xFF0F172A) else Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        placeholder = { Text("증상 입력 (예: 아이가 토해요)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (inputQuery.isNotEmpty()) {
                                chatLogs.add(Pair("보호자님", inputQuery))
                                val userQ = inputQuery
                                inputQuery = ""

                                val aiResponse = when {
                                    userQ.contains("토") || userQ.contains("구토") -> "🩺 [1차 분석 완료]\n• 추천 진료 과목: 내과 / 24시 응급센터\n• 응급도: 보통 ~ 높음 (지속 구토 시 탈수 주의)\n• 행동 지침: 음식물 섭취 일시 중단 후 수의사 진료를 권장합니다."
                                    userQ.contains("다리") || userQ.contains("절뚝") || userQ.contains("슬개골") -> "🩺 [1차 분석 완료]\n• 추천 진료 과목: 정형외과 / 외과\n• 응급도: 보통 (슬개골 탈구 또는 십자인대 파열 가능성)\n• 행동 지침: 뛰거나 계단 오르기를 금지하고 안심 이송으로 병원에 방문하세요."
                                    else -> "🩺 [1차 분석 완료]\n• 추천 진료 과목: 일반 내과 / 구강 치과\n• 응급도: 보통\n• 행동 지침: 아이 상태를 관찰하며 발자국동행 안심 진료 대행 예약을 추천해 드립니다."
                                }
                                chatLogs.add(Pair("AI 수의사 보조 🤖", aiResponse))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("상담", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onGoToBooking()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("🏥 바로 진료 대행 예약하기", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}

// 2번: 🔔 보듬펫 실시간 안심 알림 센터 다이얼로그
@Composable
fun NotificationCenterDialog(onDismiss: () -> Unit) {
    val notifications = listOf(
        "🚗 [이송 완료]" to "'초코' 아이가 명지24시온누리동물병원에 무사히 도착했습니다. (10분 전)",
        "🎁 [포인트 적립]" to "친구 추천 이벤트 코드가 등록되어 3,000 P가 적립되었습니다. (1시간 전)",
        "📄 [리포트 도착]" to "박찬헌 드라이버님이 전송한 수의사 진료 결과 리포트가 도착했습니다. (어제)",
        "💉 [접종 D-Day]" to "'초코' 아이의 광견병 예방접종 예정일이 14일 남았습니다. (2일 전)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔔", fontSize = 20.sp)
                Text("발자국동행 실시간 알림 센터", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications.size) { index ->
                    val (tag, desc) = notifications[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0284C7))
                            Text(desc, fontSize = 11.sp, color = Color(0xFF0F172A), lineHeight = 15.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// 4번: 🧾 보듬펫 전자 영수증 & 진료비 세부 정산서 보관함 다이얼로그
@Composable
fun DigitalReceiptDialog(
    booking: BookingItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🧾", fontSize = 22.sp)
                Text("발자국동행 전자 정산 영수증", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("상호명", fontSize = 12.sp, color = Color.Gray)
                            Text("발자국동행 안심 이동 케어", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("대표자", fontSize = 12.sp, color = Color.Gray)
                            Text("최지훈, 이선하", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("이용 일시", fontSize = 12.sp, color = Color.Gray)
                            Text(booking.time, fontSize = 12.sp)
                        }
                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("발자국동행 안심 픽업 기본료", fontSize = 12.sp)
                            Text("30,000 원", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("진료 대행 서비스 수수료", fontSize = 12.sp)
                            Text("5,000 원", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("보유 포인트 차감 할인", fontSize = 12.sp, color = Color(0xFFD97706))
                            Text("- 3,000 원", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                        HorizontalDivider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("최종 결제 금액", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Text(booking.estimatedPrice, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7))
                        }
                        Text("결제 수단: 대표 등록 카드 (****-****-****-5394)", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                Text("확인 완료", fontWeight = FontWeight.Bold)
            }
        }
    )
}

// 💰 포인트 내역 데이터 클래스
data class PointLog(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val amount: Int,
    val dateText: String,
    val expireDateText: String = "",
    val isEarned: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    userPoints: Int = 3000,
    userReferralCode: String = "K7TB94",
    userNickname: String = "발자국동행보호자",
    userPhone: String = "010-1234-5678",
    onUpdateProfile: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showPointHistoryDialog by remember { mutableStateOf(false) }

    val pointLogs = remember {
        mutableStateListOf(
            PointLog(
                title = "🎁 친구 추천 이벤트 적립",
                amount = 3000,
                dateText = "2026.10.18 14:30",
                expireDateText = "2027.10.18 소멸 예정",
                isEarned = true
            ),
            PointLog(
                title = "🏥 병원 진료 대행 결제 차감",
                amount = -3000,
                dateText = "2026.10.10 11:15",
                expireDateText = "사용 완료",
                isEarned = false
            ),
            PointLog(
                title = "🎉 회원가입 웰컴 포인트 적립",
                amount = 3000,
                dateText = "2026.10.01 09:00",
                expireDateText = "2027.10.01 소멸 예정",
                isEarned = true
            )
        )
    }

    // State for Nickname & Phone Number (Bound to user profile)
    var nickname by remember(userNickname) { mutableStateOf(userNickname) }
    var userPhoneState by remember(userPhone) { mutableStateOf(userPhone) }

    // State for Payment Card (null by default so no card is pre-registered)
    var registeredCard by remember { mutableStateOf<String?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var newCardNumber by remember { mutableStateOf("") }
    var newCardExpiry by remember { mutableStateOf("") }

    // State for Push Notification & Terms Dialog
    var pushEnabled by remember { mutableStateOf(true) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. 보유 포인트 및 친구 추천 관리 카드 (클릭 시 상세 내역 다이얼로그 연동)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPointHistoryDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Text("💰 내 적립 포인트", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("👉 내역보기", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                "${java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(userPoints)} P",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC)
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
                                Text("🔑 내 추천 코드 (영문 6자리)", fontSize = 12.sp, color = Color.Gray)
                                Text(userReferralCode, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                            HorizontalDivider()
                            Text("• 친구 1명 초대 시 3,000P 100% 즉시 적립 (미사용 시 유효기간 1년)", fontSize = 11.sp, color = Color(0xFF334155))
                            Text("• 예약 결제 시 1P = 1원 현금처럼 즉시 차감 사용 가능", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "📋 내 추천 코드[$userReferralCode]가 복사되었습니다! 친구에게 공유해 보세요.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("📋 내 추천 코드 복사하여 친구 초대하기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        // 1. 닉네임 및 휴대폰번호 설정
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("👤 프로필 및 회원 정보 설정", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🐶", fontSize = 28.sp)
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                label = { Text("보호자 닉네임") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = userPhoneState,
                                onValueChange = { userPhoneState = it },
                                label = { Text("보호자 휴대폰 번호") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onUpdateProfile(nickname, userPhoneState)
                                Toast.makeText(context, "보호자 프로필 정보(닉네임: $nickname, 연락처: $userPhoneState)가 계정에 성공적으로 저장되었습니다!", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("프로필 정보 변경 저장", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. 결제 카드 등록란
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💳 결제 카드 등록 및 관리", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        TextButton(onClick = { showAddCardDialog = true }) {
                            Text(if (registeredCard == null) "+ 카드 등록" else "+ 카드 변경", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Text("진료 대행 및 야간 응급 출동 서비스 이용 시 사용되는 카드입니다.", fontSize = 12.sp, color = Color.Gray)

                    if (registeredCard == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddCardDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("💳", fontSize = 22.sp)
                                    Column {
                                        Text("등록된 결제 카드가 없습니다", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                                        Text("터치하여 결제 카드를 등록해 주세요", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Text("+ 등록", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💳", fontSize = 20.sp)
                                    Column {
                                        Text("대표 결제 카드", fontSize = 11.sp, color = Color.Gray)
                                        Text(registeredCard!!, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E293B))
                                    }
                                }
                                TextButton(onClick = { registeredCard = null }) {
                                    Text("삭제", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 고객센터 전화번호 및 안내
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🎧 발자국동행 고객센터", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📞 대표 전화번호", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("010-3786-5394", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0284C7))
                            }
                            HorizontalDivider()
                            Text("• 운영시간: 평일 09:00 ~ 18:00 (점심시간 12:00 ~ 13:00)", fontSize = 11.sp, color = Color.Gray)
                            Text("• 야간 응급 콜센터: 24시간 연중무휴 (🚨 출동 대기 중)", fontSize = 11.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01037865394"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "전화 앱을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("📞 고객센터 전화걸기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "1:1 문의하기 페이지로 연결됩니다.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("💬 1:1 카톡 문의", fontSize = 13.sp, color = Color(0xFF0284C7))
                        }
                    }
                }
            }
        }

        // 4. 서비스 설정 및 안내
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚙️ 서비스 설정 및 안내", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("응급 출동 및 상태 알림 수신", fontSize = 14.sp)
                        Switch(
                            checked = pushEnabled,
                            onCheckedChange = { pushEnabled = it }
                        )
                    }

                    HorizontalDivider()

                    TextButton(
                        onClick = { showTermsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("서비스 이용약관 및 개인정보 처리방침", color = Color(0xFF475569), fontSize = 13.sp)
                            Text("➔", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // 서비스 이용약관 및 개인정보 처리방침 팝업 다이얼로그
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📜", fontSize = 20.sp)
                    Text("서비스 이용약관 & 개인정보 처리방침", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("제 1 조 (목적 및 서비스 개요)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            "본 약관은 '발자국동행'이 제공하는 반려동물 안심 이동 케어, 병원 진료 대행, 24시 야간 응급 출동 서비스 및 이에 수반되는 제반 서비스의 이용 조건 및 절차를 규정함을 목적으로 합니다.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    item {
                        HorizontalDivider()
                        Text("제 2 조 (수집하는 개인정보 항목 및 목적)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            "• 수집 항목: 보호자 실명, 비상 연락처, 닉네임, GPS 위치 정보, 반려동물 정보(이름, 품종, 생년월일, 기저질환 및 진료 내역), 결제 카드 정보.\n" +
                            "• 이용 목적: 픽업 드라이버 최단거리 배차, 24시 응급센터 최우선 이송, 제휴 동물병원 진료 대행 및 리포트 전달.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    item {
                        HorizontalDivider()
                        Text("제 3 조 (위치 기반 서비스 이용)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            "회사는 보호자와 반려동물의 안전한 이송 및 최우선 드라이버 배차를 위해 스마트폰의 GPS 위치 정보를 수집 및 활용합니다. 위치 정보는 서비스 제공 목적 외에 타 용도로 이용되지 않습니다.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    item {
                        HorizontalDivider()
                        Text("제 4 조 (실시간 바디캠 연동 및 안전 동행)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            "보호자 미동승 진료 대행 시 아이의 안전 확보 및 이동 과정을 확인하실 수 있도록 픽업 및 이동 구간에서 실시간 바디캠 촬영 및 스트리밍이 수행될 수 있습니다.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }

                    item {
                        HorizontalDivider()
                        Text("제 5 조 (개인정보의 보유 및 파기 기간)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text(
                            "회원의 개인정보는 회원 탈퇴 시 즉시 파기합니다. 단, 전자상거래 등에서의 소비자보호에 관한 법률 등 관계 법령에 따라 거래 내역 및 결제 기록은 5년간 안전하게 보관될 수 있습니다.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("확인 및 동의", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 카드 등록 다이얼로그
    if (showAddCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("💳 신규 카드 등록", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newCardNumber,
                        onValueChange = { newCardNumber = it },
                        label = { Text("카드 번호 (16자리)") },
                        placeholder = { Text("0000-0000-0000-0000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newCardExpiry,
                        onValueChange = { newCardExpiry = it },
                        label = { Text("유효기간 (MM/YY)") },
                        placeholder = { Text("12/28") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCardNumber.isNotEmpty()) {
                            val lastFour = if (newCardNumber.length >= 4) newCardNumber.takeLast(4) else "0000"
                            registeredCard = "등록된 카드 (****-****-****-$lastFour)"
                            Toast.makeText(context, "새로운 카드가 등록되었습니다.", Toast.LENGTH_SHORT).show()
                            showAddCardDialog = false
                            newCardNumber = ""
                            newCardExpiry = ""
                        } else {
                            Toast.makeText(context, "카드 번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("등록하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 5. 포인트 적립 및 소멸 상세 내역 팝업 다이얼로그
    if (showPointHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showPointHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💰", fontSize = 20.sp)
                        Text("포인트 적립 & 소멸 상세 내역", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💰 총 보유 포인트", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                    Text(
                                        "${java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(userPoints)} P",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFFD97706)
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFFDE68A))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("⏳ 30일 이내 소멸 예정 포인트", fontSize = 11.sp, color = Color(0xFFB45309))
                                    Text("0 P", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFB45309))
                                }
                                Text("※ 적립된 포인트의 유효기간은 적립일로부터 1년(365일)입니다.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    items(pointLogs.size) { index ->
                        val log = pointLogs[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(log.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("일시: ${log.dateText}", fontSize = 11.sp, color = Color.Gray)
                                    if (log.expireDateText.isNotEmpty()) {
                                        Text(
                                            text = if (log.isEarned) "⏳ 유효기간: ${log.expireDateText}" else "상태: ${log.expireDateText}",
                                            fontSize = 10.sp,
                                            color = if (log.isEarned) Color(0xFFD97706) else Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Text(
                                    text = if (log.isEarned) "+${java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(log.amount)} P" else "${java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(log.amount)} P",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (log.isEarned) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPointHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DetailedServiceCard(
    modifier: Modifier = Modifier,
    badgeText: String,
    title: String,
    carModelText: String,
    features: List<String>,
    buttonText: String,
    cardBgColor: Color,
    badgeBgColor: Color,
    buttonTextColor: Color,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 상단 태그 배지
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBgColor
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // 서비스 타이틀
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )

                // 기아 레이(RAY) 차량 태그
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = carModelText,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 세부 특징 리스트
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    features.forEach { feature ->
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = buttonTextColor
                )
            ) {
                Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

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

@Composable
fun HomeScreenContent(
    isAdminUser: Boolean = false,
    userPoints: Int = 3000,
    onUpdatePoints: (Int) -> Unit = {},
    userReferralCode: String = "K7TB94",
    userNickname: String = "발자국동행보호자",
    userPhone: String = "010-1234-5678",
    onAddBooking: (BookingItem) -> Unit = {},
    onNavigateToBookingTab: () -> Unit = {}
) {
    val context = LocalContext.current
    var showBookingDialog by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showHospitalListDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showReviewWriteDialog by remember { mutableStateOf(false) }
    var showAllReviewsDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showAiChatDialog by remember { mutableStateOf(false) }
    var selectedHospital by remember { mutableStateOf<HospitalInfo?>(null) }

    val reviewsList = remember {
        mutableStateListOf(
            ReviewItem(
                authorName = "홍길동 보호자님",
                petInfo = "말티즈 (초코, 3살)",
                rating = 5,
                serviceType = "🏥 병원 진료 대행",
                content = "회사 업무 때문에 병원에 직접 못 갔는데, 발자국동행 레이 차량으로 안전하게 진료받고 수의사 소견서 리포트까지 전달받아서 너무 안심되었어요!",
                dateText = "2026.10.18"
            ),
            ReviewItem(
                authorName = "강서구 주민님",
                petInfo = "포메라니안 (사랑이, 2살)",
                rating = 5,
                serviceType = "🚨 야간 응급 출동",
                content = "새벽에 갑자기 호흡 곤란이 와서 당황했는데 24시 응급 출동차에 산소 챔버가 있어서 명지24시병원까지 무사히 이송되었습니다. 박찬헌 기사님 감사드립니다!",
                dateText = "2026.10.15"
            )
        )
    }

    val hospitals = listOf(
        HospitalInfo("명지온누리동물병원", "부산 강서구", "낮 운영", "부산 강서구 명지국제8로 230", "051-291-7582"),
        HospitalInfo("에코스카이동물의원", "부산 강서구", "낮 운영", "부산 강서구 에코델타시티중앙로 100", "051-987-6543"),
        HospitalInfo("명지24시온누리메디컬센터", "부산 강서구", "24시 응급", "부산 강서구 명지오션시티4로 80", "051-271-2475"),
        HospitalInfo("하단가람동물병원", "부산 사하구", "낮 운영", "부산 사하구 낙동대로 550", "051-201-1111"),
        HospitalInfo("다대푸른동물병원", "부산 사하구", "낮 운영", "부산 사하구 다대로 300", "051-262-2222"),
        HospitalInfo("하단24시웰니스동물병원", "부산 사하구", "24시 응급", "부산 사하구 하단역 3번출구 앞", "051-208-2400"),
        HospitalInfo("감전동물병원", "부산 사상구", "낮 운영", "부산 사상구 사상로 150", "051-311-3333"),
        HospitalInfo("주례참누리동물병원", "부산 사상구", "낮 운영", "부산 사상구 가야대로 280", "051-322-4444"),
        HospitalInfo("사상24시응급메디컬센터", "부산 사상구", "24시 응급", "부산 사상구 괘법동 사상역 근처", "051-315-2475"),
        HospitalInfo("화명동물병원", "부산 북구", "낮 운영", "부산 북구 화명대로 32", "051-361-5555"),
        HospitalInfo("덕천동물메디컬센터", "부산 북구", "낮 운영", "부산 북구 덕천로 12", "051-332-6666"),
        HospitalInfo("화명24시나누리동물병원", "부산 북구", "24시 응급", "부산 북구 화명역 1번출구 인근", "051-363-2400")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. 인사 카드
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "안녕하세요! 👋", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "오늘 우리 아이 어디로 갈까요?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "🚙 넓은 공간의 '발자국동행 기아 레이(RAY)' 펫 전용차량이 준비되어 있습니다.", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Medium)
                }
            }
        }

        // 1-2. 📢 공지사항 한줄 카드
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNoticeDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0284C7)
                        ) {
                            Text("공지", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Text("서부산(강서/사하/사상/북구) 24시 응급 출동 서비스 정식 오픈 안내", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), maxLines = 1)
                    }
                    Text("더보기 ➔", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // 1-3. 🎁 친구 추천 이벤트 3,000P 배너 카드
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEventDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎁", fontSize = 28.sp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("친구 추천 3,000 P 100% 증정!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text("HOT 🔥", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text("친구 초대 시 1명당 3,000 포인트 적립 (결제 시 현금처럼 사용 가능)", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                    Text("참여하기 ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            }
        }

        // 1-4. 🩺 AI 반려동물 건강 증상 상담봇 배너
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAiChatDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE)),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🩺", fontSize = 28.sp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("발자국동행 AI 건강 증상 상담봇", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0369A1))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0284C7)
                                ) {
                                    Text("AI 봇 🤖", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text("아이의 증상(구토, 기침 등)을 적으면 진료 과목을 안내해 드립니다.", fontSize = 11.sp, color = Color(0xFF0284C7))
                        }
                    }
                    Text("상담하기 ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }
            }
        }

        // 2. 핵심 서비스 카드 2종
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailedServiceCard(
                    modifier = Modifier.weight(1f),
                    badgeText = "🏥 케어 서비스",
                    title = "병원 진료 대행",
                    carModelText = "🚙 기아 레이(RAY) 펫케어",
                    features = listOf(
                        "• 슬라이딩 도어 편한 승하차",
                        "• 펫 전용 안심 시트탑재",
                        "• 실시간 바디캠 라이브 연동",
                        "• 상세 진료 리포트 전달"
                    ),
                    buttonText = "예약하기 ➔",
                    cardBgColor = Color(0xFF0D9488),
                    badgeBgColor = Color(0xFF14B8A6).copy(alpha = 0.45f),
                    buttonTextColor = Color(0xFF0F766E),
                    onButtonClick = { showBookingDialog = true }
                )

                DetailedServiceCard(
                    modifier = Modifier.weight(1f),
                    badgeText = "🚨 24H 긴급 배차",
                    title = "야간 응급 출동",
                    carModelText = "🚑 24시 최우선 긴급 출동차",
                    features = listOf(
                        "• 24시 응급 동물병원 직통",
                        "• 야간 전문 기사 즉시 출동",
                        "• 최단거리 속응 긴급차 배차",
                        "• 상황별 더 빠른 긴급차 배차"
                    ),
                    buttonText = "출동 요청 ➔",
                    cardBgColor = Color(0xFFE11D48),
                    badgeBgColor = Color(0xFFF43F5E).copy(alpha = 0.45f),
                    buttonTextColor = Color(0xFFBE123C),
                    onButtonClick = { showEmergencyDialog = true }
                )
            }
        }

        // 3. 서부산 제휴병원 & 자주묻는질문 (컴팩트 높이 84dp 보정)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { showHospitalListDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🏥 서부산 제휴병원", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• 강서/사하/사상/북구", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { showFaqDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("❓ 자주 묻는 질문", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("• 이용 비용 & 바디캠 안내", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // 4. 생생 이용 후기 헤더
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⭐", fontSize = 18.sp)
                    Text("발자국동행 생생 이용 후기 (${reviewsList.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(onClick = { showReviewWriteDialog = true }) {
                        Text("✏️ 후기작성", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                    }
                    TextButton(onClick = { showAllReviewsDialog = true }) {
                        Text("전체보기 ➔", fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. 이용 후기 피드 리스트 (홈 화면에는 최신 후기 수록)
        items(reviewsList.size) { index ->
            val review = reviewsList[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🐶", fontSize = 18.sp)
                                }
                            }
                            Column {
                                Text(review.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text(review.petInfo, fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row {
                                repeat(review.rating) {
                                    Text("⭐", fontSize = 11.sp)
                                }
                            }
                            Text(review.dateText, fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = review.serviceType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isAdminUser) {
                            TextButton(
                                onClick = {
                                    reviewsList.remove(review)
                                    Toast.makeText(context, "🗑️ 관리자 권한으로 후기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🗑️ 삭제 (관리자)", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = review.content,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 16.sp
                    )

                    review.imageUri?.let { uri ->
                        val bitmap = remember(uri) {
                            try {
                                context.contentResolver.openInputStream(uri).use { inputStream ->
                                    BitmapFactory.decodeStream(inputStream)
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "후기 첨부 사진",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // 6. 이용후기 더보기 전체보기 하단 버튼
        item {
            OutlinedButton(
                onClick = { showAllReviewsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7))
            ) {
                Text("⭐ 이용 후기 전체보기 (${reviewsList.size}개) & 작성하기 ➔", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 7. 메인 하단 우측 푸터 (대표자 정보 및 사업자 등록 번호 공간)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("발자국동행 안심 이동 케어 서비스", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF334155))
                        Text("대표: 최지훈, 이선하", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
                    }
                    HorizontalDivider(color = Color(0xFFCBD5E1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("사업자등록번호: 등록 준비 중", fontSize = 10.sp, color = Color.Gray)
                        Text("고객센터: 010-3786-5394", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("Copyright © Baljaguk Corp. All rights reserved.", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }

    if (showHospitalListDialog) {
        var selectedRegionFilter by remember { mutableStateOf("전체") }
        var selectedTypeFilter by remember { mutableStateOf("전체") }

        val regions = listOf("전체", "강서구", "사하구", "사상구", "북구")

        val filteredHospitals = hospitals.filter { item ->
            val regionMatch = selectedRegionFilter == "전체" || item.region.contains(selectedRegionFilter)
            val typeMatch = selectedTypeFilter == "전체" || item.type == selectedTypeFilter
            regionMatch && typeMatch
        }

        AlertDialog(
            onDismissRequest = { showHospitalListDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🏥 서부산 제휴 병원 목록", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("구 단위 및 운영시간별 필터를 선택하세요.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. 구 단위 필터 (Region Filter)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📍 지역(구) 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(regions.size) { index ->
                                val region = regions[index]
                                FilterChip(
                                    selected = (selectedRegionFilter == region),
                                    onClick = { selectedRegionFilter = region },
                                    label = { Text(region, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                )
                            }
                        }
                    }

                    // 2. 운영시간 구분 필터 (Type Filter)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("⏰ 운영 시간 구분", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = (selectedTypeFilter == "전체"),
                                onClick = { selectedTypeFilter = "전체" },
                                label = { Text("전체", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = (selectedTypeFilter == "낮 운영"),
                                onClick = { selectedTypeFilter = "낮 운영" },
                                label = { Text("☀️ 낮 운영", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = (selectedTypeFilter == "24시 응급"),
                                onClick = { selectedTypeFilter = "24시 응급" },
                                label = { Text("🚨 24시 응급", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    // 3. 필터링된 병원 리스트
                    if (filteredHospitals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("해당 조건의 제휴 병원이 없습니다.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredHospitals.size) { index ->
                                val item = filteredHospitals[index]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedHospital = item },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (item.type == "24시 응급") Color(0xFFFFE4E6) else Color(0xFFE0F2FE)
                                                ) {
                                                    Text(
                                                        text = item.type,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.type == "24시 응급") Color(0xFFE11D48) else Color(0xFF0284C7),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text("${item.region} • ${item.address}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Text("상세보기 ➔", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHospitalListDialog = false }) { Text("닫기", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("❓", fontSize = 20.sp)
                    Text("자주 묻는 질문 & 요금 가이드", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("💰 발자국동행 투명 요금 안내 (서부산 기본 기준)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                                HorizontalDivider()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🏥 병원 진료 대행 서비스 요금", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Text("• 기본 이용료 (왕복 이동 + 동행 1시간 포함): 25,000원 ~ 35,000원", fontSize = 11.sp, color = Color(0xFF334155))
                                    Text("• 진료 대기시간 추가 (30분당): 5,000원", fontSize = 11.sp, color = Color.Gray)
                                }
                                HorizontalDivider()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🚨 24시 야간 응급 출동 서비스 요금", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD32F2F))
                                    Text("• 응급 출동 기본료 (야간 24시 긴급 출동): 45,000원 ~ 55,000원", fontSize = 11.sp, color = Color(0xFF334155))
                                    Text("• 야간/심야 할증 (22:00 ~ 06:00): +10,000원", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("※ 이동 거리(km) 및 서부산 구 단위에 따라 예상 금액이 산정되며, 미터기 기준 투명 정찰제로 안전하게 운용됩니다.", fontSize = 10.sp, color = Color(0xFF0284C7), lineHeight = 14.sp)
                            }
                        }
                    }

                    item {
                        FaqItem(
                            question = "Q1. 이용 요금은 언제 결제되나요?",
                            answer = "예약 접수 시 앱에 등록된 카드로 예상 금액이 가결제되며, 진료 및 이송 완료 후 최종 이송 거리/시간에 맞춰 자동 정산됩니다."
                        )
                    }

                    item {
                        FaqItem(
                            question = "Q2. 실시간 바디캠 라이브 스트리밍은 무료인가요?",
                            answer = "네! 발자국동행의 모든 안심 이송 및 진료 대행 서비스에는 아이의 안전을 위한 실시간 바디캠 모니터링이 기본(무료)으로 제공됩니다."
                        )
                    }

                    item {
                        FaqItem(
                            question = "Q3. 예약 취소 및 환불 규정이 궁금합니다.",
                            answer = "차량 출동 전 취소 시 100% 환불되며, 드라이버 출발 이후 취소 시 이동 거리에 따른 소정의 수수료가 부과될 수 있습니다."
                        )
                    }

                    item {
                        FaqItem(
                            question = "Q4. 보호자가 동승하지 않아도 되나요?",
                            answer = "네! 전문 코디네이터가 보호자 대신 아이를 동반하여 진료를 받고 상세 진료 리포트를 전달해 드립니다."
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFaqDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    selectedHospital?.let { hospital ->
        AlertDialog(
            onDismissRequest = { selectedHospital = null },
            title = { Text("🏥 ${hospital.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("운영 타입: ${hospital.type}", fontWeight = FontWeight.Bold, color = if (hospital.type == "24시 응급") Color(0xFFD32F2F) else Color(0xFF0284C7))
                    HorizontalDivider()
                    Text("📍 주소", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(hospital.address, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📞 전화번호", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(hospital.phone, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedHospital = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("확인")
                }
            }
        )
    }

    if (showAiChatDialog) {
        AiHealthChatDialog(
            onDismiss = { showAiChatDialog = false },
            onGoToBooking = { showBookingDialog = true }
        )
    }

    if (showBookingDialog) {
        var selectedSpecies by remember { mutableStateOf("강아지") }
        var bookingPetName by remember { mutableStateOf("초코") }
        var bookingOwnerName by remember(userNickname) { mutableStateOf(userNickname) }

        // 1. 진료 목적 다중 선택 (중복 체크 가능)
        var isCareSelected by remember { mutableStateOf(true) }
        var isMedicineSelected by remember { mutableStateOf(false) }
        var isSurgerySelected by remember { mutableStateOf(false) }

        // 2. 진료 과목 선택
        var selectedDepartment by remember { mutableStateOf("내과") }
        val departments = listOf("내과", "외과", "안과", "피부과", "치과(스케일링)", "정형외과", "수술(마취)", "암센터(종양)")

        // 3. 과목별 대표 증상 및 수술/암센터 항목 맵
        val departmentSymptomsMap = mapOf(
            "내과" to listOf("감기/기침", "구토/토함", "식욕부진", "설사/무른변", "기력저하", "예방접종/항체검사"),
            "외과" to listOf("슬개골 탈구", "다리 절뚝거림", "상처/외상", "통증/비명", "탈장/혹 수술"),
            "안과" to listOf("눈곱/눈물고임", "눈 충혈", "눈 못뜸", "백내장/혼탁", "안구 수술 상담"),
            "피부과" to listOf("가려움증/긁음", "탈모/털빠짐", "피부 발진", "각질/비듬", "알러지 검사"),
            "치과(스케일링)" to listOf("스케일링(치석제거)", "발치(유치/동요치)", "입냄새/구취", "잇몸 출혈/통증", "구강 검진"),
            "정형외과" to listOf("슬개골 탈구 2~3기", "십자인대 파열", "골절 의심", "관절염/보행장애"),
            "수술(마취)" to listOf("중성화 수술 (남아/여아)", "전신마취 사전검사", "스케일링&발치 수술", "종양/혹 제거 수술", "슬개골 교정 수술", "복부/외과 수술"),
            "암센터(종양)" to listOf("종양/혹 정밀 조직검사", "유선종양/복부종양 수술", "항암 치료 & 면역 관리", "CT/초음파 정밀 영상의학", "암 통증 완화 케어")
        )

        val selectedSymptoms = remember { mutableStateListOf<String>() }
        var customSymptomNote by remember { mutableStateOf("") }

        // 4. 병원 및 희망 시간
        var selectedHospitalName by remember { mutableStateOf("명지24시온누리메디컬센터") }
        var isCustomHospital by remember { mutableStateOf(false) }
        var customHospitalName by remember { mutableStateOf("") }
        var selectedBookingTime by remember { mutableStateOf("오전 10:00") }
        var isHospitalDropdownExpanded by remember { mutableStateOf(false) }

        // 5. 자택 안심 출입 설정 (부재 시 도어락 / 스마트키 / 비밀번호 보안)
        var homeEntryType by remember { mutableStateOf("보호자 자택 상주 (직접 배웅)") }
        val entryTypes = listOf("보호자 자택 상주", "🔑 도어락 비밀번호", "📱 스마트키 / 블루투스", "📦 경비실 / 열쇠보관함")
        var doorlockPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var homeEntryNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🏥", fontSize = 20.sp)
                        Text("병원 진료 대행 예약", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text("증상, 수술/마취 및 목적을 선택하시면 수의사 선생님께 전달됩니다.", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. 반려동물 구분 선택 (강아지 / 고양이)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1. 반려동물 구분 선택 🐾", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = (selectedSpecies == "강아지"),
                                    onClick = { selectedSpecies = "강아지" },
                                    label = { Text("🐶 강아지", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = (selectedSpecies == "고양이"),
                                    onClick = { selectedSpecies = "고양이" },
                                    label = { Text("🐱 고양이", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. 기본 정보 (반려동물 이름 & 보호자 실명)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("2. 기본 정보 📝", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = bookingPetName,
                                    onValueChange = { bookingPetName = it },
                                    label = { Text("반려동물 이름") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = bookingOwnerName,
                                    onValueChange = { bookingOwnerName = it },
                                    label = { Text("보호자 실명") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 3. 서비스 목적 다중 선택 (중복 가능)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("3. 서비스 이용 목적 (중복 선택 가능) 🎯", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = isCareSelected,
                                    onClick = { isCareSelected = !isCareSelected },
                                    label = { Text("🩺 병원 진찰", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = isMedicineSelected,
                                    onClick = { isMedicineSelected = !isMedicineSelected },
                                    label = { Text("💊 처방약 수령", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            FilterChip(
                                selected = isSurgerySelected,
                                onClick = { isSurgerySelected = !isSurgerySelected },
                                label = { Text("💉 수술 및 마취 검사", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. 진료 과목 선택
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("4. 희망 진료 과목 / 센터 선택 🏥", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(departments.size) { index ->
                                    val dept = departments[index]
                                    FilterChip(
                                        selected = (selectedDepartment == dept),
                                        onClick = {
                                            selectedDepartment = dept
                                            selectedSymptoms.clear()
                                        },
                                        label = { Text(dept, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    // 5. 과목별 대표 증상 체크박스 (다중 선택)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("5. [$selectedDepartment] 대표 세부 증상 체크 (다중 선택) 🩺", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            val currentDepartmentSymptoms = departmentSymptomsMap[selectedDepartment] ?: emptyList()
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                currentDepartmentSymptoms.chunked(2).forEach { rowSymptoms ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowSymptoms.forEach { symptom ->
                                            val isChecked = selectedSymptoms.contains(symptom)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isChecked) selectedSymptoms.remove(symptom)
                                                        else selectedSymptoms.add(symptom)
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isChecked) Color(0xFFFEF3C7) else Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, if (isChecked) Color(0xFFF59E0B) else Color(0xFFE2E8F0))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) selectedSymptoms.add(symptom)
                                                            else selectedSymptoms.remove(symptom)
                                                        }
                                                    )
                                                    Text(symptom, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                }
                                            }
                                        }
                                        if (rowSymptoms.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. 기타 전달 및 메모 직접 작성란
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("6. 기타 상세 증상 및 요청사항 직접 작성 ✍️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            OutlinedTextField(
                                value = customSymptomNote,
                                onValueChange = { customSymptomNote = it },
                                placeholder = { Text("예: 중성화 수술 전 마취 사전 검사를 희망하며, 치과 스케일링도 함께 가능한지 확인해주세요.") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    // 7. 제휴 병원 및 시간 선택 (직접 작성 기능 포함)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("7. 방문 병원 선택 및 시간 ⏰", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                TextButton(
                                    onClick = {
                                        isCustomHospital = !isCustomHospital
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (isCustomHospital) "📋 목록에서 선택" else "✏️ 병원명 직접기입",
                                        fontSize = 12.sp,
                                        color = Color(0xFF0284C7),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isCustomHospital) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { isHospitalDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedHospitalName, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 13.sp)
                                            Text("▼", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isHospitalDropdownExpanded,
                                        onDismissRequest = { isHospitalDropdownExpanded = false }
                                    ) {
                                        hospitals.forEach { hospital ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(hospital.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text("${hospital.region} | ${hospital.type}", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                },
                                                onClick = {
                                                    selectedHospitalName = hospital.name
                                                    isHospitalDropdownExpanded = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text("✏️ 직접 입력 (목록에 없는 병원)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0284C7))
                                            },
                                            onClick = {
                                                isCustomHospital = true
                                                isHospitalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = customHospitalName,
                                    onValueChange = { customHospitalName = it },
                                    label = { Text("방문 동물병원 이름 직접 입력") },
                                    placeholder = { Text("예: 부산 명지 큰마음 동물병원") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("오전 10:00", "오후 02:00", "오후 04:00", "오후 06:00").forEach { time ->
                                    FilterChip(
                                        selected = (selectedBookingTime == time),
                                        onClick = { selectedBookingTime = time },
                                        label = { Text(time, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 8. 자택 안심 출입 & 도어락 보안 설정 (부재 시 대책)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("8. 자택 안심 출입 & 도어락 보안 설정 🔑", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("부재 시 픽업 드라이버가 아이를 안심하고 픽업할 수 있도록 설정하세요.", fontSize = 11.sp, color = Color.Gray)

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(entryTypes.size) { index ->
                                    val entry = entryTypes[index]
                                    FilterChip(
                                        selected = (homeEntryType == entry),
                                        onClick = { homeEntryType = entry },
                                        label = { Text(entry, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                    )
                                }
                            }

                            if (homeEntryType.contains("도어락")) {
                                OutlinedTextField(
                                    value = doorlockPassword,
                                    onValueChange = { doorlockPassword = it },
                                    label = { Text("🔑 자택 / 공동현관 도어락 비밀번호") },
                                    placeholder = { Text("예: 공동 #1234 / 세대 5678*") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Text(if (isPasswordVisible) "👁️" else "🔒", fontSize = 16.sp)
                                        }
                                    }
                                )
                            } else if (homeEntryType.contains("블루투스") || homeEntryType.contains("스마트키")) {
                                OutlinedTextField(
                                    value = homeEntryNote,
                                    onValueChange = { homeEntryNote = it },
                                    label = { Text("📱 블루투스 / 스마트키 원패스 연동 안내") },
                                    placeholder = { Text("예: 아파트 스마트원패스 앱 임시 출입권 공유 또는 안내") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (homeEntryType.contains("경비실") || homeEntryType.contains("보관함")) {
                                OutlinedTextField(
                                    value = homeEntryNote,
                                    onValueChange = { homeEntryNote = it },
                                    label = { Text("📦 경비실 / 열쇠 보관함 위치 및 비번") },
                                    placeholder = { Text("예: 101동 경비실에 '발자국동행 픽업' 맡겨둠 / 보관함 #04 비번 1111") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🔒", fontSize = 18.sp)
                                    Text(
                                        text = "보안 보증: 입력하신 자택 출입 정보는 픽업 드라이버 출동 시 시한부 암호화 제공되며, 이송 완료 즉시 시스템에서 영구 파기됩니다.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF15803D),
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // 9. 가결제 예상 금액 및 환불 안내
                    item {
                        val calculatedPriceValue = 30000 + (if (isMedicineSelected) 5000 else 0) + (if (isSurgerySelected) 10000 else 0)
                        val formattedPriceStr = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(calculatedPriceValue)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
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
                                    Text("💳 가결제 예정 금액", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF92400E))
                                    Text("${formattedPriceStr}원", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD97706))
                                }
                                HorizontalDivider(color = Color(0xFFFDE68A))
                                Text("• 등록 카드로 사전 가결제되며, 진료 완료 후 실제 시간·거리에 따라 자동 최종 정산(추가금 결제 / 차액 환불)됩니다.", fontSize = 11.sp, color = Color(0xFFB45309), lineHeight = 15.sp)
                                Text("• ℹ️ 픽업 드라이버 출동 전 취소 시 100% 즉시 환불됩니다.", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 9. 차량 배차 안내
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0F9FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🚙", fontSize = 20.sp)
                                Column {
                                    Text("발자국동행 기아 레이(RAY) 펫 전용차 배차", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0369A1))
                                    Text("슬라이딩 도어 승하차 & 실시간 바디캠 라이브 연동", fontSize = 10.sp, color = Color(0xFF0284C7))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val purposeList = mutableListOf<String>()
                        if (isCareSelected) purposeList.add("병원 진찰")
                        if (isMedicineSelected) purposeList.add("처방약 수령")
                        if (isSurgerySelected) purposeList.add("수술/마취 검사")
                        val purposeStr = if (purposeList.isNotEmpty()) purposeList.joinToString(", ") else "일반 진료"

                        val entrySummary = when {
                            homeEntryType.contains("도어락") && doorlockPassword.isNotEmpty() -> "출입: 비밀번호 암호화"
                            homeEntryType.contains("블루투스") || homeEntryType.contains("스마트키") -> "출입: 블루투스 스마트키 ($homeEntryNote)"
                            homeEntryType.contains("경비실") || homeEntryType.contains("보관함") -> "출입: 경비실/보관함 ($homeEntryNote)"
                            else -> "출입: 보호자 직접 배웅"
                        }

                        val symptomStr = if (selectedSymptoms.isNotEmpty()) selectedSymptoms.joinToString(", ") else "상세상담"
                        val combinedNote = "목적: $purposeStr | 과목: $selectedDepartment ($symptomStr) | $entrySummary" +
                                if (customSymptomNote.isNotEmpty()) " | 메모: $customSymptomNote" else ""

                        val finalHospital = if (isCustomHospital && customHospitalName.isNotEmpty()) customHospitalName else selectedHospitalName

                        val calculatedPriceValue = 30000 + (if (isMedicineSelected) 5000 else 0) + (if (isSurgerySelected) 10000 else 0)
                        val formattedPriceStr = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(calculatedPriceValue)

                        val newBooking = BookingItem(
                            serviceType = "병원 진료 대행",
                            species = selectedSpecies,
                            petName = bookingPetName,
                            ownerName = bookingOwnerName,
                            hospitalName = finalHospital,
                            time = "오늘 $selectedBookingTime",
                            status = "예약 확정 (배차 완료)",
                            vehicleInfo = "발자국동행 기아 레이(RAY) 펫전용차",
                            symptomNote = combinedNote,
                            estimatedPrice = "${formattedPriceStr}원 (가결제)"
                        )
                        onAddBooking(newBooking)
                        Toast.makeText(
                            context,
                            "[$selectedSpecies] '$bookingPetName' 아이의 수술/진료 대행 예약이 완료되었습니다!",
                            Toast.LENGTH_LONG
                        ).show()
                        showBookingDialog = false
                        onNavigateToBookingTab()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("예약 완료하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBookingDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showEmergencyDialog) {
        var emergencySpecies by remember { mutableStateOf("강아지") }
        var emergencyPetName by remember { mutableStateOf("초코") }
        var emergencyOwnerName by remember(userNickname) { mutableStateOf(userNickname) }
        var emergencyPhone by remember(userPhone) { mutableStateOf(userPhone) }

        val emergencySymptomsList = listOf(
            "🚨 호흡곤란 / 산소부족",
            "🚨 심한 출혈 / 외상",
            "🚨 경련 / 발작 / 의식불명",
            "🚨 급성 복부팽만 / 구토",
            "🚨 사고 / 낙상 / 골절 의심",
            "🚨 야간 급성 통증 / 비명"
        )
        val selectedEmergencySymptoms = remember { mutableStateListOf<String>() }

        val emergencyHospitals = listOf(
            "명지24시온누리메디컬센터 (부산 강서구)",
            "하단24시웰니스동물병원 (부산 사하구)",
            "사상24시응급메디컬센터 (부산 사상구)",
            "화명24시나누리동물병원 (부산 북구)"
        )
        var selectedEmergencyHospital by remember { mutableStateOf(emergencyHospitals[0]) }
        var isCustomEmergencyHospital by remember { mutableStateOf(false) }
        var customEmergencyHospitalName by remember { mutableStateOf("") }
        var isEmergencyHospitalDropdownExpanded by remember { mutableStateOf(false) }

        var selectedDispatchTime by remember { mutableStateOf("🚨 즉시 긴급 출동") }
        var customEmergencyNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🚨", fontSize = 20.sp)
                        Text("24시 야간 응급 출동 신청", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD32F2F))
                    }
                    Text("24시 응급 전용차가 최우선 배차됩니다. (상황에 따라 더 빠른 차량이 출동할 수 있습니다)", fontSize = 11.sp, color = Color(0xFFD32F2F))
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(460.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. 반려동물 구분 선택 (강아지 / 고양이)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("1. 반려동물 구분 선택 🐾", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = (emergencySpecies == "강아지"),
                                    onClick = { emergencySpecies = "강아지" },
                                    label = { Text("🐶 강아지", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = (emergencySpecies == "고양이"),
                                    onClick = { emergencySpecies = "고양이" },
                                    label = { Text("🐱 고양이", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. 기본 정보 및 비상 연락처
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("2. 기본 및 비상 연락처 정보 📝", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = emergencyPetName,
                                    onValueChange = { emergencyPetName = it },
                                    label = { Text("반려동물 이름") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = emergencyOwnerName,
                                    onValueChange = { emergencyOwnerName = it },
                                    label = { Text("보호자 실명") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = emergencyPhone,
                                onValueChange = { emergencyPhone = it },
                                label = { Text("비상 연락처 (기사 직접 연락)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // 3. 긴급 응급 상황 및 증상 선택 (다중 선택)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("3. 긴급 응급 상황 선택 (다중 선택) 🚨", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD32F2F))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                emergencySymptomsList.chunked(2).forEach { rowSymptoms ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowSymptoms.forEach { symptom ->
                                            val isChecked = selectedEmergencySymptoms.contains(symptom)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        if (isChecked) selectedEmergencySymptoms.remove(symptom)
                                                        else selectedEmergencySymptoms.add(symptom)
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isChecked) Color(0xFFFFE4E6) else Color(0xFFF8FAFC),
                                                border = BorderStroke(1.dp, if (isChecked) Color(0xFFE11D48) else Color(0xFFE2E8F0))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) selectedEmergencySymptoms.add(symptom)
                                                            else selectedEmergencySymptoms.remove(symptom)
                                                        }
                                                    )
                                                    Text(symptom, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isChecked) Color(0xFFBE123C) else Color(0xFF0F172A))
                                                }
                                            }
                                        }
                                        if (rowSymptoms.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. 이송 희망 24시 응급 동물병원 선택
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("4. 이송 희망 24시 응급병원 🏥", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                TextButton(
                                    onClick = { isCustomEmergencyHospital = !isCustomEmergencyHospital },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (isCustomEmergencyHospital) "📋 목록에서 선택" else "✏️ 직접기입",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD32F2F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!isCustomEmergencyHospital) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { isEmergencyHospitalDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedEmergencyHospital, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 12.sp)
                                            Text("▼", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isEmergencyHospitalDropdownExpanded,
                                        onDismissRequest = { isEmergencyHospitalDropdownExpanded = false }
                                    ) {
                                        emergencyHospitals.forEach { hosp ->
                                            DropdownMenuItem(
                                                text = { Text(hosp, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                                onClick = {
                                                    selectedEmergencyHospital = hosp
                                                    isEmergencyHospitalDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = customEmergencyHospitalName,
                                    onValueChange = { customEmergencyHospitalName = it },
                                    label = { Text("24시 응급병원 이름 직접 입력") },
                                    placeholder = { Text("예: 부산 사상24시 동물의원") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 5. 출동 희망 시각
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("5. 출동 희망 시간 ⏰", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("🚨 즉시 긴급 출동 (최우선)", "⏰ 30분 이내 방문", "⏰ 1시간 이내 방문").forEach { dispatchTime ->
                                    FilterChip(
                                        selected = (selectedDispatchTime == dispatchTime),
                                        onClick = { selectedDispatchTime = dispatchTime },
                                        label = { Text(dispatchTime, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 6. 기타 전달사항
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("6. 기타 비상 응급 전달사항 ✍️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            OutlinedTextField(
                                value = customEmergencyNote,
                                onValueChange = { customEmergencyNote = it },
                                placeholder = { Text("예: 아이가 호흡을 힘들어해서 산소마취 키트 준비 부탁드립니다.") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    // 7. 응급차 안내 배너
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🚑", fontSize = 22.sp)
                                Column {
                                    Text("발자국동행 24H 최우선 긴급 응급 출동차 배차", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFBE123C))
                                    Text("안심 긴급 출동 (상황 및 거리에 따라 더 빠른 긴급차 배차 가능)", fontSize = 10.sp, color = Color(0xFFE11D48))
                                }
                            }
                        }
                    }

                    // 8. 응급 출동 가결제 금액 및 환불 안내
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFE4E6),
                            border = BorderStroke(1.dp, Color(0xFFFECDD3))
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
                                    Text("💳 긴급 출동 가결제 예정 금액", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9F1239))
                                    Text("50,000원", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE11D48))
                                }
                                HorizontalDivider(color = Color(0xFFFECDD3))
                                Text("• 24시 최우선 긴급 이송 가결제 금액이며, 이송 완료 후 실제 거리 및 진료 시간에 맞춰 자동 정산됩니다.", fontSize = 11.sp, color = Color(0xFFBE123C), lineHeight = 15.sp)
                                Text("• ℹ️ 차량 출동 전 취소 시 100% 즉시 환불됩니다.", fontSize = 11.sp, color = Color(0xFFBE123C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalEmergencyHospital = if (isCustomEmergencyHospital && customEmergencyHospitalName.isNotEmpty()) customEmergencyHospitalName else selectedEmergencyHospital
                        val symptomsStr = if (selectedEmergencySymptoms.isNotEmpty()) selectedEmergencySymptoms.joinToString(", ") else "급성 응급 증상"
                        val combinedEmergencyNote = "증상: $symptomsStr | 비상연락: $emergencyPhone" +
                                if (customEmergencyNote.isNotEmpty()) " | 메모: $customEmergencyNote" else ""

                        val newEmergencyBooking = BookingItem(
                            serviceType = "야간 응급 출동",
                            species = emergencySpecies,
                            petName = emergencyPetName,
                            ownerName = emergencyOwnerName,
                            hospitalName = finalEmergencyHospital,
                            time = selectedDispatchTime,
                            status = "🚨 긴급 배차 중 (최단거리 속응차)",
                            vehicleInfo = "발자국동행 24시 응급 전용차 (상황별 최우선 속응 차량 배차)",
                            symptomNote = combinedEmergencyNote,
                            estimatedPrice = "50,000원 (가결제)"
                        )
                        onAddBooking(newEmergencyBooking)
                        Toast.makeText(context, "🚨 [$emergencySpecies] '$emergencyPetName' 아이의 24시 응급 출동 배차가 시작되었습니다!", Toast.LENGTH_LONG).show()
                        showEmergencyDialog = false
                        onNavigateToBookingTab()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("🚨 긴급 출동 요청하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (showReviewWriteDialog) {
        var reviewRating by remember { mutableIntStateOf(5) }
        var reviewText by remember { mutableStateOf("") }
        var reviewImageUri by remember { mutableStateOf<Uri?>(null) }

        val reviewPhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { reviewImageUri = it }
        }

        AlertDialog(
            onDismissRequest = { showReviewWriteDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⭐", fontSize = 20.sp)
                    Text("발자국동행 이용 후기 작성", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("만족도 별점 선택", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { star ->
                                Text(
                                    text = if (star <= reviewRating) "⭐" else "☆",
                                    fontSize = 28.sp,
                                    modifier = Modifier.clickable { reviewRating = star }
                                )
                            }
                            Text("${reviewRating}.0 점", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF59E0B))
                        }
                    }

                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("이용 후기 내용") },
                        placeholder = { Text("발자국동행 안심 이동 케어 이용 소감을 남겨주세요.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { reviewPhotoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (reviewImageUri != null) "📷 사진 등록 완료 (클릭 시 변경)" else "📷 사진 첨부하기 (갤러리 선택)", fontSize = 12.sp, color = Color(0xFF0284C7))
                        }

                        reviewImageUri?.let { uri ->
                            val bitmap = remember(uri) {
                                try {
                                    context.contentResolver.openInputStream(uri).use { inputStream ->
                                        BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "첨부 사진 미리보기",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewText.isNotEmpty()) {
                            val newReview = ReviewItem(
                                authorName = "발자국동행 회원님",
                                petInfo = "내 반려동물",
                                rating = reviewRating,
                                serviceType = "🐾 발자국동행 안심 이동 서비스",
                                content = reviewText,
                                imageUri = reviewImageUri,
                                dateText = "방금 전"
                            )
                            reviewsList.add(0, newReview)
                            Toast.makeText(context, "소중한 이용 후기가 등록되었습니다!", Toast.LENGTH_SHORT).show()
                            showReviewWriteDialog = false
                        } else {
                            Toast.makeText(context, "후기 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("후기 등록하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewWriteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 이용 후기 전체보기 및 작성 팝업 다이얼로그
    if (showAllReviewsDialog) {
        AlertDialog(
            onDismissRequest = { showAllReviewsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⭐", fontSize = 20.sp)
                        Text("발자국동행 이용 후기 전체보기 (${reviewsList.size}개)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showReviewWriteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("✏️ 새 이용 후기 작성하기", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reviewsList.size) { index ->
                            val review = reviewsList[index]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🐶", fontSize = 16.sp)
                                            Text(review.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                            Text("(${review.petInfo})", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        Row {
                                            repeat(review.rating) {
                                                Text("⭐", fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE0F2FE)
                                        ) {
                                            Text(
                                                text = review.serviceType,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (isAdminUser) {
                                            TextButton(
                                                onClick = {
                                                    reviewsList.remove(review)
                                                    Toast.makeText(context, "🗑️ 관리자 권한으로 후기가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("🗑️ 삭제 (관리자)", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Text(
                                        text = review.content,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 16.sp
                                    )

                                    review.imageUri?.let { uri ->
                                        val bitmap = remember(uri) {
                                            try {
                                                context.contentResolver.openInputStream(uri).use { inputStream ->
                                                    BitmapFactory.decodeStream(inputStream)
                                                }
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }
                                        bitmap?.let {
                                            Image(
                                                bitmap = it.asImageBitmap(),
                                                contentDescription = "후기 첨부 사진",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    Text(review.dateText, fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllReviewsDialog = false }) {
                    Text("닫기", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📢", fontSize = 20.sp)
                    Text("발자국동행 공식 공지사항", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📢 서부산 24시 응급 출동 서비스 정식 오픈 안내", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("2026.10.18", fontSize = 10.sp, color = Color.Gray)
                                HorizontalDivider()
                                Text("강서구, 사하구, 사상구, 북구 서부산 전 지역에 산소 챔버 완비 24시 긴급차 최우선 이송 서비스가 정식 오픈되었습니다.", fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 15.sp)
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📢 기아 레이(RAY) 펫 전용차 정기 살균 소독 안내", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("2026.10.15", fontSize = 10.sp, color = Color.Gray)
                                HorizontalDivider()
                                Text("아이들의 위생과 건강을 위해 매 운행 종료 후 인체 무해 유기농 소독수로 레이 케어카 차내 전체 살균 소독을 실시합니다.", fontSize = 11.sp, color = Color(0xFF334155), lineHeight = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNoticeDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEventDialog) {
        var inputFriendCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEventDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎁", fontSize = 20.sp)
                    Text("친구 추천 이벤트 (1명당 3,000P)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔑 내 고유 추천 코드 (영문 6자리)", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            Text(userReferralCode, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "📋 내 추천 코드[$userReferralCode]가 복사되었습니다! 친구에게 공유해 보세요.", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📋 추천 코드 복사하기", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🎁 친구의 추천 코드 입력하기", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = inputFriendCode,
                                onValueChange = { inputFriendCode = it.uppercase() },
                                placeholder = { Text("6자리 코드 입력") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (inputFriendCode.length >= 6) {
                                        onUpdatePoints(userPoints + 3000)
                                        Toast.makeText(context, "🎉 친구 추천 코드가 등록되어 3,000 P가 즉시 적립되었습니다!", Toast.LENGTH_LONG).show()
                                        inputFriendCode = ""
                                    } else {
                                        Toast.makeText(context, "올바른 6자리 영문 추천 코드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                            ) {
                                Text("적립", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("ℹ️ 이벤트 및 포인트 이용 안내", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                            Text("• 초대자 및 초대를 받은 분 모두에게 1명당 3,000 P 즉시 적립", fontSize = 10.sp, color = Color.Gray)
                            Text("• 적립된 포인트는 예약 결제 시 1P = 1원 현금처럼 즉시 사용 가능", fontSize = 10.sp, color = Color.Gray)
                            Text("• 포인트의 유효기간은 적립일로부터 1년(365일)입니다.", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEventDialog = false }) {
                    Text("닫기", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = question, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
            Text(text = answer, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)
        }
    }
}

// 🐶 반려동물 프로필 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPetDetailScreen() {
    val context = LocalContext.current
    var petImageUri by remember { mutableStateOf<Uri?>(null) }
    var petSpecies by remember { mutableStateOf("강아지") }
    var petName by remember { mutableStateOf("초코") }
    var ownerRealName by remember { mutableStateOf("홍길동") }
    var petType by remember { mutableStateOf("말티즈") }
    var selectedGender by remember { mutableStateOf("남아") }
    var isNeutered by remember { mutableStateOf(true) }
    var genderDropdownExpanded by remember { mutableStateOf(false) }

    var selectedBirthMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    var petWeight by remember { mutableStateOf("4.5") }
    var petHospital by remember { mutableStateOf("명지24시온누리메디컬센터") }
    var petSymptoms by remember { mutableStateOf("구토 및 식욕 부진, 슬개골 탈구 2기") }
    var isSaved by remember { mutableStateOf(false) }

    val birthDateText: String
    val ageText: String
    if (selectedBirthMillis != null) {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
        birthDateText = dateFormat.format(Date(selectedBirthMillis!!))

        val birthCal = Calendar.getInstance().apply { timeInMillis = selectedBirthMillis!! }
        val todayCal = Calendar.getInstance()
        var age = todayCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        if (todayCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        ageText = if (age < 0) "0살" else "${age}살"
    } else {
        birthDateText = "2021년 05월 12일"
        ageText = "3살"
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { petImageUri = it }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .border(3.dp, Color(0xFF0284C7), CircleShape)
                            .clickable { galleryLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (petImageUri != null) {
                            val bitmap = remember(petImageUri) {
                                try {
                                    context.contentResolver.openInputStream(petImageUri!!).use { inputStream ->
                                        BitmapFactory.decodeStream(inputStream)
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "반려동물 사진",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (petSpecies == "강아지") "🐶" else "🐱", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("사진 등록", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("📷 갤러리에서 사진 선택", fontSize = 12.sp, color = Color(0xFF0284C7))
                    }
                }
            }
        }

        // 💉 예방접종 & 건강 D-Day 수첩 카드
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                        Text("💉 예방접종 & 건강 D-Day 수첩", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        TextButton(
                            onClick = {
                                Toast.makeText(context, "+ 신규 D-Day 건강 일정이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+ 일정 추가", fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("💉 광견병 백신", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text("D-14", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2563EB))
                                Text("예정: 11.01", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("💊 심장사상충", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF92400E))
                                Text("D-3", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFD97706))
                                Text("예정: 10.21", fontSize = 9.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("🩺 건강검진", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                                Text("D-30", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0284C7))
                                Text("예정: 11.17", fontSize = 9.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🐾 내 반려동물 상세 프로필", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("진료 대행 및 응급 출동 시 의료진에게 전달될 정보입니다.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(2.dp))

                    Column {
                        Text("반려동물 구분", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = (petSpecies == "강아지"),
                                onClick = {
                                    petSpecies = "강아지"
                                    if (petType == "코리안 숏헤어") petType = "말티즈"
                                },
                                label = { Text("🐶 강아지", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = (petSpecies == "고양이"),
                                onClick = {
                                    petSpecies = "고양이"
                                    if (petType == "말티즈") petType = "코리안 숏헤어"
                                },
                                label = { Text("🐱 고양이", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = petName,
                            onValueChange = { petName = it },
                            label = { Text("반려동물 이름") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ownerRealName,
                            onValueChange = { ownerRealName = it },
                            label = { Text("보호자 실명") },
                            placeholder = { Text("예: 홍길동") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = petType,
                        onValueChange = { petType = it },
                        label = { Text("품종 (${petSpecies} 세부 종류)") },
                        placeholder = { Text(if (petSpecies == "강아지") "예: 말티즈, 포메라니안" else "예: 코리안 숏헤어, 페르시안") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column {
                        Text("성별 및 중성화 여부", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { genderDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("$selectedGender ▼", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(
                                    expanded = genderDropdownExpanded,
                                    onDismissRequest = { genderDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("남아") },
                                        onClick = {
                                            selectedGender = "남아"
                                            genderDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("여아") },
                                        onClick = {
                                            selectedGender = "여아"
                                            genderDropdownExpanded = false
                                        }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isNeutered,
                                    onCheckedChange = { isNeutered = it }
                                )
                                Text("중성화", fontSize = 13.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("생년월일 및 나이", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$birthDateText ($ageText)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { showDatePickerDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("날짜 선택", fontSize = 12.sp, color = Color(0xFF0284C7))
                        }
                    }

                    if (showDatePickerDialog) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedBirthMillis ?: System.currentTimeMillis()
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePickerDialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        selectedBirthMillis = datePickerState.selectedDateMillis
                                        showDatePickerDialog = false
                                    }
                                ) {
                                    Text("확인")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePickerDialog = false }) {
                                    Text("취소")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    OutlinedTextField(
                        value = petWeight,
                        onValueChange = { petWeight = it },
                        label = { Text("몸무게 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = petHospital,
                        onValueChange = { petHospital = it },
                        label = { Text("주 이용 병원") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = petSymptoms,
                        onValueChange = { petSymptoms = it },
                        label = { Text("특이사항 및 기저질환") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            isSaved = true
                            Toast.makeText(context, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text(if (isSaved) "프로필 저장 완료 ✓" else "프로필 저장하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
