import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Shared Android settings for every module (REQUIREMENTS.md, Platform). */
object ProjectConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_17

    /** Kover variant that every module contributes to and the root project aggregates. */
    const val KOVER_VARIANT = "aggregate"
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()
