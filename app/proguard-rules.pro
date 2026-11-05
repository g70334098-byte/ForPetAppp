# 발자국동행 R8 / ProGuard 상용화 릴리스 규칙

# 1. 데이터 모델 및 직렬화 필드 보존
-keep class com.example.forpetapp.data.model.** { *; }
-keepclassmembers class com.example.forpetapp.data.model.** { *; }

# 2. 직렬화 어노테이션 보존
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
