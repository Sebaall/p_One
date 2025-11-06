plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.p_one"
    compileSdk = 36  // ← esto es Kotlin DSL, no va con bloques

    defaultConfig {
        applicationId = "com.example.p_one"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX / UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase - usa tus aliases (KTX) SIN duplicados
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.auth.ktx)

    // BoM para poder usar analytics sin versión explícita
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
