# 발자국동행 R8 / ProGuard 상용화 릴리스 및 역공학 방지 규칙

# 1. 앱 전체 패키지 및 데이터 모델/시리얼라이즈 보존 (런타임 리플렉션 크래시 방지 + Play Console 난독화 해제 파일 생성)
-dontwarn **
-keep class com.example.forpetapp.** { *; }
-keep interface com.example.forpetapp.** { *; }
-keep enum com.example.forpetapp.** { *; }

# 2. 직렬화 어노테이션 및 크래시 라인 넘버 보존
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# 3. Kakao SDK 및 Firebase 서비스 리플렉션 안전 보존
-keep class com.kakao.sdk.**.model.* { <fields>; <methods>; }
-keep class com.kakao.sdk.auth.** { *; }
-keep class com.kakao.sdk.user.** { *; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
