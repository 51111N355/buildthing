package net.im51111n355.buildthing.processing.process.inject.random

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import net.im51111n355.buildthing.standard.Inject
import org.objectweb.asm.tree.MethodInsnNode

class InjectFloatRandomEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.randFloat"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(FF)F"
            && min.name == "randFloat"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        return Inject.randFloat(args[0] as Float, args[1] as Float)
    }
}