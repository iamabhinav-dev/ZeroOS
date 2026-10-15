pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ZeroOS"

include(":app")
include(":core:model")
include(":core:ui")
include(":core:resources")
include(":core:logging")
include(":core:data")
include(":core:testing")
include(":feature:home")
include(":feature:applist")
