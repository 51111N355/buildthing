package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.processing.IProcessingStep
import net.im51111n355.buildthing.standard.InjectFlag
import net.im51111n355.buildthing.util.getRequiredArgument
import net.im51111n355.buildthing.util.isStatic
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode

class InjectFlagProcessor(
    val master: BuildThingProcessor
): IProcessingStep {
    override fun process() {
        val tIf = Type.getType(InjectFlag::class.java)

        master.processAll { classNode ->
            var modified = false
            val injectedFields = mutableListOf<FieldInfo>()

            classNode.fields.forEach {
                val isStatic = (it.access and Opcodes.ACC_STATIC) != 0
                val isFinal = (it.access and Opcodes.ACC_FINAL) != 0
                val isInjectable = isStatic && isFinal && it.type == Type.BOOLEAN_TYPE

                it.visibleAnnotations?.removeIf { annotation ->
                    if (isInjectable && annotation.type == tIf) {
                        val value = annotation.getRequiredArgument<String>("value")
                        it.value = if (value in master.config.flags) 1 else 0

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }

                    if (!isInjectable && annotation.type == tIf) {
                        master.project.logger.error("Field \"${it.name}\" in class \"${classNode.type.className}\" isnt static + final but has an @InjectFlag annotation!")
                        modified = true
                        return@removeIf true
                    }

                    return@removeIf false
                }
            }

            // Снос дефолт значений полей из static { ... } блока если использовали обходилку Inline'инга Java
            // Например IS_SERVER = some() < some(); (Чтобы компилятор не совершил inline'инг) - компилятор поместит в clinit который приоритетние дефолт значения. Тут удалится put инструкция из clinit.
            classNode.methods.forEach {
                if (it.name != "<clinit>" || it.desc != "()V" || !it.isStatic)
                    return@forEach

                for (i in 0 until it.instructions.size()) {
                    val insn = it.instructions[i]

                    if (insn is FieldInsnNode && insn.opcode == Opcodes.PUTSTATIC) {
                        val info = FieldInfo(insn.owner, insn.name, insn.desc)
                        if (info !in injectedFields) continue

                        // Заменить установку поля на снятие того что загрузили БЫ со стака
                        val type = Type.getType(insn.desc)

                        val newInsn = when (type) {
                            Type.DOUBLE_TYPE, Type.LONG_TYPE -> InsnNode(Opcodes.POP2)
                            else -> InsnNode(Opcodes.POP)
                        }

                        modified = true
                        it.instructions.set(insn, newInsn)
                    }
                }
            }

            if (modified) {
                master.project.logger.error("==========================================================================================================================================================================================")
                master.project.logger.error(" | Class \"${classNode.type.className}\" uses the @InjectFlag annotation which in the last version of BuildThing is deprecated, and is supposed to be replaced with Inject.flag calls.")
                master.project.logger.error(" | Check out the latest documentation @ https://github.com/51111N355/buildthing for information on how to use the `Inject` class =)")
                master.project.logger.error(" | For now this will still compile, but in a week or two @InjectFlag will be completely removed.")
                master.project.logger.error("==========================================================================================================================================================================================")


                master.project.logger.error("==========================================================================================================================================================================================")
                master.project.logger.error(" | Класс \"${classNode.type.className}\" использует @InjectFlag аннотацию, что в последней версии BuildThing не рекомендуется, и нужно заменить на Inject.flag вызовы.")
                master.project.logger.error(" | Посмотрите последнюю документацию на https://github.com/51111N355/buildthing для информации о том как использовать `Inject` класс =)")
                master.project.logger.error(" | For now this will still compile, but in a week or two @InjectFlag will be completely removed.")
                master.project.logger.error("==========================================================================================================================================================================================")
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }
    }

    private data class FieldInfo(
        val owner: String,
        val name: String,
        val desc: String
    )
}