package net.im51111n355.buildthing.task

import net.im51111n355.buildthing.config.BuildThingConfig
import org.gradle.api.Task
import org.gradle.api.tasks.Internal
import java.io.File

// Для вызова `configure()` блоков где может быть несколько "реальных" тасков на самом деле (Ситуация с правильным processExec и .jar проекта в runtime classpath)
interface IBuildThingTask {
    // Если вдруг нужно делать что-то именно с таском
    fun asTask(): Task = this as Task

    // Сами настройки
    val config: BuildThingConfig

    // Путь к папке который обычно будет "build/buildthing-processing/${task.name}"
    @get:Internal // <- Gradle бесится что там будет getXxx без @Input
    val processingDirectory: File
        get() {
            val task = asTask()
            val project = task.project

            return project.buildDir.resolve("buildthing-processing/${task.name}")
        }
}