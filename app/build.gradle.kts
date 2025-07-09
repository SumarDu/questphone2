import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization") version "2.0.20"

    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "neth.iecal.questphone"
    compileSdk = 36

    defaultConfig {
        applicationId = "neth.iecal.questphone"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase keys
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties.getProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${localProperties.getProperty("supabase.key")}\"")

    }

    flavorDimensions += "distribution"

    productFlavors {
        create("fdroid") {
            dimension = "distribution"
            versionNameSuffix = "-fdroid"
            buildConfigField("Boolean", "IS_FDROID", "true")
        }
        create("play") {
            dimension = "distribution"
            versionNameSuffix = "-play"
            buildConfigField("Boolean", "IS_FDROID", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation (libs.kotlinx.serialization.json)

    implementation(libs.androidx.navigation.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.coil.compose)
    implementation(kotlin("reflect"))

    implementation(libs.androidx.connect.client)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Supabase
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.3.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.3.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.3.0")

    // Gemini API
    implementation("com.google.ai.client.generativeai:generativeai:0.8.0")





    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.datetime)

    implementation (libs.androidx.work.runtime.ktx)

    implementation(libs.compose.markdown)


    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation (libs.guava)

    implementation (libs.coil.compose)

    implementation("com.github.skydoves:colorpicker-compose:1.0.0")

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    ksp(libs.androidx.room.compiler)
 }
