package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.standard.InjectFlag
import net.im51111n355.buildthing.util.getRequiredArgument
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class InjectFlagProcessor(
    val master: BuildThingProcessor
) {
    fun process() {
        val tIf = Type.getType(InjectFlag::class.java)

        master.processAll { classNode ->
            var modified = false

            classNode.fields.forEach {
                val isStatic = (it.access and Opcodes.ACC_STATIC) != 0
                val isFinal = (it.access and Opcodes.ACC_FINAL) != 0
                val isInjectable = isStatic && isFinal && it.type == Type.BOOLEAN_TYPE

                it.visibleAnnotations?.removeIf { annotation ->
                    if (isInjectable && annotation.type == tIf) {
                        val value = annotation.getRequiredArgument<String>("value")
                        it.value = value in master.config.flags

                        modified = true
                        return@removeIf true
                    }

                    if (!isInjectable && annotation.type == tIf) {
                        master.project.logger.error("Field \"${it.name}\" in class \"${classNode.type.className}\" isnt static + final but has an @InjectFlag annotation!")
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