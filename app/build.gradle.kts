plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}

android {
    namespace = "app.src"

    // Usa 35 o 36 según lo que tengas instalado en SDK Manager.
    // Si no tienes 36, pon 35 en ambos.
    compileSdk = 36

    defaultConfig {
        applicationId = "app.src"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug { /* puedes activar logs, etc. */ }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        // compose = false (por defecto)
    }

    compileOptions {
        // AGP 8.x requiere JDK 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Deshabilitar lint temporalmente para evitar problemas de bloqueo de archivos en Windows
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Retrofit para API REST
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Room Database para cache offline
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore para preferencias type-safe (reemplazo de SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.datastore:datastore:1.1.1")

    // WorkManager para sincronizacion en background
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // QR Code generation
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Coil para carga de imágenes (mejor que Glide para Kotlin/Coroutines)
    implementation("io.coil-kt:coil:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Glide (manteniendo compatibilidad con código existente)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // ✅ REQUERIMIENTO 1: Guava Cache - Librería profesional de cache en memoria (compatible API 24+)
    implementation("com.google.guava:guava:31.1-android")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
