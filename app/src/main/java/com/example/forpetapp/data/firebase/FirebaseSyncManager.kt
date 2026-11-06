@file:Suppress("DEPRECATION", "ApplySharedPref")
package com.example.forpetapp.data.firebase

import android.content.Context
import android.util.Log
import com.example.forpetapp.data.model.BookingItem
import com.example.forpetapp.service.ForPetFirebaseMessagingService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * ☁️ 발자국동행 실시간 Cloud Firestore 통합 동기화 매니저
 * - 보호자 스마트폰 ↔ 담당 가드 앱 ↔ PC 웹 관리자 포털 간 100% 실시간 3자 동기화
 * - 오더 생성/수정, 6단계 동선 상태, 가드 차량 실시간 GPS 위치 스트리밍 처리
 */
object FirebaseSyncManager {

    private const val TAG = "ForPetFirebaseSync"
    private const val USERS_COLLECTION = "users"
    private const val ORDERS_COLLECTION = "orders"
    private const val GUARD_LIVE_COLLECTION = "guard_live"
    private const val CAR_DOC_ID = "ray_care_car"
    private const val PREF_OFFLINE_SYNC = "ForPetOfflineSyncPrefs"
    private const val KEY_PENDING_ORDERS = "pending_orders_queue"
    private const val KEY_PENDING_STATUSES = "pending_statuses_queue"

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
            val activeNet = cm.activeNetworkInfo
            activeNet != null && activeNet.isConnectedOrConnecting
        } catch (_: Exception) {
            true
        }
    }

    fun getPendingSyncCount(context: Context): Int {
        return try {
            val prefs = context.getSharedPreferences(PREF_OFFLINE_SYNC, Context.MODE_PRIVATE)
            val orders = org.json.JSONArray(prefs.getString(KEY_PENDING_ORDERS, "[]") ?: "[]")
            val statuses = org.json.JSONArray(prefs.getString(KEY_PENDING_STATUSES, "[]") ?: "[]")
            orders.length() + statuses.length()
        } catch (_: Exception) {
            0
        }
    }

    private fun enqueuePendingOrder(context: Context, booking: BookingItem) {
        try {
            val prefs = context.getSharedPreferences(PREF_OFFLINE_SYNC, Context.MODE_PRIVATE)
            val existingJson = prefs.getString(KEY_PENDING_ORDERS, "[]") ?: "[]"
            val array = org.json.JSONArray(existingJson)
            val obj = org.json.JSONObject().apply {
                put("id", booking.id)
                put("serviceTrim", booking.serviceTrim)
                put("serviceType", booking.serviceType)
                put("species", booking.species)
                put("petName", booking.petName)
                put("ownerName", booking.ownerName)
                put("hospitalName", booking.hospitalName)
                put("time", booking.time)
                put("status", booking.status)
                put("vehicleInfo", booking.vehicleInfo)
                put("symptomNote", booking.symptomNote)
                put("estimatedPrice", booking.estimatedPrice)
                put("isCompleted", booking.isCompleted)
                put("doctorNote", booking.doctorNote)
                put("prescription", booking.prescription)
                put("receiptImageUri", booking.receiptImageUri ?: "")
                put("prescriptionImageUri", booking.prescriptionImageUri ?: "")
                put("doorlockPhotoUri", booking.doorlockPhotoUri ?: "")
            }
            array.put(obj)
            prefs.edit().putString(KEY_PENDING_ORDERS, array.toString()).apply()
            Log.d(TAG, "📴 Order queued for offline sync: ${booking.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Enqueue pending order error: ${e.message}")
        }
    }

    private fun enqueuePendingStatus(context: Context, orderId: String, status: String) {
        try {
            val prefs = context.getSharedPreferences(PREF_OFFLINE_SYNC, Context.MODE_PRIVATE)
            val existingJson = prefs.getString(KEY_PENDING_STATUSES, "[]") ?: "[]"
            val array = org.json.JSONArray(existingJson)
            val obj = org.json.JSONObject().apply {
                put("orderId", orderId)
                put("status", status)
                put("timestamp", System.currentTimeMillis())
            }
            array.put(obj)
            prefs.edit().putString(KEY_PENDING_STATUSES, array.toString()).apply()
            Log.d(TAG, "📴 Status queued for offline sync: $orderId -> $status")
        } catch (e: Exception) {
            Log.e(TAG, "Enqueue pending status error: ${e.message}")
        }
    }

    fun drainPendingQueue(context: Context) {
        if (!isNetworkConnected(context)) return
        try {
            val prefs = context.getSharedPreferences(PREF_OFFLINE_SYNC, Context.MODE_PRIVATE)
            val orderJson = prefs.getString(KEY_PENDING_ORDERS, "[]") ?: "[]"
            val orderArr = org.json.JSONArray(orderJson)
            if (orderArr.length() > 0) {
                for (i in 0 until orderArr.length()) {
                    val obj = orderArr.getJSONObject(i)
                    val booking = BookingItem(
                        id = obj.getString("id"),
                        serviceTrim = obj.optString("serviceTrim", "📅 예약 진료 (예약 O)"),
                        serviceType = obj.optString("serviceType", "병원 진료 대행"),
                        species = obj.optString("species", "강아지"),
                        petName = obj.optString("petName", "초코"),
                        ownerName = obj.optString("ownerName", "보호자"),
                        hospitalName = obj.optString("hospitalName", "명지24시온누리메디컬센터"),
                        time = obj.optString("time", "오늘 오전 10:00"),
                        status = obj.optString("status", "예약 확정 (배차 완료)"),
                        vehicleInfo = obj.optString("vehicleInfo", "발자국동행 레이"),
                        symptomNote = obj.optString("symptomNote", ""),
                        estimatedPrice = obj.optString("estimatedPrice", "30,000원"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        doctorNote = obj.optString("doctorNote", ""),
                        prescription = obj.optString("prescription", ""),
                        receiptImageUri = obj.optString("receiptImageUri").ifEmpty { null },
                        prescriptionImageUri = obj.optString("prescriptionImageUri").ifEmpty { null },
                        doorlockPhotoUri = obj.optString("doorlockPhotoUri").ifEmpty { null }
                    )
                    syncOrderToCloud(booking, context)
                }
                prefs.edit().putString(KEY_PENDING_ORDERS, "[]").apply()
                Log.d(TAG, "📴 Drained and uploaded ${orderArr.length()} offline pending orders.")
            }

            val statusJson = prefs.getString(KEY_PENDING_STATUSES, "[]") ?: "[]"
            val statusArr = org.json.JSONArray(statusJson)
            if (statusArr.length() > 0) {
                for (i in 0 until statusArr.length()) {
                    val obj = statusArr.getJSONObject(i)
                    val orderId = obj.getString("orderId")
                    val status = obj.getString("status")
                    firestore.collection(ORDERS_COLLECTION).document(orderId)
                        .update("status", status, "updatedAt", System.currentTimeMillis())
                }
                prefs.edit().putString(KEY_PENDING_STATUSES, "[]").apply()
                Log.d(TAG, "📴 Drained and updated ${statusArr.length()} offline pending statuses.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drain pending queue error: ${e.message}")
        }
    }

    /**
     * 1. 신규 배차/진료 예약 오더를 Cloud Firestore로 업로드 및 동기화 (오프라인 큐 보증)
     */
    fun syncOrderToCloud(booking: BookingItem, context: Context? = null, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val orderMap = hashMapOf(
                "id" to booking.id,
                "userId" to booking.userId,
                "userEmail" to booking.userEmail,
                "serviceTrim" to booking.serviceTrim,
                "serviceType" to booking.serviceType,
                "species" to booking.species,
                "petName" to booking.petName,
                "ownerName" to booking.ownerName,
                "ownerPhone" to booking.ownerPhone,
                "pickupAddress" to booking.pickupAddress,
                "hospitalName" to booking.hospitalName,
                "time" to booking.time,
                "status" to booking.status,
                "vehicleInfo" to booking.vehicleInfo,
                "symptomNote" to booking.symptomNote,
                "estimatedPrice" to booking.estimatedPrice,
                "isCompleted" to booking.isCompleted,
                "doctorNote" to booking.doctorNote,
                "prescription" to booking.prescription,
                "receiptImageUri" to (booking.receiptImageUri ?: ""),
                "prescriptionImageUri" to (booking.prescriptionImageUri ?: ""),
                "doorlockPhotoUri" to (booking.doorlockPhotoUri ?: ""),
                "updatedAt" to System.currentTimeMillis(),
            )

            firestore.collection(ORDERS_COLLECTION)
                .document(booking.id)
                .set(orderMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Order successfully synced to cloud: ${booking.id}")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync order: ${e.message}")
                    if (context != null) enqueuePendingOrder(context, booking)
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sync order exception: ${e.message}")
            if (context != null) enqueuePendingOrder(context, booking)
            onComplete?.invoke(false)
        }
    }

    /**
     * 2. 가드 이동 6단계 동선 상태 실시간 업데이트 및 보호자 푸시 알림 트리거 (오프라인 큐 보증)
     */
    fun updateOrderStatus(context: Context, orderId: String, newStatus: String, petName: String = "아이") {
        try {
            firestore.collection(ORDERS_COLLECTION)
                .document(orderId)
                .update(
                    "status", newStatus,
                    "updatedAt", System.currentTimeMillis(),
                )
                .addOnSuccessListener {
                    Log.d(TAG, "Order status updated: $orderId -> $newStatus")
                    ForPetFirebaseMessagingService.sendLocalNotification(
                        context,
                        "🐾 [발자국동행 케어 현황] $petName",
                        newStatus,
                    )
                    try {
                        com.example.forpetapp.widget.ForPetAppWidgetProvider.updateAllWidgets(context)
                    } catch (we: Exception) {
                        Log.e(TAG, "Widget update failed: ${we.message}")
                    }
                }
                .addOnFailureListener {
                    enqueuePendingStatus(context, orderId, newStatus)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Update order status failed: ${e.message}")
            enqueuePendingStatus(context, orderId, newStatus)
        }
    }

    /**
     * 3. 가드 실시간 GPS 좌표를 Firestore로 영구 전송 (관리자 포털 & 보호자 지도 실시간 수신)
     */
    fun updateGuardLiveGps(lat: Double, lng: Double, guardName: String = "박찬헌 가드", carPlate: String = "부산 38가 5394") {
        try {
            val gpsData = hashMapOf(
                "lat" to lat,
                "lng" to lng,
                "guardName" to guardName,
                "carPlate" to carPlate,
                "updatedAt" to System.currentTimeMillis(),
            )

            firestore.collection(GUARD_LIVE_COLLECTION)
                .document(CAR_DOC_ID)
                .set(gpsData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Guard GPS synced: $lat, $lng")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Guard GPS sync failed: ${e.message}")
        }
    }

    /**
     * 4. Cloud Firestore의 전체 오더 목록 실시간 스냅샷 리스너
     */
    fun listenToCloudOrders(onOrdersUpdated: (List<BookingItem>) -> Unit): ListenerRegistration? {
        return try {
            firestore.collection(ORDERS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    if ((snapshot != null) && !snapshot.isEmpty) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                BookingItem(
                                    id = doc.getString("id") ?: doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    userEmail = doc.getString("userEmail") ?: "",
                                    serviceTrim = doc.getString("serviceTrim") ?: "📅 예약 진료 (예약 O)",
                                    serviceType = doc.getString("serviceType") ?: "병원 진료 대행",
                                    species = doc.getString("species") ?: "강아지",
                                    petName = doc.getString("petName") ?: "아이",
                                    ownerName = doc.getString("ownerName") ?: "보호자",
                                    hospitalName = doc.getString("hospitalName") ?: "동물병원",
                                    time = doc.getString("time") ?: "",
                                    status = doc.getString("status") ?: "예약 확정",
                                    vehicleInfo = doc.getString("vehicleInfo") ?: "발자국동행 레이 케어카",
                                    symptomNote = doc.getString("symptomNote") ?: "",
                                    estimatedPrice = doc.getString("estimatedPrice") ?: "30,000원",
                                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                                    doctorNote = doc.getString("doctorNote") ?: "",
                                    prescription = doc.getString("prescription") ?: "",
                                    receiptImageUri = doc.getString("receiptImageUri"),
                                    prescriptionImageUri = doc.getString("prescriptionImageUri"),
                                    doorlockPhotoUri = doc.getString("doorlockPhotoUri"),
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        onOrdersUpdated(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToCloudOrders error: ${e.message}")
            null
        }
    }

    /**
     * 5. 가드 차량 실시간 GPS 스냅샷 리스너 (보호자 및 관리자 뷰)
     */
    fun listenToGuardGps(onGpsUpdated: (Double, Double) -> Unit): ListenerRegistration? {
        return try {
            firestore.collection(GUARD_LIVE_COLLECTION)
                .document(CAR_DOC_ID)
                .addSnapshotListener { snapshot, error ->
                    if ((error != null) || (snapshot == null) || !snapshot.exists()) return@addSnapshotListener
                    val lat = snapshot.getDouble("lat") ?: 35.0830000
                    val lng = snapshot.getDouble("lng") ?: 128.8950000
                    onGpsUpdated(lat, lng)
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToGuardGps error: ${e.message}")
            null
        }
    }

    /**
     * 6. 1:1 안심톡 메시지 Firestore 전송
     */
    fun sendChatMessage(orderId: String, chatMessage: com.example.forpetapp.data.model.ChatMessage, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val msgData = hashMapOf(
                "id" to chatMessage.id,
                "senderName" to chatMessage.senderName,
                "isDriver" to chatMessage.isDriver,
                "messageText" to chatMessage.messageText,
                "timestamp" to chatMessage.timestamp,
                "createdAt" to System.currentTimeMillis(),
            )

            firestore.collection(ORDERS_COLLECTION)
                .document(orderId)
                .collection("chats")
                .document(chatMessage.id)
                .set(msgData)
                .addOnSuccessListener {
                    onComplete?.invoke(true)
                }
                .addOnFailureListener {
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "sendChatMessage error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 7. 1:1 안심톡 메시지 실시간 스냅샷 리스너
     */
    fun listenToChatMessages(
        orderId: String,
        onMessagesUpdated: (List<com.example.forpetapp.data.model.ChatMessage>) -> Unit,
    ): ListenerRegistration? {
        return try {
            firestore.collection(ORDERS_COLLECTION)
                .document(orderId)
                .collection("chats")
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "listenToChatMessages error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                com.example.forpetapp.data.model.ChatMessage(
                                    id = doc.getString("id") ?: doc.id,
                                    senderName = doc.getString("senderName") ?: "알 수 없음",
                                    isDriver = doc.getBoolean("isDriver") ?: false,
                                    messageText = doc.getString("messageText") ?: "",
                                    timestamp = doc.getString("timestamp") ?: "",
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                        onMessagesUpdated(messages)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToChatMessages failed: ${e.message}")
            null
        }
    }

    /**
     * 8. 회원 정보(구글/카카오/휴대폰 인증) Cloud Firestore users 컬렉션 동기화
     */
    fun syncUserProfileToCloud(
        userId: String,
        name: String,
        phone: String,
        email: String = "",
        provider: String = "일반",
        address: String = "",
        detailAddress: String = "",
        points: Int = 0,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val userMap = hashMapOf<String, Any>(
                "userId" to userId,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "provider" to provider,
                "address" to address,
                "detailAddress" to detailAddress,
                "points" to points,
                "role" to "USER",
                "lastLoginAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection(USERS_COLLECTION).document(userId)
            docRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    userMap["createdAt"] = System.currentTimeMillis()
                }
                docRef.set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "User profile successfully synced to Firestore: $userId ($name)")
                        onComplete?.invoke(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to sync user profile: ${e.message}")
                        onComplete?.invoke(false)
                    }
            }.addOnFailureListener {
                userMap["createdAt"] = System.currentTimeMillis()
                docRef.set(userMap, SetOptions.merge())
                    .addOnCompleteListener { onComplete?.invoke(it.isSuccessful) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUserProfileToCloud error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 9. Cloud Firestore 전체 가입 회원 목록 실시간 동기화 리스너 (관리자 대시보드 연동)
     */
    fun listenToCloudUsers(onUsersUpdated: (List<Triple<String, String, String>>) -> Unit): ListenerRegistration? {
        return try {
            firestore.collection(USERS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "listenToCloudUsers error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val name = doc.getString("name") ?: "발자국보호자"
                                val phone = doc.getString("phone") ?: ""
                                val email = doc.getString("email") ?: ""
                                val provider = doc.getString("provider") ?: "일반"
                                val points = doc.getLong("points")?.toInt() ?: 0
                                val providerIcon = when (provider) {
                                    "Google" -> "🔴 Google"
                                    "Kakao" -> "💛 카카오톡"
                                    else -> "🔐 일반"
                                }
                                val formattedPoints = java.text.NumberFormat.getNumberInstance(java.util.Locale.KOREA).format(points)
                                val detail = "$providerIcon 계정${if (email.isNotBlank()) " ($email)" else ""} | ${formattedPoints}P"
                                Triple(name, if (phone.isNotBlank()) phone else "연락처 미등록", detail)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        onUsersUpdated(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToCloudUsers error: ${e.message}")
            null
        }
    }

    /**
     * 5. 앱 필수 최소 버전 실시간 감지 (강제 업데이트 체크)
     */
    fun listenToAppVersionConfig(
        onUpdateRequired: (minVersionCode: Long, latestVersionName: String, updateMessage: String) -> Unit
    ) {
        try {
            firestore.collection("app_config")
                .document("version_control")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }
                    val minVersionCode = snapshot.getLong("minVersionCode") ?: 1L
                    val latestVersionName = snapshot.getString("latestVersionName") ?: "최신버전"
                    val updateMessage = snapshot.getString("updateMessage")
                        ?: "안정적인 서비스 이용과 보안 강화를 위해 최신 버전 업데이트가 필요합니다. 구글 플레이스토어로 이동하여 업데이트해 주세요."

                    onUpdateRequired(minVersionCode, latestVersionName, updateMessage)
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToAppVersionConfig error: ${e.message}")
        }
    }

    /**
     * 기본 버전 설정 초기 생성/보장 (관리자 또는 최초 1회)
     */
    fun initDefaultVersionConfigIfMissing(currentVersionCode: Long, currentVersionName: String) {
        try {
            val docRef = firestore.collection("app_config").document("version_control")
            docRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val initialConfig = hashMapOf(
                        "minVersionCode" to currentVersionCode,
                        "latestVersionName" to currentVersionName,
                        "updateMessage" to "안정적인 서비스 이용과 보안 강화를 위해 최신 버전 업데이트가 필요합니다. 구글 플레이스토어로 이동하여 업데이트해 주세요.",
                        "updatedAt" to System.currentTimeMillis()
                    )
                    docRef.set(initialConfig)
                }
            }
        } catch (_: Exception) {}
    }
}
