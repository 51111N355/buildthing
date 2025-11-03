package net.im51111n355.buildthing

import net.im51111n355.buildthing.util.sha256
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import java.io.File
import java.io.InputStream

class BuildThingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        injectStd(project)
    }

    // Добавляет standard.jar в зависимости, обрабатывает то что может быть несколько версий плагина с разными standard.jar (привязка к sha256)
    private fun injectStd(project: Project) {
        val classLoader = javaClass.classLoader

        val bytes = classLoader.getResourceAsStream("standard.jar")!!
            .use(InputStream::readBytes)
        val hash = bytes.sha256()

        val gradleUserHome = project.gradle.gradleUserHomeDir
        val cacheDir = File(gradleUserHome, "caches/buildthing-std")
            .also(File::mkdirs)
        val standardJar = File(cacheDir, "buildthing-std-$hash.jar")

        if (!standardJar.exists())
            standardJar.writeBytes(bytes)

        // FIXME: Я точно помню в 1.21 neoforge проекте с похожим кодом у Gradle плагина - были особенности...
        project.afterEvaluate {
            project.dependencies {
                add("compileOnly", project.files(standardJar))
            }
        }
    }
}