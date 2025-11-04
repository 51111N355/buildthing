package net.im51111n355.buildthing.processing

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.process.RemoveAnnotationsProcessor
import net.im51111n355.buildthing.processing.process.FlagCuttingProcessor
import net.im51111n355.buildthing.processing.process.InjectFlagProcessor
import net.im51111n355.buildthing.processing.process.InjectRandomProcessor
import net.im51111n355.buildthing.processing.process.InjectValueProcessor
import net.im51111n355.buildthing.util.ClassPathIndex
import net.im51111n355.buildthing.util.SafeCW
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File

class BuildThingProcessor(
    val project: Project,
    val processIn: File,
    val config: BuildThingConfig
) {
    val index = ClassPathIndex(this)

    // Инстансы ClassNode на которых проходит новый processAll
    private val allClassNodes = mutableListOf<LoadedClassNode>()

    private val injectRandomProcessor = InjectRandomProcessor(this)
    private val injectFlagProcessor = InjectFlagProcessor(this)
    private val injectValueProcessor = InjectValueProcessor(this)
    private val flagCuttingProcessor = FlagCuttingProcessor(this)
    private val removeAnnotationsProcessor = RemoveAnnotationsProcessor(this)

    fun process() {
        // Индекс - библиотеки
        project.configurations.findByName("compileClasspath")!!
            .files.forEach(index::index)

        // Индекс - классы самого проекта
        for (file in processIn.walkTopDown()) {
            if (file.extension != "class") continue

            val bytes = file.readBytes()
            index.index(bytes)
        }

        // Загрузить все классы
        for (file in processIn.walkTopDown()) {
            if (file.extension != "class") continue

            // Чтение
            val classNode = ClassNode()
            val classReader = ClassReader(file.readBytes())
            classReader.accept(classNode, 0)

            allClassNodes.add(LoadedClassNode(classNode, file, false))
        }

        // Обработка
        injectRandomProcessor.process()
        injectFlagProcessor.process()
        injectValueProcessor.process()
        flagCuttingProcessor.process()
        removeAnnotationsProcessor.process()

        // Сохранить все оставшиеся ClassNode
        for (node in allClassNodes) {
            val (classNode, file, modified) = node
            if (!modified) continue

            val classWriter = SafeCW(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES, index)
            classNode.accept(classWriter)
            file.writeBytes(classWriter.toByteArray())
        }
    }

    fun processAll(modify: (ClassNode) -> ProcessAllAction) {
        val iter = allClassNodes.iterator()

        while (iter.hasNext()) {
            val data = iter.next()
            val result =  modify(data.node)

            if (result == ProcessAllAction.NOT_MODIFIED) {
                continue
            }

            if (result == ProcessAllAction.MODIFIED) {
                data.modifiedEver = true
                continue
            }

            if (result == ProcessAllAction.DELETE) {
                iter.remove()
                data.file.delete()
                continue
            }
        }
    }

    private data class LoadedClassNode(
        val node: ClassNode,
        val file: File,
        var modifiedEver: Boolean
    )

    enum class ProcessAllAction {
        NOT_MODIFIED,
        MODIFIED,
        DELETE
    }
}