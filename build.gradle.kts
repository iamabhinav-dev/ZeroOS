plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // Puts our Kotlin version on the classpath; AGP 9 provides built-in Kotlin support.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
}

// Aggregated coverage across all modules (Testing Policy #10).
dependencies {
    listOf(
        ":app",
        ":core:model",
        ":core:ui",
        ":core:resources",
        ":core:logging",
        ":core:data",
        ":core:testing",
        ":feature:home",
        ":feature:applist",
        ":feature:settings",
        ":feature:onboarding",
    ).forEach { kover(project(it)) }
}

kover {
    currentProject {
        createVariant("aggregate") {}
    }
    reports {
        filters {
            excludes {
                // UI-only composables and generated code are excluded from the logic coverage rule.
                annotatedBy("androidx.compose.runtime.Composable", "dagger.Module", "dagger.hilt.android.HiltAndroidApp")
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*ComposableSingletons*",
                    "*Hilt_*",
                    "*_Factory",
                    "*_Factory$*",
                    "*_HiltModules*",
                    "*_MembersInjector",
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    // Test helpers, not app logic.
                    "com.zeroos.core.testing.*",
                    "com.zeroos.launcher.MainActivity",
                    "com.zeroos.launcher.ZeroApplication",
                )
            }
        }
        verify {
            rule("Logic coverage ≥ 80%") {
                minBound(80)
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerifyAggregate")
}

// Website (site/): fails on hardcoded text and missing strings or files, like the app's HardcodedUiText rule.
val checkSite by tasks.registering(Exec::class) {
    description = "Tests scripts/build-site.py and checks site/ for hardcoded text."
    group = "verification"
    commandLine("python3", "-B", "scripts/test_build_site.py")
}

tasks.named("check") {
    dependsOn(checkSite)
}
