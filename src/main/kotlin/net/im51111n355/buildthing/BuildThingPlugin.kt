package net.im51111n355.buildthing

import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.util.sha256
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
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
                // FIXME: Если пользователи используют maven publish то я не знаю, не получится ли что standard добавится в pom.xml.
                // FIXME: Может быть это не произойдет потому std - файл, но я не уверен, проверить бы.
                add("implementation", project.files(standardJar))
            }
        }
    }

    @JvmOverloads
    fun sideTask(
        name: String,
        fromTask: DefaultTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingJarTask.() -> Unit
    ) {
        project.tasks.create<BuildThingJarTask>("build$name") {
            description  = "Build $name"
            group = "buildthing build profile"
            archiveClassifier.set("[$name]")

            dependsOn(fromTask)

            from(Callable {
                fromTask
                    .outputs.files
                    .map { project.zipTree(it) }
            })

            configure()
        }
    }
}