import com.github.jk1.license.render.JsonReportRenderer
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license.report)
}

// Release signing comes from the environment only (docs/01 §Build & Release). Without it the release
// build is debug-signed so local `assembleRelease` still works; the build log says so.
val keystoreBase64: String? = System.getenv("TL_KEYSTORE_BASE64")?.takeIf { it.isNotBlank() }
val releaseKeystore: File? = keystoreBase64?.let { encoded ->
    layout.buildDirectory.file("signing/release.jks").get().asFile.apply {
        parentFile.mkdirs()
        // MIME decoder: tolerates line breaks from `base64` implementations that wrap at 76 characters.
        writeBytes(Base64.getMimeDecoder().decode(encoded))
    }
}

/** Secrets pasted interactively often carry a trailing newline; only that is stripped, never inner spaces. */
fun secret(name: String): String? = System.getenv(name)?.trimEnd('\n', '\r')?.takeIf { it.isNotEmpty() }
if (releaseKeystore == null) {
    logger.warn("TL_KEYSTORE_BASE64 not set: release build will be unsigned/debug-signed (local test only).")
}

android {
    namespace = "com.wallee.terminallinker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wallee.terminallinker"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("TL_VERSION_CODE")?.toIntOrNull()) ?: 1
        versionName = System.getenv("TL_VERSION_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        localeFilters += listOf("de", "en")
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = secret("TL_KEYSTORE_PASSWORD")
                keyAlias = secret("TL_KEY_ALIAS")
                keyPassword = secret("TL_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            // Optional local mock for UI checks: TL_BASE_URL=http://10.0.2.2:8787 ./gradlew assembleDebug
            buildConfigField(
                "String",
                "WALLEE_BASE_URL",
                "\"" + (System.getenv("TL_BASE_URL")?.takeIf { it.isNotBlank() } ?: "https://app-wallee.com") + "\"",
            )
        }
        release {
            buildConfigField("String", "WALLEE_BASE_URL", "\"https://app-wallee.com\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig =
                if (releaseKeystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Full list of runtime dependencies with their licenses, rendered as assets/oss_licenses.json (docs/05 Phase 6.3).
val licenseReportDir = layout.buildDirectory.dir("reports/ossLicenses")
licenseReport {
    configurations = arrayOf("releaseRuntimeClasspath")
    outputDir = licenseReportDir.get().asFile.path
    renderers = arrayOf(JsonReportRenderer("oss_licenses.json", false))
}

/** Copies the report into a directory AGP can consume as a generated assets source. */
abstract class OssLicensesAssetTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val report: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().asFile
        target.deleteRecursively()
        target.mkdirs()
        report.get().asFile.copyTo(target.resolve("oss_licenses.json"))
    }
}

val ossLicensesAsset = tasks.register<OssLicensesAssetTask>("ossLicensesAsset") {
    dependsOn("generateLicenseReport")
    report.set(licenseReportDir.map { it.file("oss_licenses.json") })
}

// Registering through the Variant API wires the task into asset merging and lint for every variant, also on
// a clean build (a plain srcDir on the source set silently skipped the generation there).
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(ossLicensesAsset, OssLicensesAssetTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.mlkit.barcode.scanning)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
