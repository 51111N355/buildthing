package net.im51111n355.buildthing.processing.source

import net.im51111n355.buildthing.processing.IProcessingSource
import java.io.File

// Для обычной обработки в одной processing папке
class DirectoryWalkProcessingSource(val directory: File) : IProcessingSource {
    override fun iterator()
        = directory.walkTopDown().iterator()
}