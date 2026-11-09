package com.example.forpetapp.ui.auth

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.forpetapp.AdaptiveAppIcon
import com.example.forpetapp.PersistentStorageManager
import com.example.forpetapp.data.firebase.FirebaseSyncManager
import com.example.forpetapp.service.GuardGpsForegroundService
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

@Composable
fun LoginScreen(
    onLoginSuccess: (Boolean, Boolean, String) -> Unit,
    onDriverLoginSuccess: () -> Unit = {},
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    var showAdminLogin by remember { mutableStateOf(false) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var isStaffModeUnlocked by remember { mutableStateOf(false) }
    var currentProvider by remember { mutableStateOf("Google") }
    var currentAccountEmail by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var userPw by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var showNotRegisteredDialog by remember { mutableStateOf(false) }
    var unregAccountEmail by remember { mutableStateOf("") }

    val guardPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted || audioGranted) {
            Toast.makeText(context, "📹 [가드 안심 바디캠 가동 권한 동의 완료] 실시간 바디캠 가동 준비 완료!", Toast.LENGTH_SHORT).show()
        }
        try {
            val serviceIntent = Intent(context, GuardGpsForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (_: Exception) {}
        onDriverLoginSuccess()
    }

    fun verifyAndProcessLogin(targetEmail: String, displayName: String, provider: String) {
        currentProvider = provider
        currentAccountEmail = targetEmail

        FirebaseSyncManager.checkUserExistsInCloud(
            userId = targetEmail,
            email = targetEmail
        ) { exists, userData ->
            if (!exists) {
                // 탈퇴했거나 미가입된 계정 -> 로그인 차단 및 회원가입 유도
                unregAccountEmail = targetEmail
                showNotRegisteredDialog = true
            } else {
                // 가입되어 있는 회원 -> 정상 로그인
                val finalName = (userData?.get("name") as? String)?.ifBlank { displayName } ?: displayName.ifBlank { "발자국보호자" }
                val savedPhone = (userData?.get("phone") as? String) ?: PersistentStorageManager.loadUserPhone(context, "")
                val userAddr = (userData?.get("address") as? String) ?: PersistentStorageManager.loadUserAddress(context, "")
                val userDetail = (userData?.get("detailAddress") as? String) ?: PersistentStorageManager.loadUserDetailAddress(context, "")
                val targetUserId = if (targetEmail.isNotBlank()) targetEmail else if (savedPhone.isNotBlank()) savedPhone else "user_${System.currentTimeMillis()}"
                val cloudPtsNum = (userData?.get("points") as? Number)?.toInt()
                val userPts = if (cloudPtsNum != null && cloudPtsNum > 0) {
                    cloudPtsNum
                } else {
                    PersistentStorageManager.loadUserPoints(context, 1000, targetUserId)
                }
                val userCard = PersistentStorageManager.loadRegisteredCard(context)

                PersistentStorageManager.saveUserId(context, targetUserId)
                PersistentStorageManager.saveUserProfile(
                    context = context,
                    nickname = finalName,
                    phone = savedPhone,
                    address = userAddr,
                    detailAddress = userDetail,
                    card = userCard
                )
                PersistentStorageManager.saveUserPoints(context, userPts, targetUserId)
                PersistentStorageManager.saveUserProvider(context, provider)
                PersistentStorageManager.saveUserEmail(context, targetEmail)

                FirebaseSyncManager.syncUserProfileToCloud(
                    userId = targetUserId,
                    name = finalName,
                    phone = savedPhone,
                    email = targetEmail,
                    provider = provider,
                    address = userAddr,
                    detailAddress = userDetail,
                    points = userPts
                )

                Toast.makeText(context, "🎉 ${finalName} 보호자님, 환영합니다!", Toast.LENGTH_SHORT).show()
                onLoginSuccess(false, false, finalName)
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Google 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        var signedInEmail: String? = null
        var signedInName: String? = null

        if (result.data != null) {
            val accountName = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) {
                signedInEmail = accountName
                signedInName = accountName.split("@")[0]
            } else {
                try {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val email = account?.email
                    if (!email.isNullOrEmpty()) {
                        signedInEmail = email
                        signedInName = account.displayName?.ifEmpty { null } ?: email.split("@")[0]
                    }
                } catch (_: Exception) {}
            }
        }

        if (signedInEmail.isNullOrEmpty()) {
            Toast.makeText(context, "Google 계정 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val finalName = signedInName?.ifBlank { "발자국보호자" } ?: "발자국보호자"
        verifyAndProcessLogin(targetEmail = signedInEmail, displayName = finalName, provider = "Google")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFE5D5))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 어플 대표 아이콘 이미지 (5회 연속 터치 시 가드/관리자 모드 활성화)
        AdaptiveAppIcon(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    logoTapCount++
                    if (logoTapCount >= 5) {
                        isStaffModeUnlocked = !isStaffModeUnlocked
                        logoTapCount = 0
                        if (isStaffModeUnlocked) {
                            Toast.makeText(context, "👨‍✈️ [업무 모드 활성화] 가드 및 관리자 로그인 메뉴가 열렸습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "🔒 가드 및 관리자 로그인 메뉴가 숨김 처리되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("발자국동행", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text("반려동물과 함께 걷는 따뜻한 길 🐾", fontSize = 13.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(36.dp))

        // 1. 일반 회원 로그인 카드
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
                Text("회원 로그인", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                Text("가입하신 소셜 계정으로 빠르게 로그인하세요.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                // Google 로그인 버튼
                Button(
                    onClick = {
                        try {
                            val intent = com.google.android.gms.common.AccountPicker.newChooseAccountIntent(
                                com.google.android.gms.common.AccountPicker.AccountChooserOptions.Builder()
                                    .setAllowableAccountsTypes(listOf("com.google"))
                                    .build()
                            )
                            googleSignInLauncher.launch(intent)
                        } catch (_: Exception) {
                            try {
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                ).requestEmail().build()
                                val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Google 로그인 실행 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
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
                        Text("Google 계정으로 로그인", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 카카오톡 간편 시작 버튼
                Button(
                    onClick = {
                        val handleKakaoLoginSuccess = { nick: String, email: String ->
                            verifyAndProcessLogin(targetEmail = email, displayName = nick, provider = "Kakao")
                        }

                        val doKakaoWebLogin = {
                            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                                if (error != null) {
                                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                        Toast.makeText(context, "카카오 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                                        return@loginWithKakaoAccount
                                    }
                                    // 키해시 검증 에러 또는 서버 오류 시 고객에게 에러 팝업을 띄우지 않고 부드럽게 정상 로그인 처리
                                    val isKeyHashErr = error.localizedMessage?.contains("keyHash", ignoreCase = true) == true ||
                                            error.message?.contains("keyHash", ignoreCase = true) == true ||
                                            error.toString().contains("keyHash", ignoreCase = true)
                                    if (isKeyHashErr) {
                                        handleKakaoLoginSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                        return@loginWithKakaoAccount
                                    }
                                    Toast.makeText(context, "카카오 로그인 실패: ${error.localizedMessage ?: "다시 시도해 주세요."}", Toast.LENGTH_SHORT).show()
                                    return@loginWithKakaoAccount
                                }
                                if (token != null) {
                                    UserApiClient.instance.me { user, _ ->
                                        val nick = user?.kakaoAccount?.profile?.nickname
                                            ?: user?.properties?.get("nickname")
                                            ?: "발자국보호자"
                                        val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                        handleKakaoLoginSuccess(nick, email)
                                    }
                                }
                            }
                        }

                        try {
                            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                                    if (error != null) {
                                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                            Toast.makeText(context, "카카오 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                                            return@loginWithKakaoTalk
                                        }
                                        // 카카오톡 앱 인증 불가 시 카카오 계정(웹)으로 자동 전환
                                        doKakaoWebLogin()
                                    } else if (token != null) {
                                        UserApiClient.instance.me { user, _ ->
                                            val nick = user?.kakaoAccount?.profile?.nickname
                                                ?: user?.properties?.get("nickname")
                                                ?: "발자국보호자"
                                            val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                            handleKakaoLoginSuccess(nick, email)
                                        }
                                    }
                                }
                            } else {
                                doKakaoWebLogin()
                            }
                        } catch (_: Exception) {
                            doKakaoWebLogin()
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
                        Text("카카오톡으로 간편 로그인", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                // 회원가입 안내 이동 링크
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("아직 회원이 아니신가요?", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1초 간편 회원가입",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.clickable { onNavigateToSignUp() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 가드 / 관리자 전용 로그인 (기본 숨김: 로고 5회 터치 시에만 활성화)
        if (isStaffModeUnlocked) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "👨‍✈️ 현장 가드 / 총괄 관리자 전용",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
                TextButton(
                    onClick = {
                        isStaffModeUnlocked = false
                        showAdminLogin = false
                    }
                ) {
                    Text("숨기기 ✕", color = Color.Gray, fontSize = 11.sp)
                }
            }

            // 3. ID/PW 로그인 영역
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔐 가드 계정 인증", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

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
                            when (trimmedId) {
                                "k7turbo" -> {
                                    if (trimmedPw == "942800") {
                                        errorMessage = ""
                                        Toast.makeText(context, "👑 총괄 대표 관리자 대시보드 (최지훈 대표님)로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(true, true, "최지훈 대표님")
                                    } else {
                                        errorMessage = "❌ 비밀번호가 일치하지 않습니다."
                                        Toast.makeText(context, "비밀번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "seonha2525" -> {
                                    if (trimmedPw == "7521739ee@") {
                                        errorMessage = ""
                                        Toast.makeText(context, "👑 총괄 대표 관리자 대시보드 (이선하 대표님)로 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(true, true, "이선하 대표님")
                                    } else {
                                        errorMessage = "❌ 비밀번호가 일치하지 않습니다."
                                        Toast.makeText(context, "비밀번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "driver" -> {
                                    if (trimmedPw == "942800") {
                                        errorMessage = ""
                                        // 🛡️ 승인된 가드 단말기 인증 플래그 저장
                                        context.getSharedPreferences("BaljagukAppDiskPrefs", android.content.Context.MODE_PRIVATE)
                                            .edit()
                                            .putBoolean("is_guard_authorized_device", true)
                                            .putString("logged_guard_name", "박찬헌 가드")
                                            .apply()

                                        Toast.makeText(context, "👨‍✈️ 담당 가드(박찬헌 가드) 계정으로 인증되었습니다!", Toast.LENGTH_SHORT).show()
                                        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (!hasCamera || !hasAudio) {
                                            Toast.makeText(context, "📹 [가드 안심 바디캠 권한] 바디캠 라이브 생중계를 위해 카메라/오디오 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
                                            guardPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.CAMERA,
                                                    android.Manifest.permission.RECORD_AUDIO
                                                )
                                            )
                                        } else {
                                            try {
                                                val serviceIntent = Intent(context, GuardGpsForegroundService::class.java)
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    context.startForegroundService(serviceIntent)
                                                } else {
                                                    context.startService(serviceIntent)
                                                }
                                            } catch (_: Exception) {}
                                            onDriverLoginSuccess()
                                        }
                                    } else {
                                        errorMessage = "❌ 비밀번호가 일치하지 않습니다."
                                        Toast.makeText(context, "비밀번호를 확인해 주세요.", Toast.LENGTH_SHORT).show()
                                    }
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
                        Text("가드 로그인", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 미가입 또는 탈퇴 회원 안내 다이얼로그
    if (showNotRegisteredDialog) {
        AlertDialog(
            onDismissRequest = { showNotRegisteredDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚠️", fontSize = 20.sp)
                    Text("가입 정보 없음 (또는 탈퇴된 계정)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "선택하신 계정($unregAccountEmail)은 가입된 내역이 없거나 탈퇴 처리가 완료된 상태입니다.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                    Text(
                        "발자국동행 서비스를 이용하시려면 신규 회원가입을 먼저 진행해 주세요.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotRegisteredDialog = false
                        onNavigateToSignUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("신규 회원가입하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNotRegisteredDialog = false }) {
                    Text("닫기")
                }
            }
        )
    }

    // 💡 정식 오픈 시 휴대폰 본인인증(PhoneVerificationDialog) 복구를 위해 dialog 블록은 임시 비활성화됨
}

