import java.util.Properties

plugins {
    alias(libs.plugins.zeroos.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release key from the git-ignored keystore.properties (Step 0.7, D-010). Debug and release share it so
// either APK installs over the other. Without the file, builds use the default debug key and must not
// be installed on the phone.
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply { if (keystoreFile.exists()) keystoreFile.inputStream().use(::load) }
if (!keystoreFile.exists()) {
    logger.warn("keystore.properties not found: APKs are signed with the default debug key. Do NOT install them on the phone.")
}

android {
    namespace = "com.zeroos.launcher"

    defaultConfig {
        applicationId = "com.zeroos.launcher"
        versionCode = 30
        versionName = "0.0.30"
    }

    signingConfigs {
        if (keystoreFile.exists()) {
            create("zeroos") {
                storeFile = file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        signingConfigs.findByName("zeroos")?.let { zeroosKey ->
            getByName("debug").signingConfig = zeroosKey
            getByName("release").signingConfig = zeroosKey
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/**
 * F14.8: the release's README notes, LICENSE and third-party notices inside the APK (assets/docs/), so the app shows
 * exactly the documents of the installed release, offline. Only the README part between the in-app markers is used.
 */
abstract class ReleaseDocsTask : DefaultTask() {
    @get:InputFile
    abstract val readme: RegularFileProperty

    @get:InputFile
    abstract val license: RegularFileProperty

    @get:InputFile
    abstract val notices: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val docs = outputDir.get().asFile.resolve("docs").apply {
            deleteRecursively()
            mkdirs()
        }
        val text = readme.get().asFile.readText()
        val start = "<!-- in-app:start -->"
        val end = "<!-- in-app:end -->"
        require(start in text && end in text) { "README.md needs $start and $end around the part shown in the app" }
        docs.resolve("README.md").writeText(text.substringAfter(start).substringBefore(end).trim() + "\n")
        license.get().asFile.copyTo(docs.resolve("LICENSE.txt"))
        notices.get().asFile.copyTo(docs.resolve("THIRD_PARTY_NOTICES.md"))
    }
}

val releaseDocs = tasks.register<ReleaseDocsTask>("releaseDocs") {
    readme.set(rootProject.file("README.md"))
    license.set(rootProject.file("LICENSE"))
    notices.set(rootProject.file("THIRD_PARTY_NOTICES.md"))
    outputDir.set(layout.buildDirectory.dir("generated/releaseDocs"))
}

androidComponents {
    onVariants { variant -> variant.sources.assets?.addGeneratedSourceDirectory(releaseDocs, ReleaseDocsTask::outputDir) }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":core:resources"))
    implementation(project(":core:logging"))
    implementation(project(":core:data"))
    implementation(project(":feature:home"))
    implementation(project(":feature:applist"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:wellbeing"))
    implementation(project(":feature:focus"))
    implementation(project(":feature:insights"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Debug-only memory-leak detection (Step 0.6).
    debugImplementation(libs.leakcanary)

    testImplementation(project(":core:testing"))
}
