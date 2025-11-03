package net.im51111n355.buildthing.processing

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.process.RemoveAnnotationsProcessor
import net.im51111n355.buildthing.processing.process.FlagCuttingProcessor
import net.im51111n355.buildthing.processing.process.InjectFlagProcessor
import net.im51111n355.buildthing.processing.process.InjectRandomProcessor
import net.im51111n355.buildthing.processing.process.InjectValueProcessor
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File

class BuildThingProcessor(
    val project: Project,
    val processIn: File,
    val config: BuildThingConfig
) {

    private val injectRandomProcessor = InjectRandomProcessor(this)
    private val injectFlagProcessor = InjectFlagProcessor(this)
    private val injectValueProcessor = InjectValueProcessor(this)
    private val flagCuttingProcessor = FlagCuttingProcessor(this)
    private val removeAnnotationsProcessor = RemoveAnnotationsProcessor(this)

    fun process() {
        injectRandomProcessor.process()
        injectFlagProcessor.process()
        injectValueProcessor.process()
        flagCuttingProcessor.process()
        removeAnnotationsProcessor.process()
    }

    inline fun processAll(modify: (ClassNode) -> ProcessAllAction) {
        for (file in processIn.walkTopDown()) {
            if (file.extension != "class") continue

            // Чтение
            val classNode = ClassNode()
            val classReader = ClassReader(file.readBytes())
            classReader.accept(classNode, Opcodes.ASM9)

            // Обработка
            val action = modify(classNode)

            if (action == ProcessAllAction.NOT_MODIFIED) {
                // Пропускаем
                continue
            }

            if (action == ProcessAllAction.MODIFIED) {
                // Запись
                val classWriter = ClassWriter(0)
                classNode.accept(classWriter)
                file.writeBytes(classWriter.toByteArray())
            }

            if (action == ProcessAllAction.DELETE) {
                // Удалить класс полностью
                file.delete()
                continue
            }
        }
    }

    enum class ProcessAllAction {
        NOT_MODIFIED,
        MODIFIED,
        DELETE
    }
}