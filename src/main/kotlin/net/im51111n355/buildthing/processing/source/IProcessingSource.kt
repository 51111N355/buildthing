package net.im51111n355.buildthing.processing.source

import java.io.File

interface IProcessingSource {
    operator fun iterator(): Iterator<File>
}