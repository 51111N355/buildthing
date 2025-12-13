package net.im51111n355.buildthing.task.build

import net.im51111n355.buildthing.config.BuildThingConfig
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.Input
import org.gradle.jvm.tasks.Jar

open class BuildThingJarTask : Jar() {
    @get:Input
    val config: BuildThingConfig = BuildThingConfig()

    override fun createCopyAction(): CopyAction {
        val processingDirectory = project.buildDir.resolve("buildthing-jar-processing")
        processingDirectory.deleteRecursively()
        processingDirectory.mkdirs()

        val targetFile = destinationDirectory.asFile.get()
            .resolve(archiveFileName.get())

        return BuildThingJarCopyAction(
            project,
            processingDirectory,
            targetFile,
            config
        )
    }
}