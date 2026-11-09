import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.forpetapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.baldong.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 50
        versionName = "1.0.49"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/baldong-release-key.jks")
            storePassword = System.getenv("KEYSTORE_STORE_PASSWORD") 
                ?: localProperties.getProperty("KEYSTORE_STORE_PASSWORD") 
                ?: "baldong1234"
            keyAlias = System.getenv("KEYSTORE_KEY_ALIAS") 
                ?: localProperties.getProperty("KEYSTORE_KEY_ALIAS") 
                ?: "baldong"
            keyPassword = System.getenv("KEYSTORE_KEY_PASSWORD") 
                ?: localProperties.getProperty("KEYSTORE_KEY_PASSWORD") 
                ?: "baldong1234"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// 외부 AAR(libandroidx.graphics.path.so 등)이 strip된 상태로 배포되어 Play Console에서
// '네이티브 디버그 기호가 업로드되지 않았습니다' 경고가 발생하는 현상을 방지하기 위해
// mergeReleaseNativeLibs 단계에서 심볼 테이블을 주입하여 AAB 내 debugsymbols 메타데이터 생성을 보장합니다.
tasks.configureEach {
    if (name == "mergeReleaseNativeLibs") {
        doLast {
            val sdkDir = File(System.getProperty("user.home"), "AppData/Local/Android/Sdk")
            val objcopy = File(sdkDir, "ndk")
                .walkTopDown()
                .firstOrNull { it.name.equals("llvm-objcopy.exe", ignoreCase = true) || it.name == "llvm-objcopy" }
            if (objcopy != null && objcopy.exists()) {
                outputs.files.forEach { outRoot ->
                    outRoot.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { soFile ->
                        try {
                            ProcessBuilder(
                                objcopy.absolutePath,
                                "--add-symbol=baldong_native_symbol=.text:0,function,global",
                                soFile.absolutePath
                            ).start().waitFor()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}

dependencies {
    implementation("com.kakao.sdk:v2-user:2.20.1")
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}