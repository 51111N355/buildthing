package net.im51111n355.buildthing.processing.process.inject.value

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode

class InjectDoubleValueEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.doubleValue"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(Ljava/lang/String;)D"
            && min.name == "doubleValue"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val key = args[0] as String
        return master.config.values[key] as Double
    }
}