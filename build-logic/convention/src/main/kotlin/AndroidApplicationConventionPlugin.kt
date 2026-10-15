import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android application with shared SDK/Java settings, Compose, lint rules, quality checks and tests. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("zeroos.quality")

        extensions.configure<ApplicationExtension> {
            compileSdk = ProjectConfig.COMPILE_SDK
            defaultConfig {
                minSdk = ProjectConfig.MIN_SDK
                targetSdk = ProjectConfig.COMPILE_SDK
            }
            compileOptions {
                sourceCompatibility = ProjectConfig.JAVA_VERSION
                targetCompatibility = ProjectConfig.JAVA_VERSION
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
            }
            sourceSets.getByName("test").resources.directories.add(rootProject.file("config/robolectric").path)
            lint {
                configureZeroLint()
            }
        }
        addUnitTestDependencies()
        configureCompose()
    }
}
