plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val buildCommit = providers.environmentVariable("GITHUB_SHA")
    .orElse(
        providers.exec {
            commandLine("git", "rev-parse", "HEAD")
        }.standardOutput.asText.map { it.trim() }
    )
    .getOrElse("unknown")

android {
    namespace = "com.example.despierta"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.bluelabstudios.despierta"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "2.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GIT_COMMIT", "\"${buildCommit.take(12)}\"")
        // Publicidad experimental (rama experimental/admob). Solo identificadores de PRUEBA
        // oficiales de Google. Los reales se pasarán como propiedades de Gradle cuando existan
        // (admobAppId / admobBannerId); no hay ningún identificador real en el código.
        val admobAppId = (project.findProperty("admobAppId") as String?) ?: "ca-app-pub-3940256099942544~3347511713"
        val admobBannerId = (project.findProperty("admobBannerId") as String?) ?: "ca-app-pub-3940256099942544/6300978111"
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("boolean", "ADS_ENABLED", "true")
        buildConfigField("boolean", "ADS_TEST_IDS", (project.findProperty("admobBannerId") == null).toString())
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
    }
    buildTypes {
        // Versión optimizada para instalar y probar: R8 activado, sin depuración y firmada con la
        // clave de depuración, así se instala encima del APK de depuración conservando los datos.
        // No es la versión de Google Play (esa se firma con la clave de carga).
        create("fluida") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    // Publicidad (experimental): SDK oficial de Google Mobile Ads y consentimiento (UMP).
    implementation("com.google.android.gms:play-services-ads:24.0.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
