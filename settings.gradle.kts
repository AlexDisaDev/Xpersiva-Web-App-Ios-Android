plugins {
    // Si Gradle no encuentra un JDK 17 instalado, lo descarga él solo en vez
    // de fallar. Sin esto, un PC sin el JDK exacto da "Cannot find a Java
    // installation... Toolchain download repositories have not been configured".
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

pluginManagement {
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
rootProject.name = "Xpersiva"
include(":app")
