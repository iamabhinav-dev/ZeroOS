import org.gradle.api.JavaVersion

/** Shared Android settings for every module (REQUIREMENTS.md, Platform). */
object ProjectConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_17
}
