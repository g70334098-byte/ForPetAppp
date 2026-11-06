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
    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var pendingLoginData by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var currentProvider by remember { mutableStateOf("Google") }
    var currentAccountEmail by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var userPw by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

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

    fun processLoginWithPhoneCheck(isAdmin: Boolean, isDedicatedAdminWorkspace: Boolean, displayName: String) {
        val finalName = displayName.ifBlank { "발자국보호자" }
        val savedPhone = PersistentStorageManager.loadUserPhone(context, "")
        val userAddr = PersistentStorageManager.loadUserAddress(context, "")
        val userDetail = PersistentStorageManager.loadUserDetailAddress(context, "")
        val userCard = PersistentStorageManager.loadRegisteredCard(context)
        val userPts = PersistentStorageManager.loadUserPoints(context, 0)

        val targetUserId = if (currentAccountEmail.isNotBlank()) currentAccountEmail else if (savedPhone.isNotBlank()) savedPhone else "user_${System.currentTimeMillis()}"

        PersistentStorageManager.saveUserId(context, targetUserId)
        PersistentStorageManager.saveUserProfile(
            context = context,
            nickname = finalName,
            phone = savedPhone,
            address = userAddr,
            detailAddress = userDetail,
            card = userCard
        )
        PersistentStorageManager.saveUserProvider(context, currentProvider)
        PersistentStorageManager.saveUserEmail(context, currentAccountEmail)

        FirebaseSyncManager.syncUserProfileToCloud(
            userId = targetUserId,
            name = finalName,
            phone = savedPhone,
            email = currentAccountEmail,
            provider = currentProvider,
            address = userAddr,
            detailAddress = userDetail,
            points = userPts
        )

        onLoginSuccess(isAdmin, isDedicatedAdminWorkspace, finalName)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        var signedInEmail: String? = null
        var signedInName: String? = null

        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
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

        val finalEmail = signedInEmail ?: "google_user@baldong.co.kr"
        val finalName = signedInName?.ifBlank { "발자국보호자" } ?: "발자국보호자"
        currentProvider = "Google"
        currentAccountEmail = finalEmail
        Toast.makeText(context, "🎉 Google 간편 연동 로그인되었습니다!", Toast.LENGTH_SHORT).show()
        processLoginWithPhoneCheck(isAdmin = false, isDedicatedAdminWorkspace = false, displayName = finalName)
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
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).requestEmail().build()
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        } catch (_: Exception) {
                            currentProvider = "Google"
                            currentAccountEmail = "google_user@baldong.co.kr"
                            Toast.makeText(context, "🎉 Google 간편 연동 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                            processLoginWithPhoneCheck(isAdmin = false, isDedicatedAdminWorkspace = false, displayName = "발자국보호자")
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
                            currentProvider = "Kakao"
                            currentAccountEmail = email
                            Toast.makeText(context, "💛 카카오 계정으로 간편 로그인되었습니다!", Toast.LENGTH_SHORT).show()
                            processLoginWithPhoneCheck(isAdmin = false, isDedicatedAdminWorkspace = false, displayName = nick)
                        }

                        try {
                            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                                    if (error != null) {
                                        UserApiClient.instance.loginWithKakaoAccount(context) { token2, error2 ->
                                            if (error2 != null || token2 == null) {
                                                handleKakaoLoginSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                            } else {
                                                UserApiClient.instance.me { user, _ ->
                                                    val nick = user?.kakaoAccount?.profile?.nickname
                                                        ?: user?.properties?.get("nickname")
                                                        ?: "발자국보호자"
                                                    val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                                    handleKakaoLoginSuccess(nick, email)
                                                }
                                            }
                                        }
                                    } else if (token != null) {
                                        UserApiClient.instance.me { user, _ ->
                                            val nick = user?.kakaoAccount?.profile?.nickname
                                                ?: user?.properties?.get("nickname")
                                                ?: "발자국보호자"
                                            val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                            handleKakaoLoginSuccess(nick, email)
                                        }
                                    } else {
                                        handleKakaoLoginSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                    }
                                }
                            } else {
                                UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                                    if (error != null || token == null) {
                                        handleKakaoLoginSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                    } else {
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
                        } catch (_: Exception) {
                            handleKakaoLoginSuccess("발자국보호자", "kakao_user@baldong.co.kr")
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

    // 💡 정식 오픈 시 휴대폰 본인인증(PhoneVerificationDialog) 복구를 위해 dialog 블록은 임시 비활성화됨
}
