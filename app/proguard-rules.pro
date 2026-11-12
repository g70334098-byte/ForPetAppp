# 발자국동행 R8 / ProGuard 상용화 릴리스 및 역공학 방지 규칙

# 1. 데이터 모델 및 Firestore/JSON 직렬화 필수 클래스 보존 (Google Play 난독화 25% 이상 기준 충족)
-dontwarn **
-keep class com.example.forpetapp.data.model.** { *; }
-keep class com.example.forpetapp.PointLog { *; }
-keep class com.example.forpetapp.ReviewItem { *; }
-keep class com.example.forpetapp.PetDDayItem { *; }
-keep class com.example.forpetapp.util.SmsGatewayManager$SmsResult { *; }

# Firestore / Gson / 리플렉션 직렬화 어노테이션 멤버 보존
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
}

# 2. 직렬화 어노테이션, 제네릭 시그니처(Retrofit Call<T> 필수) 및 크래시 라인 넘버 보존
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-renamesourcefileattribute SourceFile

# 3. Kakao SDK + Retrofit2 + Gson + OkHttp 리플렉션/제네릭 완벽 보존 (UserApi.checkAccessToken 크래시 원천 차단)
-keep class com.kakao.sdk.** { *; }
-keep interface com.kakao.sdk.** { *; }
-keep enum com.kakao.sdk.** { *; }
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
