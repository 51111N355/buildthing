package net.im51111n355.buildthing.processing.process.inject

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.processing.IProcessingStep
import net.im51111n355.buildthing.standard.ClassList
import net.im51111n355.buildthing.util.getConstantPushedValue
import net.im51111n355.buildthing.util.getOptionalAnnotation
import net.im51111n355.buildthing.util.getRequiredArgument
import net.im51111n355.buildthing.util.type
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode

class InjectClassListProcessor(
    val master: BuildThingProcessor
) : IProcessingStep {
    // Key -> List<Class Name>
    private val classMap = mutableMapOf<String, MutableList<String>>()

    override fun process() {
        // 1 - Индексация классов с нужной аннотацией @ClassList
        master.processAll { classNode ->
            val annotation = classNode.visibleAnnotations?.getOptionalAnnotation<ClassList>()
                ?: return@processAll ProcessAllAction.NOT_MODIFIED

            val keys = annotation.getRequiredArgument<List<String>>("value")
            for (key in keys) {
                classMap.computeIfAbsent(key) { mutableListOf() }
                    .add(classNode.name)
            }

            return@processAll ProcessAllAction.NOT_MODIFIED
        }

        var errors = false

        // 2 - Замена INVOKESTATIC Inject.classList(String)
        master.processAll { classNode ->
            var modified = false

            classNode.methods.forEach {
                var i = 0

                while (i < it.instructions.size()) {
                    val insn = it.instructions[i]

                    if(insn !is MethodInsnNode) {
                        i++
                        continue
                    }
                    if (insn.opcode != Opcodes.INVOKESTATIC) {
                        i++
                        continue
                    }

                    if (insn.owner != "net/im51111n355/buildthing/standard/Inject"
                        || insn.desc != "(Ljava/lang/String;)Ljava/util/List;"
                        || insn.name != "classList") {
                        i++
                        continue
                    }

                    val expectedTypes = Type.getArgumentTypes(insn.desc)
                    var dontInject = false
                    val nArgs = expectedTypes.size

                    for (n in 0 until nArgs) {
                        val expectedType = expectedTypes[expectedTypes.size - n - 1]
                        val expetedLdc = i - (n + 1)
                        val hopefullyLdc = it.instructions[expetedLdc]
                        val value = hopefullyLdc.getConstantPushedValue()

                        if (value == null) {
                            errors = true
                            dontInject = true
                            master.project.logger.error("Expected constant to be passed to a build-time-evaluated method Inject.classList. In method \"${it.name}\" of class \"${classNode.type.className}\".")
                            break
                        }
                    }
                    // Скип если были ошибки
                    if (dontInject) {
                        i++
                        continue
                    }

                    modified = true

                    val removedLdcs = mutableListOf<Any?>()

                    for (n in 0 until nArgs) {
                        val expectedType = expectedTypes[expectedTypes.size - n - 1]
                        val expectedLdc = i - (n + 1)
                        val hopefullyLdc = it.instructions[expectedLdc]

                        val value = hopefullyLdc.getConstantPushedValue()
                            ?: throw AssertionError()

                        removedLdcs.add(0, value.t)
                        it.instructions.remove(hopefullyLdc)
                    }

                    val newValue = classMap[removedLdcs[0]]
                        ?: emptyList()

                    println("======================")
                    println(removedLdcs[0])
                    println(newValue)

                    // Создать нужный List
                    val wInsnList = InsnList()
                    var lastAddedInsn: AbstractInsnNode
                    wInsnList.add(TypeInsnNode(Opcodes.NEW, "java/util/ArrayList"))
                    wInsnList.add(InsnNode(Opcodes.DUP))

                    lastAddedInsn = MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false)
                    wInsnList.add(lastAddedInsn)

                    for (clazz in newValue) {
                        wInsnList.add(InsnNode(Opcodes.DUP))
                        wInsnList.add(LdcInsnNode(Type.getType("L$clazz;")))
                        wInsnList.add(MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true))

                        lastAddedInsn = InsnNode(Opcodes.POP)
                        wInsnList.add(lastAddedInsn)
                    }

                    it.instructions.insert(insn, wInsnList)
                    it.instructions.remove(insn)
                    i = it.instructions.indexOf(lastAddedInsn)
                    i++
                }
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }
    }
}