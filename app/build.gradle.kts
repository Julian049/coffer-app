plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.dev.julian09.financeapp"
    // Compiling against 37 lets current AndroidX and OkHttp 5 build; behavior still follows targetSdk 36
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.dev.julian09.financeapp"
        minSdk = 31
        targetSdk = 36
        // versionCode = MAJOR*10000 + MINOR*100 + PATCH (manage-config §3.1.2)
        versionCode = 700
        versionName = "0.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only in release.yml: the keystore comes from GitHub secrets, never from the repo
        System.getenv("RELEASE_KEYSTORE_PATH")?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    // Suffixes let staging and release live side by side on the PO's phone (manage-config §5.3)
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
            resValue("string", "app_name", "Coffer debug")
            // JaCoCo built into AGP: ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "API_BASE_URL", "\"https://coffer-server.onrender.com/\"")
            resValue("string", "app_name", "Coffer")
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("String", "API_BASE_URL", "\"https://coffer-server-staging.onrender.com/\"")
            resValue("string", "app_name", "Coffer staging")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    // Room's MigrationTestHelper reads the exported schemas as assets; Robolectric sees the debug variant's,
    // so they ship only in debug builds, never in release (FinanceDatabaseMigrationTest)
    sourceSets["debug"].assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            // Robolectric renders Compose with the app's resources (Roborazzi screenshots)
            isIncludeAndroidResources = true
        }
    }
}

//noinspection UseTomlInstead
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // Encrypted Room database (ADR-0008)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore)
    // Background sync (ADR-0006): WorkManager with Hilt-injected workers
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation("androidx.hilt:hilt-navigation-compose:1.4.0")
    val navVersion = "2.10.2"
    implementation("androidx.navigation:navigation-compose:$navVersion")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.junit)
    testImplementation(libs.konsist)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Reference screenshots live in the repo; CI fails when a component changes without updating them
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

// Room schema JSON per version, versioned in Git and used by migration tests (data-model §5.2)
room {
    schemaDirectory("$projectDir/schemas")
}
