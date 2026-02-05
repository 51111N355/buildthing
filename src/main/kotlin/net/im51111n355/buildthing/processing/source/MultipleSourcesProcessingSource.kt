package net.im51111n355.buildthing.processing.source

import net.im51111n355.buildthing.processing.IProcessingSource
import java.io.File
import kotlin.collections.flatMap

// Для обработки в нескольких источниках
class MultipleSourcesProcessingSource(val sources: List<File>) : IProcessingSource {
    private val internalFullList = mutableListOf<File>()

    init {
        internalFullList.addAll(sources
            .filter { it.isFile })

        internalFullList.addAll(sources
            .filter { it.isDirectory }
            .flatMap(File::walkTopDown))
    }

    override fun iterator() = internalFullList.iterator()
}