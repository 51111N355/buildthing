package net.im51111n355.buildthing.processing.process.inject.random

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import net.im51111n355.buildthing.standard.Inject
import org.objectweb.asm.tree.MethodInsnNode

class InjectIntRandomEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.randInt"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(II)I"
            && min.name == "randInt"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        return Inject.randInt(args[0] as Int, args[1] as Int)
    }
}