package com.example.forpetapp.data.model

// 🏥 병원 정보 데이터 클래스 (부산 구별 수동 등록 & 진료 과목/부가 서비스 지원)
data class HospitalInfo(
    val name: String,
    val region: String,
    val type: String,
    val address: String,
    val phone: String,
    val targetSpecies: List<String> = listOf("개", "고양이"),
    val departments: List<String> = emptyList(),
    val additionalServices: List<String> = emptyList(),
)

// 📱 화면 전환 상태 머신
enum class ScreenState {
    LOGIN, SIGNUP, MAIN, DRIVER, ADMIN
}

// 🐾 다중 반려동물 프로필 정보 데이터 클래스 (다견/다묘 가정 100% 등록 지원)
data class PetProfile(
    val id: String = System.currentTimeMillis().toString(),
    val imageUriStr: String? = null,
    val species: String = "강아지",
    val name: String = "초코",
    val ownerRealName: String = "홍길동",
    val type: String = "말티즈",
    val gender: String = "남아",
    val neutered: Boolean = true,
    val weight: String = "4.5",
    val hospital: String = "명지24시온누리메디컬센터",
    val symptoms: String = "슬개골 탈구 2기, 정기 검진 권장",
    val birthMillis: Long? = null,
)

// 📅 예약 정보 데이터 클래스 (3가지 트림: 긴급 50,000원 / 당일미예약 35,000원 / 예약진료 30,000원)
data class BookingItem(
    val id: String = System.currentTimeMillis().toString(),
    val serviceTrim: String = "📅 예약 진료 (예약 O)",
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
    val prescription: String = "",
    val receiptImageUri: String? = null,
    val prescriptionImageUri: String? = null,
    val doorlockPhotoUri: String? = null,
)

// 💬 1:1 안심 톡 메시지 데이터 클래스
data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val senderName: String,
    val isDriver: Boolean,
    val messageText: String,
    val timestamp: String,
)

// 💊 일일 복약 알리미 항목
data class MedicationItem(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val time: String,
    val dosage: String,
    val note: String = "",
    var isTaken: Boolean = false,
)

// 💉 연간 예방접종 캘린더 항목
data class VaccineScheduleItem(
    val id: String = System.currentTimeMillis().toString(),
    val vaccineName: String,
    val targetDate: String,
    val cycle: String,
    val dDay: Int,
    val description: String,
    val isCompleted: Boolean = false,
)

// 🎟️ 할인 쿠폰 모델
data class CouponItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val discountAmount: Int,
    val minOrderAmount: Int = 30000,
    val validUntil: String = "2027.12.31",
    val isUsed: Boolean = false,
)

// 📊 반려동물 의료 가계부 항목 모델
data class PetExpenseItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val category: String = "🏥 병원 진료비",
    val amount: Int,
    val dateText: String,
    val hospitalOrStore: String,
    val receiptUriStr: String? = null,
    val note: String = ""
)

