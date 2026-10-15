import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/** Android Lint: warnings are errors, and hardcoded UI text fails the build (NFR-L7). */
internal fun Lint.configureZeroLint() {
    warningsAsErrors = true
    abortOnError = true
    checkReleaseBuilds = false
    error += "HardcodedText"
    // Version-update checks need the network and aren't code problems.
    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable", "OldTargetApi")
}

/** JVM test stack shared by all Android modules (Testing Policy), with memory caps for an 8 GB machine (D-017). */
internal fun Project.addUnitTestDependencies() {
    tasks.withType<Test>().configureEach {
        maxHeapSize = "1g"
        maxParallelForks = 1
        // Robolectric on Java 21 reads FileDescriptor internals.
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        )
    }
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
