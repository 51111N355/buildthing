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