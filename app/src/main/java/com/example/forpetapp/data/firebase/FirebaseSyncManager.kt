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

    /**
     * 📸 스마트폰 로컬 URI 이미지를 압축된 Base64 Data URI로 변환 (가드 ↔ 보호자 ↔ PC 홈페이지 간 100% 사진 공유)
     */
    fun encodeImageUriToDataUrl(context: Context, uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        if (uriStr.startsWith("data:image")) return uriStr
        return try {
            val uri = android.net.Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val original = android.graphics.BitmapFactory.decodeStream(input) ?: return uriStr
                val maxDim = 480
                val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1.0f)
                val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
                val scaled = android.graphics.Bitmap.createScaledBitmap(original, targetW, targetH, true)
                val baos = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            } ?: uriStr
        } catch (_: Exception) {
            uriStr
        }
    }

    fun decodeImageFromUriOrDataUrl(context: Context, uriOrData: String?): android.graphics.Bitmap? {
        if (uriOrData.isNullOrBlank()) return null
        return try {
            if (uriOrData.startsWith("data:image")) {
                val base64Part = uriOrData.substringAfter("base64,", "")
                if (base64Part.isNotEmpty()) {
                    val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else null
            } else {
                val uri = android.net.Uri.parse(uriOrData)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
            }
        } catch (_: Exception) {
            null
        }
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
            val encodedReceipt = if (context != null) encodeImageUriToDataUrl(context, booking.receiptImageUri) else booking.receiptImageUri
            val encodedPrescription = if (context != null) encodeImageUriToDataUrl(context, booking.prescriptionImageUri) else booking.prescriptionImageUri
            val encodedDoorlock = if (context != null) encodeImageUriToDataUrl(context, booking.doorlockPhotoUri) else booking.doorlockPhotoUri

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
                "receiptImageUri" to (encodedReceipt ?: ""),
                "prescriptionImageUri" to (encodedPrescription ?: ""),
                "doorlockPhotoUri" to (encodedDoorlock ?: ""),
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
     * 1-2. 오더 취소/삭제 (보호자 실수 취소 또는 관리자 삭제 시 Firestore에서 즉시 제거)
     */
    fun deleteOrderFromCloud(orderId: String, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            firestore.collection(ORDERS_COLLECTION)
                .document(orderId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Order successfully deleted from Firestore: $orderId")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete order from Firestore: ${e.message}")
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Delete order exception: ${e.message}")
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

                    if (snapshot != null) {
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
                                    ownerPhone = doc.getString("ownerPhone") ?: "",
                                    pickupAddress = doc.getString("pickupAddress") ?: "",
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
        points: Int = -1,
        petSummary: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val targetId = userId.ifBlank { email.ifBlank { phone } }.trim()
            if (targetId.isBlank()) {
                onComplete?.invoke(false)
                return
            }
            val userMap = hashMapOf<String, Any>(
                "userId" to targetId,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "provider" to provider,
                "address" to address,
                "detailAddress" to detailAddress,
                "role" to "USER",
                "lastLoginAt" to System.currentTimeMillis()
            )
            if (points >= 0) {
                userMap["points"] = points
            }
            if (!petSummary.isNullOrBlank()) {
                userMap["pet"] = petSummary
            }

            val docRef = firestore.collection(USERS_COLLECTION).document(targetId)
            docRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    userMap["createdAt"] = System.currentTimeMillis()
                    if (points < 0) {
                        userMap["points"] = 1000
                    }
                } else if (points < 0 && !doc.contains("points")) {
                    userMap["points"] = 1000
                }
                docRef.set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "User profile successfully synced to Firestore: $targetId ($name)")
                        onComplete?.invoke(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to sync user profile: ${e.message}")
                        onComplete?.invoke(false)
                    }
            }.addOnFailureListener {
                userMap["createdAt"] = System.currentTimeMillis()
                if (points < 0) {
                    userMap["points"] = 1000
                }
                docRef.set(userMap, SetOptions.merge())
                    .addOnCompleteListener { onComplete?.invoke(it.isSuccessful) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUserProfileToCloud error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 8-1. Cloud Firestore users 컬렉션에서 회원 존재 여부 확인 (탈퇴 여부 및 미가입 검증)
     */
    fun checkUserExistsInCloud(
        userId: String,
        email: String = "",
        phone: String = "",
        onResult: (exists: Boolean, userData: Map<String, Any>?) -> Unit
    ) {
        try {
            val validUserId = userId.trim()
            val validEmail = email.trim()
            val validPhone = phone.trim()

            if (validUserId.isBlank() && validEmail.isBlank() && validPhone.isBlank()) {
                onResult(false, null)
                return
            }

            val targetDocId = if (validUserId.isNotBlank()) validUserId else validEmail
            firestore.collection(USERS_COLLECTION).document(targetDocId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.data != null) {
                        onResult(true, doc.data)
                    } else if (validEmail.isNotBlank() && validEmail != targetDocId) {
                        firestore.collection(USERS_COLLECTION).document(validEmail).get()
                            .addOnSuccessListener { emailDoc ->
                                if (emailDoc.exists() && emailDoc.data != null) {
                                    onResult(true, emailDoc.data)
                                } else {
                                    checkByFieldQuery(validEmail, validPhone, onResult)
                                }
                            }
                            .addOnFailureListener {
                                checkByFieldQuery(validEmail, validPhone, onResult)
                            }
                    } else {
                        checkByFieldQuery(validEmail, validPhone, onResult)
                    }
                }
                .addOnFailureListener {
                    checkByFieldQuery(validEmail, validPhone, onResult)
                }
        } catch (e: Exception) {
            Log.e(TAG, "checkUserExistsInCloud error: ${e.message}")
            onResult(false, null)
        }
    }

    private fun checkByFieldQuery(
        email: String,
        phone: String,
        onResult: (exists: Boolean, userData: Map<String, Any>?) -> Unit
    ) {
        if (email.isNotBlank()) {
            firestore.collection(USERS_COLLECTION)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { qs ->
                    if (!qs.isEmpty && qs.documents[0].data != null) {
                        onResult(true, qs.documents[0].data)
                    } else if (phone.isNotBlank()) {
                        firestore.collection(USERS_COLLECTION)
                            .whereEqualTo("phone", phone)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { pqs ->
                                if (!pqs.isEmpty && pqs.documents[0].data != null) {
                                    onResult(true, pqs.documents[0].data)
                                } else {
                                    onResult(false, null)
                                }
                            }
                            .addOnFailureListener { onResult(false, null) }
                    } else {
                        onResult(false, null)
                    }
                }
                .addOnFailureListener { onResult(false, null) }
        } else if (phone.isNotBlank()) {
            firestore.collection(USERS_COLLECTION)
                .whereEqualTo("phone", phone)
                .limit(1)
                .get()
                .addOnSuccessListener { pqs ->
                    if (!pqs.isEmpty && pqs.documents[0].data != null) {
                        onResult(true, pqs.documents[0].data)
                    } else {
                        onResult(false, null)
                    }
                }
                .addOnFailureListener { onResult(false, null) }
        } else {
            onResult(false, null)
        }
    }

    /**
     * 8-2. 회원 탈퇴 시 Cloud Firestore users 컬렉션에서 해당 회원 문서 영구 삭제
     */
    fun deleteUserProfileFromCloud(
        userId: String,
        email: String = "",
        phone: String = "",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val docIds = listOf(userId, email, phone).map { it.trim() }.filter { it.isNotBlank() }.distinct()
            docIds.forEach { id ->
                firestore.collection(USERS_COLLECTION).document(id).delete()
            }

            // 1. users 컬렉션에서 userId, email, phone 일치 문서 전수 삭제
            if (userId.isNotBlank()) {
                firestore.collection(USERS_COLLECTION).whereEqualTo("userId", userId.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }
            if (phone.isNotBlank()) {
                firestore.collection(USERS_COLLECTION).whereEqualTo("phone", phone.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }
            if (email.isNotBlank()) {
                firestore.collection(USERS_COLLECTION).whereEqualTo("email", email.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }

            // 2. orders 컬렉션에서도 탈퇴 회원의 예약/오더 내역 전수 삭제 (DB 및 홈페이지 완전 파기)
            if (userId.isNotBlank()) {
                firestore.collection(ORDERS_COLLECTION).whereEqualTo("userId", userId.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }
            if (email.isNotBlank()) {
                firestore.collection(ORDERS_COLLECTION).whereEqualTo("userEmail", email.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }
            if (phone.isNotBlank()) {
                firestore.collection(ORDERS_COLLECTION).whereEqualTo("ownerPhone", phone.trim()).get()
                    .addOnSuccessListener { qs -> qs.documents.forEach { it.reference.delete() } }
            }

            onComplete?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "deleteUserProfileFromCloud error: ${e.message}")
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

    /**
     * 10. 다중 반려동물, 복약 알리미, 예방접종, 의료 가계부, 보유 쿠폰 데이터를 Firestore users/{userId} 문서에 실시간 백업
     */
    fun syncUserExtendedDataToCloud(userId: String, fieldKey: String, jsonValue: String, timestamp: Long = System.currentTimeMillis()) {
        try {
            if (userId.isBlank()) return
            val updateMap = hashMapOf<String, Any>(
                fieldKey to jsonValue,
                "${fieldKey}_ts" to timestamp,
                "updatedAt" to timestamp
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId.trim())
                .set(updateMap, SetOptions.merge())
        } catch (e: Exception) {
            Log.e(TAG, "syncUserExtendedDataToCloud error: ${e.message}")
        }
    }

    /**
     * 10-1. 앱 실행/로그인 시 Firestore users/{userId} 문서에 백업된 다중 반려동물·복약·접종·가계부·쿠폰 데이터를 로컬로 자동 복원
     */
    fun restoreUserExtendedDataFromCloud(
        context: Context,
        userId: String,
        onRestored: (() -> Unit)? = null
    ) {
        try {
            if (userId.isBlank()) return
            firestore.collection(USERS_COLLECTION)
                .document(userId.trim())
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val prefs = context.getSharedPreferences("BaljagukAppDiskPrefs", Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                        var changed = false
                        val sanitized = userId.trim().replace("[^a-zA-Z0-9_]".toRegex(), "_")
                        val cloudPoints = doc.getLong("points")?.toInt()
                        if (cloudPoints != null) {
                            val localLogsKey = if (sanitized.isNotBlank()) "point_logs_json_$sanitized" else "point_logs_json"
                            val logsJson = prefs.getString(localLogsKey, null)
                            val hasSpent = logsJson?.contains("\"amount\":-") == true || logsJson?.contains("\"isEarned\":false") == true
                            val finalPoints = if (cloudPoints <= 0 && !hasSpent) 1000 else cloudPoints
                            editor.putInt("user_points", finalPoints)
                            if (sanitized.isNotBlank()) {
                                editor.putInt("user_points_$sanitized", finalPoints)
                            }
                            changed = true
                        }
                        listOf(
                            "multi_pet_list_json",
                            "medication_list_json",
                            "vaccine_list_json",
                            "pet_expense_list_json",
                            "coupon_list_json",
                            "point_logs_json"
                        ).forEach { key ->
                            val cloudVal = doc.getString(key)
                            val cloudTs = doc.getLong("${key}_ts") ?: 0L
                            val targetLocalKey = if (key == "point_logs_json" && sanitized.isNotBlank()) "point_logs_json_$sanitized" else key
                            val localVal = prefs.getString(targetLocalKey, null)
                            val localTs = prefs.getLong("${targetLocalKey}_ts", 0L)
                            if (cloudVal != null && (localVal == null || cloudTs > localTs)) {
                                editor.putString(targetLocalKey, cloudVal)
                                if (cloudTs > 0L) {
                                    editor.putLong("${targetLocalKey}_ts", cloudTs)
                                }
                                changed = true
                            }
                        }
                        if (changed) {
                            editor.apply()
                            onRestored?.invoke()
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "restoreUserExtendedDataFromCloud error: ${e.message}")
        }
    }

    /**
     * 11. 네이버 블로그 / 유튜브 포스팅 10,000P 이벤트 검수 신청 Firestore 저장
     */
    fun submitBlogYoutubeEventToCloud(
        userId: String,
        userName: String,
        userPhone: String,
        postUrl: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val cleanId = userId.ifBlank { userPhone }.trim().replace("[^a-zA-Z0-9_@.-]".toRegex(), "_")
            val docId = if (cleanId.isNotBlank()) "blog_$cleanId" else "blog_${System.currentTimeMillis()}"
            val data = hashMapOf<String, Any>(
                "id" to docId,
                "type" to "BLOG_YOUTUBE",
                "category" to "📝 네이버 블로그 / 유튜브 포스팅",
                "userId" to userId.ifBlank { userPhone },
                "userName" to userName,
                "userPhone" to userPhone,
                "postUrl" to postUrl,
                "content" to postUrl,
                "rewardPoints" to 10000,
                "status" to "검토 대기중 (관리자 확인 후 10,000P 지급)",
                "submittedAt" to System.currentTimeMillis()
            )
            firestore.collection("event_submissions")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    onComplete?.invoke(true)
                }
                .addOnFailureListener {
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "submitBlogYoutubeEventToCloud error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 11-2. 어플 내 생생후기 (사진 + 글 작성) 3,000P 관리자 검토 신청 Firestore 저장
     */
    fun submitPhotoReviewEventToCloud(
        userId: String,
        userName: String,
        userPhone: String,
        petInfo: String,
        reviewContent: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        try {
            val cleanId = userId.ifBlank { userPhone }.trim().replace("[^a-zA-Z0-9_@.-]".toRegex(), "_")
            val docId = if (cleanId.isNotBlank()) "review_${cleanId}_${System.currentTimeMillis() % 100000}" else "review_${System.currentTimeMillis()}"
            val data = hashMapOf<String, Any>(
                "id" to docId,
                "type" to "PHOTO_REVIEW",
                "category" to "📸 어플 내 생생후기 (사진+글 작성)",
                "userId" to userId.ifBlank { userPhone },
                "userName" to userName,
                "userPhone" to userPhone,
                "petInfo" to petInfo,
                "postUrl" to "[앱 내 포토후기] $reviewContent",
                "content" to reviewContent,
                "rewardPoints" to 3000,
                "status" to "검토 대기중 (관리자 확인 후 3,000P 지급)",
                "submittedAt" to System.currentTimeMillis()
            )
            firestore.collection("event_submissions")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    onComplete?.invoke(true)
                }
                .addOnFailureListener {
                    onComplete?.invoke(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "submitPhotoReviewEventToCloud error: ${e.message}")
            onComplete?.invoke(false)
        }
    }

    /**
     * 12. 홈페이지/앱 관리자가 회원의 DB 포인트를 지급하거나 차감할 때 Firestore 실시간 반영 (내역 및 1년 소멸일자 자동 기록)
     */
    fun adminAdjustUserPointsInCloud(
        targetDocId: String,
        deltaPoints: Int,
        reasonTitle: String,
        onComplete: ((Boolean, Int) -> Unit)? = null
    ) {
        try {
            val cleanDocId = targetDocId.trim()
            if (cleanDocId.isBlank()) {
                onComplete?.invoke(false, 0)
                return
            }
            val docRef = firestore.collection(USERS_COLLECTION).document(cleanDocId)
            docRef.get().addOnSuccessListener { doc ->
                val currentPts = doc?.getLong("points")?.toInt() ?: 1000
                val newPts = (currentPts + deltaPoints).coerceAtLeast(0)
                val existingLogsJson = doc?.getString("point_logs_json") ?: "[]"
                val logsArray = try {
                    org.json.JSONArray(existingLogsJson)
                } catch (_: Exception) {
                    org.json.JSONArray()
                }

                val sdfNow = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.KOREA)
                val sdfExpire = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA)
                val cal = java.util.Calendar.getInstance()
                val nowStr = sdfNow.format(cal.time)
                cal.add(java.util.Calendar.YEAR, 1)
                val isEarned = deltaPoints >= 0
                val expireText = if (isEarned) {
                    "${sdfExpire.format(cal.time)} 소멸 예정 (적립 1년 후 자동 소멸)"
                } else {
                    "관리자 차감 완료 (잔여: ${newPts}P)"
                }

                val newLogObj = org.json.JSONObject().apply {
                    put("id", "pt_${System.currentTimeMillis()}")
                    put("title", reasonTitle)
                    put("amount", deltaPoints)
                    put("dateText", nowStr)
                    put("expireDateText", expireText)
                    put("isEarned", isEarned)
                }

                val updatedArray = org.json.JSONArray()
                updatedArray.put(newLogObj)
                for (i in 0 until logsArray.length()) {
                    updatedArray.put(logsArray.getJSONObject(i))
                }

                val nowTs = System.currentTimeMillis()
                val updateData = hashMapOf<String, Any>(
                    "points" to newPts,
                    "point_logs_json" to updatedArray.toString(),
                    "point_logs_json_ts" to nowTs,
                    "updatedAt" to nowTs
                )
                docRef.set(updateData, SetOptions.merge())
                    .addOnSuccessListener {
                        onComplete?.invoke(true, newPts)
                    }
                    .addOnFailureListener {
                        onComplete?.invoke(false, currentPts)
                    }
            }.addOnFailureListener {
                onComplete?.invoke(false, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "adminAdjustUserPointsInCloud error: ${e.message}")
            onComplete?.invoke(false, 0)
        }
    }

    /**
     * 13. 로그인된 회원의 DB 포인트 및 내역 실시간 수신 리스너 (홈페이지 관리자 페이지에서 포인트 지급/차감 시 스마트폰 앱 즉시 반영)
     */
    fun listenToCurrentUserPointsRealtime(
        context: Context,
        userId: String,
        onPointsChanged: (Int) -> Unit
    ): ListenerRegistration? {
        if (userId.isBlank()) return null
        return try {
            val sanitized = userId.trim().replace("[^a-zA-Z0-9_]".toRegex(), "_")
            firestore.collection(USERS_COLLECTION)
                .document(userId.trim())
                .addSnapshotListener { snapshot, error ->
                    try {
                        if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                        val prefs = context.getSharedPreferences("BaljagukAppDiskPrefs", Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                        var updated = false

                        val cloudLogs = snapshot.getString("point_logs_json")
                        val cloudLogsTs = snapshot.getLong("point_logs_json_ts") ?: 0L
                        val localLogsKey = if (sanitized.isNotBlank()) "point_logs_json_$sanitized" else "point_logs_json"
                        val localLogsTs = prefs.getLong("${localLogsKey}_ts", 0L)
                        if (cloudLogs != null && cloudLogsTs >= localLogsTs) {
                            editor.putString(localLogsKey, cloudLogs)
                            editor.putString("point_logs_json", cloudLogs)
                            editor.putLong("${localLogsKey}_ts", cloudLogsTs)
                            updated = true
                        }

                        val cloudPoints = snapshot.getLong("points")?.toInt()
                        if (cloudPoints != null) {
                            val logsJson = prefs.getString(localLogsKey, null)
                            val hasSpent = logsJson?.contains("\"amount\":-") == true || logsJson?.contains("\"isEarned\":false") == true
                            val finalPoints = if (cloudPoints <= 0 && !hasSpent) 1000 else cloudPoints
                            editor.putInt("user_points", finalPoints)
                            if (sanitized.isNotBlank()) {
                                editor.putInt("user_points_$sanitized", finalPoints)
                            }
                            updated = true
                            editor.apply()
                            onPointsChanged(finalPoints)
                        } else if (updated) {
                            editor.apply()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "listenToCurrentUserPointsRealtime snapshot error: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenToCurrentUserPointsRealtime error: ${e.message}")
            null
        }
    }
}
