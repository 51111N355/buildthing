package net.im51111n355.buildthing.task

import net.im51111n355.buildthing.config.BuildThingConfig
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.tasks.Jar
import java.io.File

open class BuildThingTask : Jar() {
    @get:Input
    val config: BuildThingConfig = BuildThingConfig()

    override fun createCopyAction(): CopyAction {
        val processingDirectory = project.buildDir.resolve("buildthing-processing")
        processingDirectory.deleteRecursively()
        processingDirectory.mkdirs()

        val targetFile = destinationDirectory.asFile.get()
            .resolve(archiveFileName.get())

        return BuildThingCopyAction(
            project,
            processingDirectory,
            targetFile,
            config
        )
    }
}