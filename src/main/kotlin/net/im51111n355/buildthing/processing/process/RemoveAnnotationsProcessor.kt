package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.processing.IProcessingStep
import net.im51111n355.buildthing.standard.FlagCuttable
import net.im51111n355.buildthing.standard.InjectRandom
import net.im51111n355.buildthing.standard.RemoveAtCallsite
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode

class RemoveAnnotationsProcessor(
    val master: BuildThingProcessor
): IProcessingStep {
    override fun process() {
        master.processAll { classNode ->
            var modified = false

            classNode.visibleAnnotations
                ?.removeIf {
                    val remove = shouldRemoveAnnotation(it)
                    if (remove) modified = true

                    return@removeIf remove
                }

            classNode.methods.forEach {
                it.visibleAnnotations
                    ?.removeIf {
                        val remove = shouldRemoveAnnotation(it)
                        if (remove) modified = true

                        return@removeIf remove
                    }
            }

            classNode.fields.forEach {
                it.visibleAnnotations
                    ?.removeIf {
                        val remove = shouldRemoveAnnotation(it)
                        if (remove) modified = true

                        return@removeIf remove
                    }
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }
    }

    private fun shouldRemoveAnnotation(node: AnnotationNode): Boolean {
        // InjectRandom.XXX удаляются сами
        // FlagCuttable/RemoveAtCallsite удаляются только тут если включено

        return node.type == Type.getType(FlagCuttable::class.java)
                || node.type == Type.getType(RemoveAtCallsite::class.java)
    }
}