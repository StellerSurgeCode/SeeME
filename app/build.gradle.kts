plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zjf.seeme"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("seeme-release.jks")
            storePassword = System.getenv("SEEME_STORE_PASSWORD") ?: "YOUR_STORE_PASSWORD"
            keyAlias = "seeme"
            keyPassword = System.getenv("SEEME_KEY_PASSWORD") ?: "YOUR_KEY_PASSWORD"
        }
    }

    defaultConfig {
        applicationId = "com.zjf.seeme"
        minSdk = 26
        targetSdk = 35
        versionCode = 20260221
        versionName = "2026.02.21"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Network
    implementation(libs.okhttp)
    implementation(libs.gson)

    // WorkManager
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
