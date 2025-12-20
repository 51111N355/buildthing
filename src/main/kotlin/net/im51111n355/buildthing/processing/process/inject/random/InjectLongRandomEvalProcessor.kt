package net.im51111n355.buildthing.processing.process.inject.random

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.common.AbstractBuildTimeEvalStep
import net.im51111n355.buildthing.standard.Inject
import org.objectweb.asm.tree.MethodInsnNode
import java.util.concurrent.ThreadLocalRandom

class InjectLongRandomEvalProcessor(
    master: BuildThingProcessor
) : AbstractBuildTimeEvalStep(master) {
    override fun describeMethod() = "Inject.randLong"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(JJ)J"
            && min.name == "randLong"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        return ThreadLocalRandom.current().nextLong(args[0] as Long, args[1] as Long)
    }
}