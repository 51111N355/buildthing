package net.im51111n355.buildthing.dsl

import net.im51111n355.buildthing.task.IBuildThingTask
import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessInPlaceTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessOffPlaceTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

class BuildThingConfiguration(
    private val project: Project
) {
    fun buildProfile(
        name: String,

        fromTask: Task = project.tasks.named<Jar>("jar").get(),
        configure: IBuildThingTask.() -> Unit
    ) {
        project.tasks.create<BuildThingJarTask>("build$name") {
            description  = "Build $name"
            group = "buildthing build profile"

            dependsOn(fromTask)

            from(Callable {
                fromTask
                    .outputs.files
                    .map { project.zipTree(it) }
            })

            configure()

            archiveClassifier.set("[$name]")
        }
    }

    fun processClassesBeforeTask(
        name: String,
        beforeTask: Task,

        fromTask: Task = project.tasks.named<Jar>("jar").get(),
        configure: IBuildThingTask.() -> Unit
    ) {
        throw GradleException("Replace processClassesBeforeTask(String, Task, Task, Configure) with processClassesForExec(String, JavaExec, AbstractArchiveTask, Configure)")
    }

    fun processJarBeforeTask(
        name: String,
        beforeTask: Task,

        fromTask: AbstractArchiveTask = project.tasks.named<Jar>("jar").get(),
        configure: IBuildThingTask.() -> Unit
    ) {
        throw GradleException("Replace processJarBeforeTask(String, Task, AbstractArchiveTask, Configure) with processJarForExec(String, JavaExec, AbstractArchiveTask, Configure)")
    }

    fun processClassesForExec(
        name: String,
        // Именно для JavaExec ситуаций !!!
        beforeTask: JavaExec,

        // На архив какого таска обратить внимание при проверке на то не вложили ли .jar билд самого проекта в classpath
        // ForgeGradle ложит .jar мода в classpath (вместе с classes папками одновременно). Нужно будет подумать об обоих при обработке для JavaExec.
        considerArchiveTask: AbstractArchiveTask = project.tasks.named<Jar>("jar").get(),

        configure: IBuildThingTask.() -> Unit
    ) {
        // НЕ ЗНАЮ почему у меня ИМЕННО этот (project.sourceSets) аксессор, ИМЕННО из плагина (но не из билд-скрипта) не доступен.
        val sourceSets = project.extensions.getByName<SourceSetContainer>("sourceSets")
        val main = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME)
        val originalClasses = main.map { it.output.classesDirs } // <- Внимательно! Тут нельзя чтобы случался resolve!

        val classesOffPlace = project.tasks.create<BuildThingProcessOffPlaceTask>("processExecClasses$name") {
            description = "Build Classses - $name for JavaExec (${beforeTask.name})"
            group = "other"

            dependsOn(main.map { it.classesTaskName }) // Это dependsOn("classes"). Я в шоке что и такое есть.
            sourceDirectories.set(
                sourceDirectories.getOrElse(project.files())
                    .plus(project.files(originalClasses))
            )

            config.disableCutter = true
            configure()
        }

        // Подменить classpath изменения классов
        beforeTask.dependsOn(classesOffPlace)

        beforeTask.classpath = beforeTask.classpath
            .minus(project.files(originalClasses))
            .plus(project.files(classesOffPlace::figureOutDestinationDirectories))
    }

    fun processJarForExec(
        name: String,
        // Именно для JavaExec ситуаций !!!
        beforeTask: JavaExec,

        // На архив какого таска обратить внимание при проверке на то не вложили ли .jar билд самого проекта в classpath
        // ForgeGradle ложит .jar мода в classpath (вместе с classes папками одновременно). Нужно будет подумать об обоих при обработке для JavaExec.
        considerArchiveTask: AbstractArchiveTask = project.tasks.named<Jar>("jar").get(),

        configure: IBuildThingTask.() -> Unit
    ) {
        // Та самая ситуация к которой относится "considerArchiveTask", детектор включения своего собственного .jar в зависимостях
        // Только в afterEvaluate потому что отсюда уже 100% может произойти просчёт конфигураций
        project.afterEvaluate {
            val iter = beforeTask.classpath.iterator()
            var found = false

            while (iter.hasNext()) {
                val el = iter.next()

                if (el == considerArchiveTask.archiveFile.get().asFile) {
                    found = true
                    break
                }
            }

            if (!found) return@afterEvaluate

            val jar = project.tasks.create<BuildThingJarTask>("processExecJar$name") {
                description = "Build Jar - $name for JavaExec (${beforeTask.name})"
                group = "other"

//                dependsOn(main.map { it.classesTaskName })
                dependsOn(considerArchiveTask)
                from(Callable {
                    considerArchiveTask
                        .outputs.files
                        .map { project.zipTree(it) }
                })

                config.disableCutter = true
                configure()

                destinationDirectory.set(processingDirectory)
                archiveClassifier.set("[$name]")
            }

            beforeTask.dependsOn(jar)
            beforeTask.classpath = beforeTask.classpath
                .minus(project.files(considerArchiveTask.archiveFile))
                .plus(project.files(jar.archiveFile))
        }
    }
}

fun Project.buildthing(configure: BuildThingConfiguration.() -> Unit) {
    BuildThingConfiguration(this).configure()
}