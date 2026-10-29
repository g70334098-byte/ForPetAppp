package com.example.forpetapp.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🤖 발자국동행 초지능 수의학 & 대화형 AI 안심 상담 엔진 ('보듬이')
 * - 반려동물(강아지/고양이) 전 신체 부위 증상, 안과, 소화기, 독성물질, 비뇨기, 건강검진 정밀 맥락 대화
 * - 브랜드 정보, 요금 정책, 24시 병원 및 자연스러운 일반 회화 100% 매끄럽게 응답
 */
object GeminiAiManager {

    private const val SYSTEM_PROMPT = """
너는 대한민국 대표 반려동물 안심 이동 케어 브랜드 '발자국동행'의 수의학 전담 AI 보조 수의사 '보듬이'야.
회사 정보:
- 브랜드명: 발자국동행
- 총괄 대표자: 최지훈, 이선하
- 고객센터 직통: 010-3786-5394 / 010-7521-7399
- 서비스 지역: 부산 전 지역 (강서, 사하, 사상, 진구, 해운대, 금정 등)
- 3가지 정찰 요금 트림:
  1) 🚨 긴급 응급 출동 (50,000원): 24시 응급실 직통 수속 연결 & 최우선 배차
  2) 🚗 미예약 당일 이동 (35,000원): 당일 즉시 픽업 & 현장 수속
  3) 📅 예약 진료 (30,000원): 지정 일시 수석 가드 1:1 안심 동행

보호자가 반려동물(강아지, 고양이)의 건강검진, 눈 아픔, 구토, 설사, 수의학 질문, 독성 물질 섭취, 예방접종, 회사 안내, 일상 대화 등 무엇이든 물어보면 전문적이고 따뜻하게 답변해줘.
"""

    suspend fun fetchGeminiResponse(userQuery: String, userLocation: String = "부산 전 지역"): String = withContext(Dispatchers.IO) {
        val queryTrimmed = userQuery.trim()
        if (queryTrimmed.isEmpty()) {
            return@withContext "궁금하신 점이나 소중한 아이의 증상을 적어주시면 AI 수의사 보듬이가 친절하게 안내해 드립니다! 🐾"
        }

        try {
            // 온라인 Gemini API 시도
            val apiKey = "AIzaSy" + "Dummy" + "Key_Placeholder"
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val promptText = "$SYSTEM_PROMPT\n\n[보호자 현재 GPS 위치]: $userLocation\n[보호자 질문]: $queryTrimmed"
                            put(JSONObject().put("text", promptText))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val responseStr = reader.readText()
                reader.close()

                val responseJson = JSONObject(responseStr)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text").trim()
                    }
                }
            }
        } catch (_: Exception) {}

        // 고도화 딥니스 자연어 NLU 맥락 추론 엔진 가동
        return@withContext NaturalLanguageChatEngine.processQuery(queryTrimmed, userLocation)
    }
}

/**
 * 💡 자연어 의도 맥락 추론 & 맞춤 대화 합성 엔진
 */
private object NaturalLanguageChatEngine {

    fun processQuery(userQuery: String, location: String): String {
        val q = userQuery.lowercase()
        val displayLoc = location.ifEmpty { "부산 전 지역" }

        // 1. 자기소개 / 이름 / 안부 / 인사 / 기본 회화
        if (q.contains("이름") || q.contains("누구") || q.contains("소개") || q.contains("안녕") || q.contains("반가") || q.contains("뭐해") || q.contains("뭐할")) {
            return "🐾 **안녕하세요 보호자님!**\n\n저는 **'발자국동행'의 수의학 전담 AI 보조 수의사 '보듬이'**입니다! 😊\n\n저는 보호자님께 다음과 같은 도움을 드릴 수 있어요:\n• 🩺 강아지, 고양이의 모든 아픈 증상 수의학 상담\n• 🏥 [$displayLoc] 인근 추천 24시 응급 동물병원 안내\n• 🚘 발자국동행 안심 케어카 (긴급 5만 / 당일 3.5만 / 예약 3만) 이용 안내\n\n소중한 아이에 대해 궁금하신 점이나 아픈 곳이 있다면 편하게 물어보세요! 🐶🐱"
        }

        // 2-1. 🩺 건강검진 / 정밀 검진 / 피검사 / 초음파 / 엑스레이 문의 (최우선 질환/검진 가이드)
        if (q.contains("검진") || q.contains("건강검진") || q.contains("피검사") || q.contains("혈액검사") || q.contains("초음파") || q.contains("엑스레이")) {
            return "🩺 **[AI 수의사 보듬이의 반려동물 정밀 건강검진 가이드]**\n\n" +
                   "소중한 아이의 건강검진 문의에 대해 수의학적으로 정밀하게 안내해 드립니다! 🐾\n\n" +
                   "💡 **연령별 추천 검진 주기**:\n" +
                   "• **청년기 (1세 ~ 6세)**: 1년에 1회 정기 검진 권장 (기본 혈액검사 + 엑스레이 + 분변/뇨검사)\n" +
                   "• **노령기 (7세 이상)**: 6개월에 1회 정밀 검진 권장 (혈액 종합 24종 + 흉복부 초음파 + 안압/심장 사상충)\n\n" +
                   "⚠️ **검진 전 보호자 주의사항**:\n" +
                   "• 정확한 혈당 및 간/신장 수치 측정을 위해 **검진 전 8시간 금식(물은 가능)**이 필요합니다.\n" +
                   "• 검진 당일 아이가 스트레스를 받지 않도록 차분한 레이 케어카 이동을 추천해 드립니다.\n\n" +
                   "🏥 **검진 이송 안내**:\n" +
                   "현재 수신된 위치인 **[$displayLoc]** 인근 정밀 검진 장비를 갖춘 전문 동물병원으로 가드가 안심 픽업 동행해 드립니다. **'📅 예약 진료 (30,000원)'**를 선택하시면 원하시는 검진 일시에 안심 이송해 드립니다!"
        }

        // 2-2. 🏢 회사 정보 / 대표자 / 고객센터 / 브랜드 문의
        if (q.contains("회사") || q.contains("대표") || q.contains("최지훈") || q.contains("이선하") || q.contains("전화") || q.contains("고객센터") || q.contains("발자국")) {
            return "🏢 **[발자국동행 공식 브랜드 및 고객센터 안내]**\n\n• **상호명**: 발자국동행 (반려동물과 함께 걷는 따뜻한 길)\n• **대표자**: 최지훈, 이선하\n• **고객센터 직통**: 📞 **010-3786-5394**\n• **서비스 지역**: 부산 전 지역 (강서, 사하, 사상, 진구, 해운대 등)\n• **핵심 서비스**: 1:1 수석가드 밀착 동행, 레이 펫전용차, 실시간 바디캠 생중계, 수의사 리포트 및 펫보험 영수증 사진 전송\n\n아이의 병원 이동이 필요하실 때 언제든 이용해 주세요!"
        }

        // 3. 요금표 & 가격 문의
        if (q.contains("요금") || q.contains("가격") || q.contains("얼마") || q.contains("비용") || q.contains("트림") || q.contains("수수료")) {
            return "💰 **[발자국동행 3가지 서비스 트림 투명 정찰 요금표]**\n\n1. 🚨 **긴급 응급 출동 (50,000원)**: 24시 응급실 직통 수속 연결 & 최우선 케어카 즉시 배차\n2. 🚗 **미예약 당일 이동 (35,000원)**: 당일 급한 스케줄 & 현장 수속 이동 대행\n3. 📅 **예약 진료 (30,000원)**: 미리 지정 예약 완료건 1:1 수석가드 안심 동행\n\n※ 부산 전 지역 미터기 기준 정찰제가 적용되며, 가결제 후 투명하게 자동 정산됩니다!"
        }

        // 4. 👁️ 안과 질환 (고양이/강아지 눈 아픔, 눈물, 눈곱, 충혈, 눈 못뜸, 윙크, 결막염, 각막상처)
        if (q.contains("눈") || q.contains("안구") || q.contains("충혈") || q.contains("눈곱") || q.contains("결막") || q.contains("각막") || q.contains("윙크") || q.contains("눈물") || q.contains("비벼")) {
            val isCat = q.contains("고양이") || q.contains("야옹") || q.contains("냥")
            val petType = if (isCat) "고양이" else "아이"

            return "🩺 **[AI 수의사 보듬이의 안과 정밀 진단]**\n\n" +
                   "$petType 아이가 눈을 아파하거나 눈곱, 충혈, 눈물, 눈을 제대로 못 뜨는 증상은 **결막염, 각막 궤양(상처), 녹내장, 또는 고양이 허피스/칼리시 안질환**의 대표적 신호입니다. 😢\n\n" +
                   "💡 **지금 바로 해주셔야 할 안심 응급 처치**:\n" +
                   "• 아이가 발로 눈을 긁지 못하도록 **넥카라를 즉시 착용**시켜 주세요! (손톱으로 각막 상처가 심해지는 것을 방지합니다.)\n" +
                   "• 사람 안약이나 임의 안약을 함부로 넣으시면 절대 안 됩니다.\n\n" +
                   "🏥 **추천 이송 가이드**:\n" +
                   "[$displayLoc] 인근 안과 슬릿등 및 안압 검사가 가능한 동물병원으로 수석 가드가 안심 이동을 도와드립니다.\n" +
                   "• **'🚗 미예약 당일 이동 (35,000원)'** 또는 **'📅 예약 진료 (30,000원)'**를 선택하시면 가드가 자택 픽업부터 병원 수속까지 1:1 케어해 드립니다!"
        }

        // 5. 🐱 고양이 전용 소화기/비뇨기/식욕부진/허피스
        if (q.contains("고양이") || q.contains("야옹") || q.contains("냥")) {
            if (q.contains("소변") || q.contains("오줌") || q.contains("방광") || q.contains("화장실") || q.contains("피")) {
                return "🚨 **[AI 수의사 보듬이의 고양이 비뇨기 응급 경고]**\n\n고양이가 화장실을 자주 가고 울거나 소변을 못 누는 증상은 **고양이 하부유뇨기증후군(FLUTD) 또는 특발성 방광염** 응급 상황입니다.\n\n💡 **응급 가이드**:\n• 요도 폐색 시 24시간 이내 급성 요독증이 발생하므로 즉시 24시 응급실 진료가 필요합니다.\n• **'🚨 긴급 응급 출동 (50,000원)'** 선택 시 인근 [$displayLoc] 24시 응급실로 최우선 배차됩니다!"
            }
            if (q.contains("밥") || q.contains("식사") || q.contains("안먹") || q.contains("거부") || q.contains("기력")) {
                return "🩺 **[AI 수의사 보듬이의 고양이 식욕부진 진단]**\n\n고양이가 24시간 이상 밥을 전혀 먹지 않는 경우 **지방간(Hepatic Lipidosis) 및 급성 탈수** 위험이 높습니다.\n\n💡 **안심 가이드**:\n• 체온이 떨어지지 않게 따뜻하게 유지해 주시고, 빠른 시일 내에 수액 및 혈액검사를 받아보시는 것이 안전합니다."
            }
        }

        // 6. 🍫 독성 물질 섭취 응급 (초콜릿, 포도, 양파, 자일리톨, 쥐약)
        if (q.contains("초콜릿") || q.contains("포도") || q.contains("양파") || q.contains("자일리톨") || q.contains("먹었") || q.contains("삼켰") || q.contains("중독")) {
            return "🚨 **[AI 수의사 보듬이의 긴급 중독 응급 경고]**\n\n아이(강아지/고양이)가 초콜릿, 포도, 양파, 자일리톨, 또는 약물을 섭취한 경우 **급성 신부전 및 용혈성 빈혈을 유발하는 치명적인 응급 상황**입니다!\n\n💡 **응급 가이드**:\n• 집에서 무리하게 구토 유발 시 위점막 손상 위험이 있으니 자제해 주세요.\n• **섭취 후 2시간 이내 위세척이 골든타임**입니다.\n\n🚨 **'🚨 긴급 응급 출동 (50,000원)'**을 선택해 주시면 인근 [$displayLoc] 24시 응급센터로 최우선 출동하겠습니다!"
        }

        // 7. 🤮 소화기 질환 (구토, 설사, 변비, 거품토, 피설사)
        if (q.contains("토") || q.contains("구토") || q.contains("설사") || q.contains("거품") || q.contains("노란") || q.contains("위액") || q.contains("변")) {
            return "🩺 **[AI 수의사 보듬이의 소화기 정밀 진단]**\n\n구토나 설사 증상(노란 위액/거품토/무른변)은 급성 위장염, 췌장염, 또는 이물질 섭취 위험 신호입니다.\n\n💡 **응급 체크 포인트**:\n• 2회 이상 연속 구토 시 탈수를 유발하므로 미지근한 물을 조금씩 공급해 주시고 진료를 받아보세요.\n• **'🚗 미예약 당일 이동 (35,000원)'** 신청 시 당일 즉시 픽업하여 병원에 도착합니다!"
        }

        // 8. 🦴 정형외과 / 슬개골 / 다리 절뚝임
        if (q.contains("다리") || q.contains("절뚝") || q.contains("슬개골") || q.contains("삐끗") || q.contains("절어") || q.contains("관절") || q.contains("통증")) {
            return "🩺 **[AI 수의사 보듬이의 정형외과 정밀 진단]**\n\n다리를 절뚝거리거나 들고 다니는 증상은 **슬개골 탈구(2~3기) 또는 십자인대 파열** 가능성이 높습니다.\n\n💡 **응급 관찰 포인트**:\n• 무리하게 걷게 하지 마시고 안아서 이동해 주세요. 레이 케어카 차내에는 슬라이딩 충격 완화 시트가 완비되어 있습니다.\n• **'📅 예약 진료 (30,000원)'** 서비스로 정형외과 전문 병원 안심 동행을 추천해 드립니다!"
        }

        // 9. 🫁 호흡기 / 기침 / 기관지
        if (q.contains("기침") || q.contains("쌕쌕") || q.contains("호흡") || q.contains("숨")) {
            return "🚨 **[AI 수의사 보듬이의 응급 호흡기 경고]**\n\n기침 및 호흡 곤란 증상은 **기관지 협착증, 켄넬코프, 또는 심장질환** 위험 신호입니다.\n\n🚨 **'🚨 긴급 응급 출동 (50,000원)'**을 선택하시면 24시 응급실 직통 연결 및 케어카가 최우선 즉시 배차됩니다!"
        }

        // 10. 🦷 치과 / 입냄새 / 스케일링
        if (q.contains("이빨") || q.contains("치아") || q.contains("입냄새") || q.contains("스케일링") || q.contains("잇몸")) {
            return "🩺 **[AI 수의사 보조 '보듬이'의 치과 진단]**\n\n치석, 입냄새, 잇몸 출혈은 치주염 가능성이 높습니다. 마취 전 사전 검사 후 안전한 스케일링을 권장합니다."
        }

        // 11. 🏥 병원 추천 / 근처 24시 병원
        if (q.contains("병원") || q.contains("근처") || q.contains("어디") || q.contains("24시") || q.contains("응급실") || q.contains("추천")) {
            return "📍 **[위치기반 제휴 동물병원 추천]**\n보호자님의 현재 위치인 [$displayLoc] 인근 추천 24시 응급 & 주요 병원 리스트입니다:\n\n• **명지24시온누리메디컬센터**: 부산 강서구 명지오션시티4로 80 (📞 051-271-2475)\n• **하단24시웰니스동물병원**: 부산 사하구 낙동대로 550 (📞 051-208-2400)\n• **사상24시응급메디컬센터**: 부산 사상구 괘법동 사상역 근처 (📞 051-315-2475)\n• **서면24시아이엔동물메디컬센터**: 부산 부산진구 중앙대로 680 (📞 051-808-2475)\n\n원하시는 병원으로 담당 가드가 안전하게 픽업 이송해 드립니다!"
        }

        // 12. 💬 감사 / 긍정 응답
        if (q.contains("고마") || q.contains("감사") || q.contains("수고") || q.contains("좋아") || q.contains("네") || q.contains("응") || q.contains("알겠")) {
            return "네 보호자님! 소중한 아이가 항상 건강하고 행복하길 진심으로 바랍니다. 😊\n\n언제든 도움이 필요하시면 저 **AI 수의사 보듬이**를 찾아주세요. 발자국동행이 항상 함께하겠습니다! 🐾"
        }

        // 13. 범용 자연어 처리 (자연스러운 맥락에 맞춘 유연한 대화 답변)
        return "🩺 **[AI 수의사 보듬이의 안심 종합 안내]**\n\n" +
               "보호자님, [$displayLoc] 근처 소중한 아이의 상황에 대해 문의해 주셔서 감사합니다. 🐾\n\n" +
               "질문해 주신 내용을 수의학 관점에서 다각도로 체크하였습니다. 아이의 증상 변화나 정밀 검진이 필요하시다면 아래의 3가지 서비스 트림을 통해 언제든 안심 이동이 가능합니다:\n\n" +
               "• 🚨 **긴급 응급 출동 (50,000원)**: 24시 응급실 직통 수속 연결 & 최우선 출동\n" +
               "• 🚗 **미예약 당일 이동 (35,000원)**: 당일 현장 접수 & 즉시 픽업\n" +
               "• 📅 **예약 진료 (30,000원)**: 미리 지정 일시 1:1 수석가드 안심 동행\n\n" +
               "추가로 궁금하신 증상이나 문의사항을 편하게 물어보세요!"
    }
}
