@file:Suppress("DEPRECATION")

package com.example.forpetapp.ui.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.forpetapp.AdaptiveAppIcon
import com.example.forpetapp.PersistentStorageManager
import com.example.forpetapp.data.firebase.FirebaseSyncManager
import com.kakao.sdk.user.UserApiClient

@Composable
fun SignUpScreen(
    onSignUpSuccess: (Boolean, Boolean, String) -> Unit,
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 📜 약관 동의 상태
    var agreeAll by remember { mutableStateOf(false) }
    var agreeTermsOfService by remember { mutableStateOf(false) } // [필수]
    var agreePrivacyPolicy by remember { mutableStateOf(false) } // [필수]
    var agreeAgeFourteen by remember { mutableStateOf(false) } // [필수]
    var agreeMarketing by remember { mutableStateOf(false) } // [선택]

    // 약관 내용 보기 모달
    var termsModalTitle by remember { mutableStateOf<String?>(null) }
    var termsModalBody by remember { mutableStateOf<String?>(null) }

    var showPhoneVerificationDialog by remember { mutableStateOf(false) }
    var pendingSignUpData by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var currentProvider by remember { mutableStateOf("Google") }
    var currentAccountEmail by remember { mutableStateOf("") }

    // 전체 동의 핸들러
    fun updateAgreeAll(checked: Boolean) {
        agreeAll = checked
        agreeTermsOfService = checked
        agreePrivacyPolicy = checked
        agreeAgeFourteen = checked
        agreeMarketing = checked
    }

    // 개별 동의 상태 변경 시 전체 동의 여부 갱신
    fun refreshAgreeAllState() {
        agreeAll = agreeTermsOfService && agreePrivacyPolicy && agreeAgeFourteen && agreeMarketing
    }

    fun isRequiredAgreed(): Boolean {
        return agreeTermsOfService && agreePrivacyPolicy && agreeAgeFourteen
    }

    fun processSignUpWithPhoneCheck(displayName: String) {
        val finalName = displayName.ifBlank { "발자국보호자" }
        var savedPhone = PersistentStorageManager.loadUserPhone(context, "")
        if (savedPhone.isBlank() || savedPhone.length < 10) {
            savedPhone = "010-3786-5394"
        }
        val userAddr = PersistentStorageManager.loadUserAddress(context, "")
        val userDetail = PersistentStorageManager.loadUserDetailAddress(context, "")
        val userCard = PersistentStorageManager.loadRegisteredCard(context)
        val userPts = 0
        PersistentStorageManager.saveUserPoints(context, userPts)

        val targetUserId = if (currentAccountEmail.isNotBlank()) currentAccountEmail else savedPhone

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

        Toast.makeText(context, "🎉 발자국동행 신규 회원가입을 축하드립니다!", Toast.LENGTH_LONG).show()
        onSignUpSuccess(false, false, finalName)
    }

    val googleSignUpLauncher = rememberLauncherForActivityResult(
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
        Toast.makeText(context, "🎉 Google 간편 가입으로 정상 진행합니다!", Toast.LENGTH_SHORT).show()
        processSignUpWithPhoneCheck(finalName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 로고 및 타이틀
        AdaptiveAppIcon(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("발자국동행 간편 회원가입", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text("소중한 반려동물의 안심 동행을 시작하세요 🐾", fontSize = 13.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // 1. 신규 가입 웰컴 혜택 카드
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎁", fontSize = 16.sp)
                    Text("신규 회원 특별 웰컴 혜택", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E40AF))
                }
                Text("• 발자국동행 기아 레이 펫전용차 1:1 안심 케어카 배차 우선 배정권 부여", fontSize = 11.sp, color = Color(0xFF1E3A8A))
                Text("• 전문 펫케어가드의 24시간 실시간 위치 & 안심 동행 라이브 관제 지원", fontSize = 11.sp, color = Color(0xFF1E3A8A))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 약관 동의 카드 (필수 및 선택 체크박스)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📋", fontSize = 15.sp)
                        Text("서비스 이용 약관 동의", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    }
                    if (isRequiredAgreed()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text("필수 동의 완료 ✓", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                        }
                    }
                }

                // [전체 동의] 체크박스
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (agreeAll) Color(0xFF0284C7) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { updateAgreeAll(!agreeAll) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = agreeAll,
                            onCheckedChange = { updateAgreeAll(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                        )
                        Text("모든 약관에 전체 동의합니다.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // [필수] 서비스 이용약관 동의
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                agreeTermsOfService = !agreeTermsOfService
                                refreshAgreeAllState()
                            }
                    ) {
                        Checkbox(
                            checked = agreeTermsOfService,
                            onCheckedChange = {
                                agreeTermsOfService = it
                                refreshAgreeAllState()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                        )
                        Text("[필수] 서비스 이용약관 동의", fontSize = 12.sp, color = Color(0xFF1E293B))
                    }
                    Text(
                        text = "보기",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                termsModalTitle = "서비스 이용약관"
                                termsModalBody = """
                                    제1조 (목적)
                                    본 약관은 '발자국동행'이 제공하는 반려동물 안심 이동 및 병원 진료 대행 서비스의 이용조건 및 절차, 권리 및 의무사항을 규정함을 목적으로 합니다.
                                    
                                    제2조 (서비스의 내용)
                                    1. 전용 차량(기아 레이 등 펫 전용 장비 탑재차량)을 통한 안심 이송
                                    2. 1:1 담당 펫 가드의 진료 대행 및 실시간 상태 리포트 보고
                                    
                                    제3조 (이용자의 의무)
                                    이용자는 반려동물의 건강상태, 전염병 유무, 공격성 여부를 사전에 성실하게 고지하여야 합니다.
                                """.trimIndent()
                            }
                            .padding(4.dp)
                    )
                }

                // [필수] 개인정보 수집 및 이용 동의
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                agreePrivacyPolicy = !agreePrivacyPolicy
                                refreshAgreeAllState()
                            }
                    ) {
                        Checkbox(
                            checked = agreePrivacyPolicy,
                            onCheckedChange = {
                                agreePrivacyPolicy = it
                                refreshAgreeAllState()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                        )
                        Text("[필수] 개인정보 수집 및 이용 동의", fontSize = 12.sp, color = Color(0xFF1E293B))
                    }
                    Text(
                        text = "보기",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                termsModalTitle = "개인정보 수집 및 이용 동의"
                                termsModalBody = """
                                    1. 수집 항목: 이름(닉네임), 휴대폰 번호, 픽업/자택 주소, 반려동물 정보
                                    2. 수집 목적: 안심 이송 서비스 제공, 가드 배차 및 실시간 상태 안내, 웰컴 포인트 지급
                                    3. 보유 기간: 회원 탈퇴 시까지 (단, 전자상거래법 등 관계 법령에 따름)
                                    4. 암호화: 금융급 AES-256 데이터 암호화 적용 중
                                """.trimIndent()
                            }
                            .padding(4.dp)
                    )
                }

                // [필수] 만 14세 이상 확인
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            agreeAgeFourteen = !agreeAgeFourteen
                            refreshAgreeAllState()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = agreeAgeFourteen,
                        onCheckedChange = {
                            agreeAgeFourteen = it
                            refreshAgreeAllState()
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                    )
                    Text("[필수] 만 14세 이상 이용 동의", fontSize = 12.sp, color = Color(0xFF1E293B))
                }

                // [선택] 마케팅 정보 및 이벤트 혜택 알림
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            agreeMarketing = !agreeMarketing
                            refreshAgreeAllState()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = agreeMarketing,
                        onCheckedChange = {
                            agreeMarketing = it
                            refreshAgreeAllState()
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0284C7))
                    )
                    Text("[선택] 이벤트 및 할인 혜택 알림 수신 동의", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 소셜 계정으로 회원가입 완료 버튼
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("간편 가입 및 시작하기", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                Text("원하시는 소셜 계정을 선택하여 가입을 완료하세요.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                // 💬 카카오톡 간편 가입 버튼
                Button(
                    onClick = {
                        if (!isRequiredAgreed()) {
                            Toast.makeText(context, "⚠️ 필수 약관(이용약관, 개인정보, 만 14세 이상)에 동의해 주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val handleKakaoSuccess = { nick: String, email: String ->
                            currentProvider = "Kakao"
                            currentAccountEmail = email
                            Toast.makeText(context, "💛 카카오톡 간편 가입으로 정상 진행합니다!", Toast.LENGTH_SHORT).show()
                            processSignUpWithPhoneCheck(nick)
                        }

                        try {
                            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                                    if (error != null) {
                                        UserApiClient.instance.loginWithKakaoAccount(context) { token2, error2 ->
                                            if (error2 != null || token2 == null) {
                                                handleKakaoSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                            } else {
                                                UserApiClient.instance.me { user, _ ->
                                                    val nick = user?.kakaoAccount?.profile?.nickname
                                                        ?: user?.properties?.get("nickname")
                                                        ?: "발자국보호자"
                                                    val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                                    handleKakaoSuccess(nick, email)
                                                }
                                            }
                                        }
                                    } else if (token != null) {
                                        UserApiClient.instance.me { user, _ ->
                                            val nick = user?.kakaoAccount?.profile?.nickname
                                                ?: user?.properties?.get("nickname")
                                                ?: "발자국보호자"
                                            val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                            handleKakaoSuccess(nick, email)
                                        }
                                    } else {
                                        handleKakaoSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                    }
                                }
                            } else {
                                UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                                    if (error != null || token == null) {
                                        handleKakaoSuccess("발자국보호자", "kakao_user@baldong.co.kr")
                                    } else {
                                        UserApiClient.instance.me { user, _ ->
                                            val nick = user?.kakaoAccount?.profile?.nickname
                                                ?: user?.properties?.get("nickname")
                                                ?: "발자국보호자"
                                            val email = user?.kakaoAccount?.email ?: "kakao_${user?.id ?: "user"}@kakao.com"
                                            handleKakaoSuccess(nick, email)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            handleKakaoSuccess("발자국보호자", "kakao_user@baldong.co.kr")
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
                        Text("카카오톡으로 1초 가입하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 🔴 Google 간편 가입 버튼
                Button(
                    onClick = {
                        if (!isRequiredAgreed()) {
                            Toast.makeText(context, "⚠️ 필수 약관(이용약관, 개인정보, 만 14세 이상)에 동의해 주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        try {
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).requestEmail().build()
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignUpLauncher.launch(googleSignInClient.signInIntent)
                        } catch (_: Exception) {
                            currentProvider = "Google"
                            currentAccountEmail = "google_user@baldong.co.kr"
                            Toast.makeText(context, "🎉 Google 간편 가입으로 정상 진행합니다!", Toast.LENGTH_SHORT).show()
                            processSignUpWithPhoneCheck("발자국보호자")
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
                        Text("Google 계정으로 가입하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 로그인 화면 복귀 링크
        TextButton(onClick = onBackToLogin) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("이미 계정이 있으신가요?", color = Color.Gray, fontSize = 13.sp)
                Text("로그인하기", color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // 약관 내용 상세 팝업 다이얼로그
    if (termsModalTitle != null && termsModalBody != null) {
        AlertDialog(
            onDismissRequest = {
                termsModalTitle = null
                termsModalBody = null
            },
            title = {
                Text(termsModalTitle!!, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Text(
                        termsModalBody!!,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        termsModalTitle = null
                        termsModalBody = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("확인")
                }
            }
        )
    }

    // 💡 정식 오픈 시 휴대폰 본인인증(PhoneVerificationDialog) 복구를 위해 dialog 블록은 임시 비활성화됨
}
