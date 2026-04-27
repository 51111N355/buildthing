package net.im51111n355.buildthing.processing

import org.objectweb.asm.tree.ClassNode
import java.io.File

data class ProcessingClassNode(
    val node: ClassNode,
    val file: File,
    var hasUnwrittenModifications: Boolean
)