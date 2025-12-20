package net.im51111n355.buildthing.dsl

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessInPlaceTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import java.util.concurrent.Callable

class BuildThingConfiguration(
    private val project: Project
) {
    fun buildProfile(
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

fun Project.buildthing(configure: BuildThingConfiguration.() -> Unit) {
    BuildThingConfiguration(this).configure()
}