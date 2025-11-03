package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.standard.InjectValue
import net.im51111n355.buildthing.util.getOptionalArgument
import net.im51111n355.buildthing.util.getRequiredArgument
import net.im51111n355.buildthing.util.isStatic
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode

class InjectValueProcessor(
    val master: BuildThingProcessor
) {
    fun process() {
        val tIvInt = Type.getType(InjectValue.Integer::class.java)
        val tIvFloat = Type.getType(InjectValue.Float::class.java)
        val tIvLong = Type.getType(InjectValue.Long::class.java)
        val tIvDouble = Type.getType(InjectValue.Double::class.java)
        val tIvString = Type.getType(InjectValue.String::class.java)
        val tIvBoolean = Type.getType(InjectValue.Boolean::class.java)

        master.processAll { classNode ->
            var modified = false
            val injectedFields = mutableListOf<FieldInfo>()

            classNode.fields.forEach {
                val isStatic = (it.access and Opcodes.ACC_STATIC) != 0
                val isFinal = (it.access and Opcodes.ACC_FINAL) != 0
                val isInjectable = isStatic && isFinal

                // Установка значения из аннотации
                it.visibleAnnotations?.removeIf { annotation ->
                    if (isInjectable && it.type == Type.INT_TYPE && annotation.type == tIvInt) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as Int

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.FLOAT_TYPE && annotation.type == tIvFloat) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as Float

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.LONG_TYPE && annotation.type == tIvLong) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as Long

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.DOUBLE_TYPE && annotation.type == tIvDouble) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as Double

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.getType(String::class.java) && annotation.type == tIvString) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as String

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.getType(Boolean::class.java) && annotation.type == tIvBoolean) {
                        val key = annotation.getRequiredArgument<String>("value")
                        val value = master.config.values[key] as Boolean

                        it.value = value

                        injectedFields.add(FieldInfo(classNode.name, it.name, it.desc))
                        modified = true
                        return@removeIf true
                    }
                    return@removeIf false
                }
            }

            // Снос дефолт значений полей из static { ... } блока если использовали обходилку Inline'инга Java
            // Например сделали BUILD_TIME = System.currentTimeMillis() - компилятор поместит это в clinit. Тут удалится put инструкция из clinit.
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