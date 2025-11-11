plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "br.ufpr.vibetrack.vibetrackwearos"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.ufpr.vibetrack.vibetrackwearos"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    // Para a UI do Wear OS
    implementation("androidx.wear.compose:compose-material:1.3.0")
    implementation("androidx.wear.compose:compose-foundation:1.3.0")
    // Para comunicação com o celular
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
    // Para serializar os dados em JSON
    implementation("com.google.code.gson:gson:2.10.1")
    // Para coletar dados de saúde (opcional, mas recomendado)
    implementation("androidx.health:health-services-client:1.1.0-alpha03")

    // --- CORREÇÃO APLICADA ---
    // A linha duplicada "2.8.0" foi removida daqui.

    // Para o AndroidViewModel e a função getApplication()
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // Para a delegação "by viewModels()" na MainActivity
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.1.1")

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}