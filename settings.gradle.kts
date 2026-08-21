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

rootProject.name = "MessageMe"
include(":app")

// Apply before AGP configures outputs. OneDrive cannot snapshot files under app/build.
gradle.beforeProject {
    val path = rootDir.absolutePath
    if (!path.contains("OneDrive", ignoreCase = true)) return@beforeProject
    val relocated = file("${System.getProperty("user.home")}/.messageme-build/${rootProject.name}/${name}")
    layout.buildDirectory.set(relocated)
}
