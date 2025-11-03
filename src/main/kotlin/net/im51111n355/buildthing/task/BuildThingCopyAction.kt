package net.im51111n355.buildthing.task

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.BuildThingProcessor
import org.gradle.api.Project
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.tasks.WorkResult
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BuildThingCopyAction(
    val project: Project,
    val processIn: File,
    val targetFile: File,
    val config: BuildThingConfig
) : CopyAction {

    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        // Копирование в папку обработки
        stream.process {
            if (it.isDirectory)
                return@process

            it.copyTo(processIn.resolve(it.relativePath.pathString))
        }

        // Обработкаа
        val processor = BuildThingProcessor(project, processIn, config)
        processor.process()

        // Запаковать в .jar
        ZipOutputStream(targetFile.outputStream()).use { zip ->
            for (file in processIn.walkTopDown()) {
                if (file.isDirectory) continue

                val entry = ZipEntry(file.relativeTo(processIn).path.replace("\\", "/"))
                zip.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return WorkResult { true }
    }
}