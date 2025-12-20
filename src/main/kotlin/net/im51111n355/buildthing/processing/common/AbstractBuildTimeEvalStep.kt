package net.im51111n355.buildthing.processing.common

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.processing.IProcessingStep
import net.im51111n355.buildthing.util.getConstantPushedValue
import net.im51111n355.buildthing.util.type
import org.gradle.api.GradleException
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.math.exp

// BuildTimeEval шаги это например заменить InjectRandom.randInt(10, 10) на случайное число
// Обязательно работает только со статичными целевыми методами!!!
abstract class AbstractBuildTimeEvalStep(
    val master: BuildThingProcessor,
) : IProcessingStep {

    // Для ошибок
    abstract fun describeMethod(): String

    // Проверка заменяется ли именно этот метод на что-то
    abstract fun canBeReplaced(min: MethodInsnNode): Boolean

    // Что передать в cst поле "ldc" на который заменяется вызов
    // Аргументы - какие константы передали в этот метод.
    abstract fun makeReplacementCst(args: Array<Any?>): Any

    override fun process() {
        var errors = false

        master.processAll { classNode ->
            var modified = false

            classNode.methods.forEach {
                var i = 0

                while (i < it.instructions.size()) {
                    val insn = it.instructions[i]
                    if (insn !is MethodInsnNode) {
                        i++
                        continue
                    }
                    if (insn.opcode != Opcodes.INVOKESTATIC) {
                        i++
                        continue
                    }
                    if (!canBeReplaced(insn)) {
                        i++
                        continue
                    }

                    // Сначала проверить если ldc на нужные типы на месте
                    val expectedTypes = Type.getArgumentTypes(insn.desc)

                    if (!expectedTypes.all(::isAllowedTargetMethodType)) {
                        i++
                        continue
                    }

                    val nArgs = expectedTypes.size
                    var dontInject = false

                    for (n in 0 until nArgs) {
                        val expectedType = expectedTypes[expectedTypes.size - n - 1]
                        val expectedLdc = i - (n + 1)
                        val hopefullyLdc = it.instructions[expectedLdc]
                        val value = hopefullyLdc.getConstantPushedValue()

                        if (value == null) {
                            errors = true
                            dontInject = true
                            master.project.logger.error("Expected constant to be passed to a build-time-evaluated method ${describeMethod()}. In method \"${it.name}\" of class \"${classNode.type.className}\".")
                            break
                        }

                        if (!isCompatibleType(expectedType, value.t)) {
                            // Никогда не должно случиться потому что это будет значить что класс битый (Переданные аргументы в метод не совпадают с самими аргументами метода)
                            throw AssertionError("${expectedType.className}, ${value.t?.javaClass?.name}")
                        }
                    }
                    // Скип если были ошибки
                    if (dontInject) {
                        i++
                        continue
                    }

                    modified = true

                    // Снести LDC, сам Invoke, заменить на значение из makeReplacementCst()
                    val removedLdcs = mutableListOf<Any?>()

                    for (n in 0 until nArgs) {
                        val expectedType = expectedTypes[expectedTypes.size - n - 1]
                        val expectedLdc = i - (n + 1)
                        val hopefullyLdc = it.instructions[expectedLdc]

                        val value = hopefullyLdc.getConstantPushedValue()
                            ?: throw AssertionError()

                        if (!isCompatibleType(expectedType, value.t))
                            throw AssertionError()

                        removedLdcs.add(0, castCompatibleTypes(expectedType, value.t))
                        it.instructions.remove(hopefullyLdc)
                    }

                    val newValue = makeReplacementCst(removedLdcs.toTypedArray())
                    val newInsn = LdcInsnNode(newValue)
                    it.instructions.set(insn, newInsn)

                    i = it.instructions.indexOf(newInsn)
                    i++
                }
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }

        if (errors)
            throw GradleException("Build Time evaluation step errors were found!")
    }

    private fun isAllowedTargetMethodType(type: Type): Boolean {
        return when (type) {
            Type.getType(String::class.java),
            Type.BOOLEAN_TYPE,
            Type.CHAR_TYPE,
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            Type.INT_TYPE,
            Type.FLOAT_TYPE,
            Type.LONG_TYPE,
            Type.DOUBLE_TYPE -> true
            else -> false
        }
    }

    private fun isCompatibleType(expected: Type, insnReturns: Any?): Boolean {
        return when (expected) {
            Type.BOOLEAN_TYPE,
            Type.CHAR_TYPE,
            Type.BYTE_TYPE,
            Type.SHORT_TYPE,
            // Проверка на int/byte/short - что-то связанное с BIPUSH, SIPUSH инструкциями
            Type.INT_TYPE -> insnReturns is Int || insnReturns is Byte || insnReturns is Short

            Type.FLOAT_TYPE -> insnReturns is Float

            Type.LONG_TYPE -> insnReturns is Long

            Type.DOUBLE_TYPE -> insnReturns is Double

            Type.getType(String::class.java) -> insnReturns is String

            // Не должно вообще случиться
            else -> throw AssertionError("Expected \"expected\" not to be an object!")
        }
    }

    private fun castCompatibleTypes(expected: Type, insnReturns: Any?): Any? {
        return when (expected) {
            Type.BOOLEAN_TYPE -> (insnReturns as Number) == 1
            Type.CHAR_TYPE -> (insnReturns as Number).toChar()
            Type.BYTE_TYPE -> (insnReturns as Number).toByte()
            Type.SHORT_TYPE -> (insnReturns as Number).toShort()
            Type.INT_TYPE -> (insnReturns as Number).toInt()

            Type.FLOAT_TYPE -> insnReturns as Float

            Type.LONG_TYPE -> insnReturns as Long

            Type.DOUBLE_TYPE -> insnReturns as Double

            Type.getType(String::class.java) -> insnReturns as String

            // Не должно вообще случиться
            else -> throw AssertionError("Expected \"expected\" not to be an object!")
        }
    }
}