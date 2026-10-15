import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android library with shared SDK/Java settings, lint rules, quality checks and the JVM test stack. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("zeroos.quality")

        extensions.configure<LibraryExtension> {
            compileSdk = ProjectConfig.COMPILE_SDK
            defaultConfig {
                minSdk = ProjectConfig.MIN_SDK
            }
            compileOptions {
                sourceCompatibility = ProjectConfig.JAVA_VERSION
                targetCompatibility = ProjectConfig.JAVA_VERSION
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            lint {
                configureZeroLint()
            }
        }
        addUnitTestDependencies()
    }
}
