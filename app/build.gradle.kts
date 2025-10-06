plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mangocam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mangocam"
        minSdk = 30
        targetSdk = 34
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Exclude the com.intellij:annotations (12.0) dependency
    implementation("com.google.firebase:firebase-database:21.0.0") {
        exclude(group = "com.intellij", module = "annotations")
        exclude(group = "androidx.annotation", module = "annotation")

    }

    implementation("com.google.firebase:firebase-auth-ktx:21.0.0") {
        exclude(group = "com.intellij", module = "annotations")
        exclude(group = "androidx.annotation", module = "annotation")

    }

    // Retrofit and other dependencies
    implementation("com.squareup.retrofit2:retrofit:2.9.0") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Coroutine dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    // Room dependencies
    implementation("androidx.room:room-runtime:2.4.1")

    // Lifecycle dependencies
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.1")

    implementation("androidx.core:core:1.9.0") {
        exclude (group= "com.android.support", module="support-compat")
    }

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation(libs.com.google.android.material.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.json)
    implementation(libs.material.calendarview)
    implementation(libs.appcompat)
    implementation(libs.androidx.core.core.ktx)
    implementation(libs.fragment.ktx)
    implementation("com.github.bumptech.glide:glide:4.16.0") {
        exclude(group = "com.github.bumptech.glide", module = "compiler")
        exclude(group = "com.github.bumptech.glide", module = "annotations")
    }

    implementation("com.google.android.material:material:1.9.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}

