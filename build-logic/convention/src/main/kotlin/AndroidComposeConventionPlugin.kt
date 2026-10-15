import com.android.build.api.dsl.LibraryExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Enables Jetpack Compose in an Android library module. Apply after `zeroos.android.library`. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure<LibraryExtension> {
            buildFeatures {
                compose = true
            }
        }
        configureCompose()
    }
}

/** Compose compiler, BOM libraries, Compose UI tests and Roborazzi screenshot tests. */
internal fun Project.configureCompose() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    pluginManager.apply("io.github.takahirom.roborazzi")

    extensions.configure<RoborazziExtension> {
        // Reference screenshots are committed next to the tests (Testing Policy #3).
        outputDir.set(file("src/test/screenshots"))
    }

    dependencies {
        val bom = platform(libs.lib("androidx-compose-bom"))
        add("implementation", bom)
        add("implementation", libs.lib("androidx-compose-ui"))
        add("implementation", libs.lib("androidx-compose-material3"))
        add("testImplementation", bom)
        add("testImplementation", libs.lib("androidx-compose-ui-test-junit4"))
        add("testImplementation", libs.lib("roborazzi"))
        add("testImplementation", libs.lib("roborazzi-compose"))
        add("debugImplementation", bom)
        add("debugImplementation", libs.lib("androidx-compose-ui-test-manifest"))
    }
}
