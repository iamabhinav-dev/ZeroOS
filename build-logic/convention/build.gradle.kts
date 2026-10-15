import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "zeroos.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "zeroos.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "zeroos.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("androidCompose") {
            id = "zeroos.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
