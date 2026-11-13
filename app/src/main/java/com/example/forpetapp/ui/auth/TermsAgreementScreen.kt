package com.example.forpetapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsAgreementScreen(onAgreed: () -> Unit) {
    var allAgreed by remember { mutableStateOf(false) }
    var term1Agreed by remember { mutableStateOf(false) }
    var term2Agreed by remember { mutableStateOf(false) }
    var term3Agreed by remember { mutableStateOf(false) }

    LaunchedEffect(term1Agreed, term2Agreed, term3Agreed) {
        allAgreed = term1Agreed && term2Agreed && term3Agreed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFE5D5))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            "발자국동행 서비스 이용을 위해\n약관 동의가 필요합니다.",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF78350F),
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allAgreed,
                        onCheckedChange = {
                            allAgreed = it
                            term1Agreed = it
                            term2Agreed = it
                            term3Agreed = it
                        },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                    )
                    Text("약관 전체동의", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = term1Agreed,
                        onCheckedChange = { term1Agreed = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                    )
                    Text("[필수] 서비스 이용약관 동의", fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = term2Agreed,
                        onCheckedChange = { term2Agreed = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                    )
                    Text("[필수] 개인정보 수집 및 이용 동의", fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = term3Agreed,
                        onCheckedChange = { term3Agreed = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD97706))
                    )
                    Text("[필수] 위치기반 서비스 이용약관 동의", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { if (allAgreed) onAgreed() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = allAgreed,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD97706),
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text("동의하고 시작하기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
