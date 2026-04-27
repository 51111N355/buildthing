package net.im51111n355.buildthing.processing

import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.processing.process.RemoveAnnotationsProcessor
import net.im51111n355.buildthing.processing.process.FlagCuttingProcessor
import net.im51111n355.buildthing.processing.process.inject.InjectClassListProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectBooleanValueEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectDoubleValueEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.InjectFlagEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.random.InjectDoubleRandomEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.random.InjectFloatRandomEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.random.InjectIntRandomEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.random.InjectLongRandomEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectFloatValueEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectIntValueEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectLongValueEvalProcessor
import net.im51111n355.buildthing.processing.process.inject.value.InjectStringValueEvalProcessor
import net.im51111n355.buildthing.processing.source.IProcessingSource
import net.im51111n355.buildthing.util.ClassPathIndex
import net.im51111n355.buildthing.util.SafeCW
import net.im51111n355.buildthing.util.type
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class ProcessingProject(
    val gradleProject: Project,
    val files: IProcessingSource,
    val config: BuildThingConfig
) {
    val index = ClassPathIndex(this)

    // Инстансы ClassNode на которых проходит новый processAll
    private val allClassNodes = mutableListOf<ProcessingClassNode>()

    private val processors = listOf(
        FlagCuttingProcessor(this),

        InjectFlagEvalProcessor(this),
        InjectIntValueEvalProcessor(this),
        InjectFloatValueEvalProcessor(this),
        InjectLongValueEvalProcessor(this),
        InjectDoubleValueEvalProcessor(this),
        InjectStringValueEvalProcessor(this),
        InjectBooleanValueEvalProcessor(this),

        InjectIntRandomEvalProcessor(this),
        InjectFloatRandomEvalProcessor(this),
        InjectLongRandomEvalProcessor(this),
        InjectDoubleRandomEvalProcessor(this),

        InjectClassListProcessor(this),

        RemoveAnnotationsProcessor(this),
    )

    fun process() {
        // Индекс - библиотеки
        gradleProject.configurations.findByName("compileClasspath")!!
            .files.forEach(index::index)

        // Индекс - классы самого проекта
        for (file in files) {
            if (file.extension != "class") continue

            val bytes = file.readBytes()
            index.index(bytes)
        }

        // Загрузить все классы
        for (file in files) {
            if (file.extension != "class") continue

            // Чтение
            val classNode = ClassNode()
            val classReader = ClassReader(file.readBytes())
            classReader.accept(classNode, ClassReader.SKIP_DEBUG) // <- Поддерживать Node'ы на всякие номера строк очень бесит на самом деле, так что пока что SKIP_DEBUG

            allClassNodes.add(ProcessingClassNode(classNode, file, false))
        }

        // Обработка
        for (step in processors)
            step.process()

        // Сохранить все оставшиеся ClassNode
        for (node in allClassNodes) {
            val (classNode, file, modified) = node
            if (!modified) continue

            try {
                val classWriter = SafeCW(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES, index)
                classNode.accept(classWriter)
                file.writeBytes(classWriter.toByteArray())
            } catch (e: Exception) {
                throw ProcessingCrashException(node, null, e)
            } catch (e: ProcessingCrashException) {
                throw e.copy(inClass = node)
            }
        }
    }

    fun processAllClasses(modify: (ClassNode) -> ProcessingResult) {
        val iter = allClassNodes.iterator()

        while (iter.hasNext()) {
            val data = iter.next()

            val result = try {
                modify(data.node)
            } catch (e: Exception) {
                throw ProcessingCrashException(data, null, e)
            } catch (c: ProcessingCrashException) {
                throw c.copy(inClass = data)
            }

            if (result == ProcessingResult.NOT_MODIFIED) {
                continue
            }

            if (result == ProcessingResult.MODIFIED) {
                data.hasUnwrittenModifications = true
                if (config.logModifications) {
                    gradleProject.logger.warn("└ Class ${data.node.type.className} modified")
                }

                continue
            }

            if (result == ProcessingResult.DELETE) {
                iter.remove()
                data.file.delete()
                continue
            }
        }
    }

    fun processAllMethods(modify: (ClassNode, MethodNode) -> ProcessingResult) {
        processAllClasses { classNode ->
            var classModified = false

            classNode.methods.removeIf { methodNode ->
                val memberForCrashReport = MemberInfo(classNode.name, methodNode.name, methodNode.desc)
                val result = try {
                    modify(classNode, methodNode)
                } catch (e: Exception) {
                    throw ProcessingCrashException(null, memberForCrashReport, e)
                } catch (c: ProcessingCrashException) {
                    throw c.copy(inMember = memberForCrashReport)
                }

                if (result != ProcessingResult.NOT_MODIFIED) {
                    classModified = true

                    if (config.logModifications) {
                        gradleProject.logger.warn("| Method ${classNode.type.className}#${methodNode.name} modified")
                    }
                }

                return@removeIf result == ProcessingResult.DELETE
            }

            return@processAllClasses if (classModified)
                ProcessingResult.MODIFIED
            else
                ProcessingResult.NOT_MODIFIED
        }
    }

    fun processAllFields(modify: (ClassNode, FieldNode) -> ProcessingResult) {
        processAllClasses { classNode ->
            var classModified = false

            classNode.fields.removeIf { fieldNode ->
                val memberForCrashReport = MemberInfo(classNode.name, fieldNode.name, fieldNode.desc)
                val result = try {
                    modify(classNode, fieldNode)
                } catch (e: Exception) {
                    throw ProcessingCrashException(null, memberForCrashReport, e)
                } catch (c: ProcessingCrashException) {
                    throw c.copy(inMember = memberForCrashReport)
                }

                if (result != ProcessingResult.NOT_MODIFIED) {
                    classModified = true

                    if (config.logModifications) {
                        gradleProject.logger.warn("| Field ${classNode.type.className}#${fieldNode.name} modified")
                    }
                }

                return@removeIf result == ProcessingResult.DELETE
            }

            return@processAllClasses if (classModified)
                ProcessingResult.MODIFIED
            else
                ProcessingResult.NOT_MODIFIED
        }
    }
}