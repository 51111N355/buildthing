package net.im51111n355.buildthing.processing

import net.im51111n355.buildthing.util.type
import org.gradle.api.GradleException
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

data class ProcessingCrashException(
    val inClass: ProcessingClassNode?,
    val inMember: MemberInfo?,
//    val instruction: AbstractInsnNode?,
    val causedByException: Exception
) : GradleException(makeMessage(inClass, inMember), causedByException) {
    companion object {
        private fun makeMessage(inClass: ProcessingClassNode?, inMember: MemberInfo?): String {
            var message = "BT Processing Crash"

            if (inClass != null) {
                message += " in class ${inClass.node.type.className}"
            }

            if (inMember != null) {
                message += " in member ${inMember.owner}.${inMember.name}${inMember.desc}"
            }

            val causeMethod = inClass?.node?.methods?.find {
                val sameDesc = it.desc == inMember?.desc
                val sameName = it.name == inMember?.name
                val sameOwner = inClass.node.name == inMember?.owner
                return@find sameDesc && sameName && sameOwner
            }

            if (causeMethod != null) {
                val textifier = Textifier()
                val tmv = TraceMethodVisitor(textifier)

                causeMethod.accept(tmv)

                val sw = StringWriter()
                val pw = PrintWriter(sw)
                textifier.print(pw)
                pw.flush()

                message += ".\n"
                message += "Causing method bytecode at the time of the crash:\n"
                message += sw.toString()
            }

            return message
        }
    }

    override fun toString(): String {
        return message ?: ""
    }
}