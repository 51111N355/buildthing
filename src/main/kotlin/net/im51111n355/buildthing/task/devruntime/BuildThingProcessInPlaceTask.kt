package net.im51111n355.buildthing.task.devruntime

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.MultipleSourcesProcessingSource
import net.im51111n355.buildthing.task.IBuildThingTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

open class BuildThingProcessInPlaceTask() : DefaultTask(), IBuildThingTask {
    @get:Input
    override val config = BuildThingConfig().also {
        it.disableCutter = true
    }

    // Директории на которых происходит обработка
    // Это, скорее всего, будут ./build/classes/java, ./build/classes/kotlin, ресурсы и похожие
    @get:Input
    val sourceDirectories = mutableListOf<File>()

    @TaskAction
    fun execute() {
        val processingSource = MultipleSourcesProcessingSource(sourceDirectories)
        val processor = BuildThingProcessor(project, processingSource, config)
        processor.process()
    }
}