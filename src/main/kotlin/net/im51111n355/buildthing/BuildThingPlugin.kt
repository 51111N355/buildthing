package net.im51111n355.buildthing

import net.im51111n355.buildthing.dsl.BuildThingGroovyDsl
import net.im51111n355.buildthing.dsl.buildthing
import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.util.sha256
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable

class BuildThingPlugin : Plugin<Project> {
    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project

        injectStd()
        setupGroovyDsl()
    }

    private fun setupGroovyDsl() {
        project.extensions.create("buildthing", BuildThingGroovyDsl::class.java, project)
    }

    // Добавляет standard.jar в зависимости, обрабатывает то что может быть несколько версий плагина с разными standard.jar (привязка к sha256)
    private fun injectStd() {
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

        project.dependencies {
            add("implementation", project.files(standardJar))
        }
    }
}