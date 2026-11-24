import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// API keys live in local.properties, which is NOT committed to GitHub.
// Add these lines to local.properties:
//   TS_CLIENT_ID=your-traffic-scotland-client-id
//   TS_CLIENT_KEY=your-traffic-scotland-client-key
//   MAPS_API_KEY=your-google-maps-key
val localProperties = Properties().apply {
    val text = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
    if (text != null) load(StringReader(text))
}
fun secret(name: String): String = localProperties.getProperty(name)?.trim().takeUnless { it.isNullOrEmpty() } ?: "KEY_HERE"

android {
    namespace = "org.me.gcu.macdonald_cian_s2426804"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.me.gcu.macdonald_cian_s2426804"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["MAPS_API_KEY"] = secret("MAPS_API_KEY")
        manifestPlaceholders["TS_CLIENT_ID"] = secret("TS_CLIENT_ID")
        manifestPlaceholders["TS_CLIENT_KEY"] = secret("TS_CLIENT_KEY")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.fragment)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
