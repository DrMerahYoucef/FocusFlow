plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  id("com.google.gms.google-services")
}

// Ensure app/google-services.json exists to prevent build failures when cloning from GitHub
val googleServicesJson = file("google-services.json")
if (!googleServicesJson.exists()) {
    googleServicesJson.writeText(
        """
        {
          "project_info": {
            "project_number": "15963877054",
            "project_id": "focuse-island",
            "storage_bucket": "focuse-island.firebasestorage.app"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:15963877054:android:96d280ae44802a00bbf8ef",
                "android_client_info": {
                  "package_name": "com.focusisland"
                }
              },
              "oauth_client": [],
              "api_key": [
                {
                  "current_key": "AIzaSyAFKsKjqCROungwqAxXCHgqeUQEu-kg3go"
                }
              ],
              "services": {
                "appinvite_service": {
                  "other_platform_oauth_client": []
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent()
    )
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.focusisland"
    minSdk = 26
    targetSdk = 35
    versionCode = 7
    versionName = "1.0.7"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  @Suppress("DEPRECATION")
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "androidx.camera") {
      useVersion("1.3.4")
    }
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-auth")
  implementation("com.google.firebase:firebase-firestore")
  implementation("com.google.firebase:firebase-config")
  implementation("com.google.firebase:firebase-storage")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation("com.google.mlkit:text-recognition:16.0.1")
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation("androidx.work:work-runtime-ktx:2.9.1")
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation("androidx.fragment:fragment-ktx:1.8.9")

  // Media3 (ExoPlayer + MediaSession)
  implementation("androidx.media3:media3-exoplayer:1.3.1")
  implementation("androidx.media3:media3-session:1.3.1")
  implementation("androidx.media3:media3-ui:1.3.1")
  implementation("androidx.media3:media3-datasource-okhttp:1.3.1")
  implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register<Copy>("copyApkToYoucef") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("app-debug.apk")
    into(rootProject.file("youcef"))
}

tasks.register<Copy>("copyApkToBuildOutputs") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("app-debug.apk")
    into(rootProject.file("build-outputs"))
}

