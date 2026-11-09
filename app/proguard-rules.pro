# 발자국동행 R8 / ProGuard 상용화 릴리스 및 역공학 방지 규칙

# 1. 데이터 모델 및 직렬화 필드 보존
-keep class com.example.forpetapp.data.model.** { *; }
-keepclassmembers class com.example.forpetapp.data.model.** { *; }

# 2. 직렬화 어노테이션 보존
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 3. 보안 매니저 난독화 방지 (메모리 탐지 방어 유지)
-keep class com.example.forpetapp.security.** { *; }

# 4. Kakao SDK 및 Firebase 리플렉션 안전 보존
-keep class com.kakao.sdk.**.model.* { <fields>; }
-keep class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }

# 5. 소스 파일명 및 라인 넘버는 크래시 분석용 보존
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
