import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// 🛠️ 자동 패치: MainActivity.kt 내 context 파라미터 안전 보정
val mainActivityFile = file("src/main/java/com/example/forpetapp/MainActivity.kt")
if (mainActivityFile.exists()) {
    val rawText = mainActivityFile.readText()
    if (rawText.contains("this@MainActivity, \"g70334098\"")) {
        mainActivityFile.writeText(rawText.replace("this@MainActivity, \"g70334098\"", "context, \"g70334098\""))
    }
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
        versionCode = 87
        versionName = "1.0.87"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    androidResources {
        localeFilters += listOf("ko")
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
                debugSymbolLevel = "SYMBOL_TABLE"
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
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.proto"
            )
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
