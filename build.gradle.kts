import java.io.File

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// OneDrive locks files under app/build and makes :app:hiltJavaCompileDebug fail.
allprojects {
    val path = rootDir.absolutePath
    if (!path.contains("OneDrive", ignoreCase = true)) return@allprojects
    val relocated = File(System.getProperty("user.home"), ".messageme-build/${rootProject.name}/${name}")
    layout.buildDirectory.set(relocated)
    logger.lifecycle("OneDrive detected. Build output → ${relocated.absolutePath}")
}
