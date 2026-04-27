package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.ProcessingProject
import net.im51111n355.buildthing.processing.ProcessingResult
import net.im51111n355.buildthing.standard.ClassList
import net.im51111n355.buildthing.standard.FlagCuttable
import net.im51111n355.buildthing.standard.RemoveAtCallsite
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode

class RemoveAnnotationsProcessor(
    val project: ProcessingProject
): IProcessingStep {
    override fun process() {
        project.processAllClasses { classNode ->
            val modified = classNode.visibleAnnotations
                ?.removeIf {
                    val remove = shouldRemoveAnnotation(it)
                    return@removeIf remove
                }

            return@processAllClasses ProcessingResult.fromIsModified(modified == true)
        }

        project.processAllFields { _, it ->
            val modified = it.visibleAnnotations
                ?.removeIf {
                    val remove = shouldRemoveAnnotation(it)
                    return@removeIf remove
                }

            return@processAllFields ProcessingResult.fromIsModified(modified == true)
        }

        project.processAllMethods { _, it ->
            val modified = it.visibleAnnotations
                ?.removeIf {
                    val remove = shouldRemoveAnnotation(it)
                    return@removeIf remove
                }

            return@processAllMethods ProcessingResult.fromIsModified(modified == true)
        }
    }

    private fun shouldRemoveAnnotation(node: AnnotationNode): Boolean {
        // InjectRandom.XXX удаляются сами
        // FlagCuttable/RemoveAtCallsite удаляются только тут если включено

        return node.type == Type.getType(FlagCuttable::class.java)
                || node.type == Type.getType(RemoveAtCallsite::class.java)
                || node.type == Type.getType(ClassList::class.java)
    }
}