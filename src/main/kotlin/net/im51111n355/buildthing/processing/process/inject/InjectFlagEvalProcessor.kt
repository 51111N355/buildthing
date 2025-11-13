package net.im51111n355.buildthing.processing.process.inject

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode

class InjectFlagEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.flag"

    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(Ljava/lang/String;)Z"
            && min.name == "flag"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val flag = args[0] as String
        val present = flag in master.config.flags

        return present
    }
}