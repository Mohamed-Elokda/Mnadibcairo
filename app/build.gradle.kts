plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp") // Add this line
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            // حل مشكلة LICENSES.md وغيرها من الملفات المتكررة
            excludes += "/META-INF/MPL-2.0.txt"
            excludes += "/META-INF/LGPL-2.1.txt"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSES.md" // الحل للخطأ الحالي
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/DEPENDENCIES"

            // إضافة هذه الأسطر لأن المكتبات التي تستخدمها تحتوي على هذه الملفات أيضاً
            excludes += "/META-INF/AL2.0"
            excludes += "/META-INF/LGPL2.1"

            // في حالة وجود أي تعارض آخر، يأخذ أول نسخة يجدها ويستمر في البناء
            pickFirsts += "META-INF/INDEX.LIST"
            pickFirsts += "META-INF/io.netty.versions.properties"
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.gridlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Google Location
    implementation(libs.play.services.location)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Image Loading
    implementation("io.coil-kt:coil:2.5.0")

    // =========================
    // Supabase
    // =========================
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.0.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.0.1")
    implementation(libs.protolite.well.known.types)

    // Ktor required for Supabase
    val ktor_version = "3.0.0"

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-plugins:${ktor_version}")
    // =========================
    // Room Database
    // =========================
    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // =========================
    // Coroutines
    // =========================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // await() for Google Tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // =========================
    // Gemini AI
    // =========================
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // =========================
    // Testing
    // =========================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Apache POI (Excel)
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.poi:poi-ooxml-lite:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("androidx.print:print:1.0.0")
}
