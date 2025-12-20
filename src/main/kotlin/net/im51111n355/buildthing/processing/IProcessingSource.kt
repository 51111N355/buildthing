package net.im51111n355.buildthing.processing

import java.io.File

interface IProcessingSource {
    operator fun iterator(): Iterator<File>
}

// Для обычной обработки в одной processing папке
class DirectoryWalkProcessingSource(val directory: File) : IProcessingSource {
    override fun iterator()
        = directory.walkTopDown().iterator()
}

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