package net.im51111n355.buildthing.processing.process.inject.random

import net.im51111n355.buildthing.processing.ProcessingProject
import net.im51111n355.buildthing.processing.process.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode
import java.util.concurrent.ThreadLocalRandom

class InjectFloatRandomEvalProcessor(
    project: ProcessingProject
) : AbstractBuildTimeEvalStep(project) {
    override fun describeMethod() = "Inject.randFloat"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(FF)F"
            && min.name == "randFloat"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val d = args[1] as Float - args[0] as Float
        return args[0] as Float + ThreadLocalRandom.current().nextFloat() * d
    }
}