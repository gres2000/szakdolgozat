plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")


}

configurations.all {
    resolutionStrategy {
        force(
            "io.grpc:grpc-okhttp:1.57.2",
            "io.grpc:grpc-protobuf-lite:1.57.2",
            "io.grpc:grpc-stub:1.57.2",
            "io.grpc:grpc-core:1.57.2",
            "io.grpc:grpc-android:1.57.2",
            "io.grpc:grpc-api:1.57.2",
            "io.grpc:grpc-context:1.57.2"
        )
    }
}


android {
    namespace = "com.taskraze.myapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.taskraze.myapplication"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Uncomment only if you run into multidex issues
        multiDexEnabled = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
    buildFeatures { viewBinding = true }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation("com.microsoft.identity.client:msal:8.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.firebaseui:firebase-ui-database:8.0.2")
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2")
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation("com.firebaseui:firebase-ui-storage:8.0.2")

    // AndroidX & UI
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("io.ktor:ktor-client-core:2.3.11")
    implementation("io.ktor:ktor-client-logging:2.3.11")
    implementation("io.ktor:ktor-client-cio:2.3.11")
    implementation("com.kizitonwose.calendar:view:2.0.4")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.api-client:google-api-client-android:2.8.1")
    implementation("com.google.api-client:google-api-client-gson:2.8.1")
}
