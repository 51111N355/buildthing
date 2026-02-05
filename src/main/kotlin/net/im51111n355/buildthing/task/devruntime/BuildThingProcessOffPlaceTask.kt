package net.im51111n355.buildthing.task.devruntime

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.source.MultipleSourcesProcessingSource
import net.im51111n355.buildthing.task.IBuildThingTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildThingProcessOffPlaceTask() : DefaultTask(), IBuildThingTask {
    // Тут будут только папки из sourcesets.main.classesDirs (Например путь к java, kotlin классам)
    @get:Input
    abstract val sourceDirectories: Property<FileCollection>

    @get:Input
    override val config = BuildThingConfig()

    fun figureOutDestinationDirectories(): List<File> {
        val list = mutableListOf<File>()

        val processingDirectory = project.buildDir.resolve(processingDirectory)
        val classesDir = project.buildDir.resolve("classes")

        for (directory in sourceDirectories.get()) {
            val relative = directory.relativeTo(classesDir)
            val inProcessing = processingDirectory.resolve(relative)
            list.add(inProcessing)
        }

        return list
    }

    @TaskAction
    fun execute() {
        val processingDirectory = project.buildDir.resolve(processingDirectory)
        processingDirectory.deleteRecursively()
        processingDirectory.mkdirs()

        val classesDir = project.buildDir.resolve("classes")
        val sourceDirs = mutableListOf<File>()

        for (directory in sourceDirectories.get()) {
            val relative = directory.relativeTo(classesDir)
            val inProcessing = processingDirectory.resolve(relative)
            directory.copyRecursively(inProcessing)
            sourceDirs.add(inProcessing)
        }

        val processingSource = MultipleSourcesProcessingSource(sourceDirs)
        val processor = BuildThingProcessor(project, processingSource, config)
        processor.process()
    }
}