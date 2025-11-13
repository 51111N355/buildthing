package net.im51111n355.buildthing.processing.process.inject.random

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import net.im51111n355.buildthing.standard.Inject
import org.objectweb.asm.tree.MethodInsnNode

class InjectDoubleRandomEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.randDouble"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(DD)D"
            && min.name == "randDouble"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        return Inject.randDouble(args[0] as Double, args[1] as Double)
    }
}