package net.im51111n355.buildthing.processing.process.inject.value

import net.im51111n355.buildthing.processing.ProcessingProject
import net.im51111n355.buildthing.processing.process.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode

class InjectFloatValueEvalProcessor(
    project: ProcessingProject
) : AbstractBuildTimeEvalStep(project) {
    override fun describeMethod() = "Inject.floatValue"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(Ljava/lang/String;)F"
            && min.name == "floatValue"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val key = args[0] as String
        return project.config.values[key] as Float
    }
}