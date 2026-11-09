# 발자국동행 R8 / ProGuard 상용화 릴리스 및 역공학 방지 규칙

# 1. 앱 전체 패키지 및 데이터 모델 보존
-dontwarn **
-keep class com.example.forpetapp.** { *; }
-keep interface com.example.forpetapp.** { *; }
-keep enum com.example.forpetapp.** { *; }

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
