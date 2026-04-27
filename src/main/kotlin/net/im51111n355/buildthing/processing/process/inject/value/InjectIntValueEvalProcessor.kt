package net.im51111n355.buildthing.processing.process.inject.value

import net.im51111n355.buildthing.processing.ProcessingProject
import net.im51111n355.buildthing.processing.process.common.AbstractBuildTimeEvalStep
import org.objectweb.asm.tree.MethodInsnNode

class InjectIntValueEvalProcessor(
    project: ProcessingProject
) : AbstractBuildTimeEvalStep(project) {
    override fun describeMethod() = "Inject.intValue"


    override fun canBeReplaced(min: MethodInsnNode)
        = min.owner == "net/im51111n355/buildthing/standard/Inject"
            && min.desc == "(Ljava/lang/String;)I"
            && min.name == "intValue"

    override fun makeReplacementCst(args: Array<Any?>): Any {
        val key = args[0] as String
        return this@InjectIntValueEvalProcessor.project.config.values[key] as Int
    }
}