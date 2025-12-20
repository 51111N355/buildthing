package net.im51111n355.buildthing

import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessInPlaceTask
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

    @JvmOverloads
    fun processClassesBeforeTask(
        name: String,
        beforeTask: DefaultTask,

        fromTask: DefaultTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingProcessInPlaceTask.() -> Unit
    ) {
        project.tasks.create<BuildThingProcessInPlaceTask>("processInPlace$name") {
            description = "Runs processing for development runtime"
            group = "buildthing process in place"

            // Зависимости чтобы обязательно вызывался после вызова "beforetask" таска
            // Тут же нужно чтобы вызывалося ПОСЛЕ classes, но это будет задават пользователь
            beforeTask.dependsOn(this)

            val sources = fromTask.inputs.files.files.toList()
            sourceDirectories.addAll(sources)

            configure()
        }
    }

    @JvmOverloads
    fun processJarBeforeTask(
        name: String,
        beforeTask: DefaultTask,

        fromTask: AbstractArchiveTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingJarTask.() -> Unit
    ) {
        project.tasks.create<BuildThingJarTask>("processJarInPlace$name") {
            description = "Runs processing for development runtime"
            group = "buildthing process in place"
            // Обязательно полностью копирует название
            archiveFileName.set(fromTask.archiveFileName.get())

            dependsOn(fromTask)
            beforeTask.dependsOn(this)

            from(Callable {
                fromTask
                    .outputs.files
                    .map { project.zipTree(it) }
            })

            configure()
        }
    }
}