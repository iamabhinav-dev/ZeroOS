import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Android Lint: warnings are errors, and hardcoded UI text fails the build (NFR-L7). */
internal fun Lint.configureZeroLint() {
    warningsAsErrors = true
    abortOnError = true
    checkReleaseBuilds = false
    error += "HardcodedText"
    // Version-update checks need the network and aren't code problems.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi")
}

/** JVM test stack shared by all Android modules (Testing Policy). */
internal fun Project.addUnitTestDependencies() {
    dependencies {
        add("testImplementation", libs.lib("junit"))
        add("testImplementation", libs.lib("robolectric"))
        add("testImplementation", libs.lib("mockk"))
        add("testImplementation", libs.lib("turbine"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("androidx-test-core"))
        add("testImplementation", libs.lib("androidx-test-ext-junit"))
    }
}
