import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** detekt (with ZeroOS custom rules), ktlint, and Kover coverage for a module. Wired into `check`. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            basePath = rootProject.projectDir.absolutePath
        }

        extensions.configure<KtlintExtension> {
            version.set(libs.findVersion("ktlint").get().requiredVersion)
            android.set(true)
        }

        extensions.configure<KoverProjectExtension> {
            currentProject {
                createVariant(ProjectConfig.KOVER_VARIANT) {
                    add("debug")
                }
            }
        }

        dependencies {
            add("detektPlugins", project(":detekt-rules"))
        }
    }
}
