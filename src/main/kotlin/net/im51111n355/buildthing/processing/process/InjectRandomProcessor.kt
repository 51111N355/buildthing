package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.processing.IProcessingStep
import net.im51111n355.buildthing.standard.InjectRandom
import net.im51111n355.buildthing.util.getOptionalArgument
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.concurrent.ThreadLocalRandom

class InjectRandomProcessor(
    val master: BuildThingProcessor
): IProcessingStep {
    override fun process() {
        val tIrInt = Type.getType(InjectRandom.Integer::class.java)
        val tIrFloat = Type.getType(InjectRandom.Float::class.java)
        val tIrLong = Type.getType(InjectRandom.Long::class.java)
        val tIrDouble = Type.getType(InjectRandom.Double::class.java)
        val anyTIr = listOf(tIrInt, tIrFloat, tIrLong, tIrDouble)

        master.processAll { classNode ->
            var modified = false

            classNode.fields.forEach {
                val isStatic = (it.access and Opcodes.ACC_STATIC) != 0
                val isFinal = (it.access and Opcodes.ACC_FINAL) != 0
                val isInjectable = isStatic && isFinal

                it.visibleAnnotations?.removeIf { annotation ->
                    if (isInjectable && it.type == Type.INT_TYPE && annotation.type == tIrInt) {
                        val min = annotation.getOptionalArgument<Int>("minInclusive", Int.MIN_VALUE)
                        val max = annotation.getOptionalArgument<Int>("maxExclusive", Int.MAX_VALUE)

                        val randomNumber = ThreadLocalRandom.current()
                            .nextInt(min, max)

                        it.value = randomNumber
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.FLOAT_TYPE &&  annotation.type == tIrFloat) {
                        val min = annotation.getOptionalArgument<Float>("min", Float.MIN_VALUE)
                        val max = annotation.getOptionalArgument<Float>("max", Float.MAX_VALUE)

                        val randomNumber = min + ThreadLocalRandom.current().nextFloat() * (max - min)

                        it.value = randomNumber
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.LONG_TYPE && annotation.type == tIrLong) {
                        val min = annotation.getOptionalArgument<Long>("minInclusive", Long.MIN_VALUE)
                        val max = annotation.getOptionalArgument<Long>("maxExclusive", Long.MAX_VALUE)

                        val randomNumber = ThreadLocalRandom.current()
                            .nextLong(min, max)

                        it.value = randomNumber
                        modified = true
                        return@removeIf true
                    }
                    if (isInjectable && it.type == Type.DOUBLE_TYPE && annotation.type == tIrDouble) {
                        val min = annotation.getOptionalArgument<Double>("min", Double.MIN_VALUE)
                        val max = annotation.getOptionalArgument<Double>("max", Double.MAX_VALUE)

                        val randomNumber = min + ThreadLocalRandom.current().nextDouble() * (max - min)

                        it.value = randomNumber
                        modified = true
                        return@removeIf true
                    }

                    if (!isInjectable && annotation.type in anyTIr) {
                        master.project.logger.error("Field \"${it.name}\" in class \"${classNode.type.className}\" isnt static + final but has an @InjectRandom annotation!")

                        modified = true
                        return@removeIf true
                    }

                    return@removeIf false
                }
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }
    }
}