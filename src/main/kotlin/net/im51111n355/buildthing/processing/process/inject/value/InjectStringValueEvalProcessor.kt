package net.im51111n355.buildthing.processing.process.inject.value

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode

class InjectStringValueEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.stringValue"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(Ljava/lang/String;)Ljava/lang/String;"
            && min.name == "stringValue"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val key = args[0] as String
        return master.config.values[key] as String
    }
}