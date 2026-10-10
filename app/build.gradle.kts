import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android { namespace = "ir.pinbom.app"; compileSdk = 37
    defaultConfig { applicationId = "ir.pinbom.app"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0.0"
        // Only the two shipped locales ride along: smaller APK, and a device set to
        // any other language falls back to values/ instead of a library's locale.
        resourceConfigurations += listOf("en", "fa")
    }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { compose = true; buildConfig = true }
    /* Java and Kotlin must agree on a JVM target or kapt refuses to run. */
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes.getByName("debug").buildConfigField("String", "DEFAULT_BASE_URL", "\"http://185.26.33.188:8080/\"")
    buildTypes.getByName("release").buildConfigField("String", "DEFAULT_BASE_URL", "\"http://185.26.33.188:8080/\"")
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
    /* DIAGNOSTIC ONLY — lets a JVM (Robolectric) launch the real MainActivity. */
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

kapt { correctErrorTypes = true }

// نسخه‌های Compose را BOM هماهنگ می‌کند.
dependencies {
    implementation(platform(libs.androidx.compose.bom)); androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose); implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.core); implementation(libs.androidx.core.splashscreen); implementation(libs.androidx.activity); implementation(libs.androidx.lifecycle.runtime); implementation(libs.androidx.lifecycle.viewmodel)
    /* Compiles the baseline profile in src/main/baseline-prof.txt and installs it
     * after updates; without it the profile only helps fresh installs. */
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.navigation.compose); implementation(libs.androidx.paging.runtime); implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.datastore); implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.ktx); kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work); implementation(libs.androidx.hilt.work); implementation(libs.androidx.hilt.navigation)
    implementation(libs.hilt.android); kapt(libs.hilt.compiler)
    /* @HiltWorker: without this processor WorkManager falls back to its default
     * factory and cannot construct UploadWorker (NoSuchMethodException). */
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.retrofit); implementation(libs.retrofit.serialization); implementation(libs.okhttp); implementation(libs.okhttp.logging); implementation(libs.serialization.json)
    implementation(libs.coil.compose); implementation(libs.coil.network); implementation(libs.coroutines)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
