package net.im51111n355.buildthing.util

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

val ClassNode.type: Type
    get() = Type.getObjectType(name)

val FieldNode.type: Type
    get() = Type.getType(desc)

val AnnotationNode.type: Type
    get() = Type.getType(desc)

val MethodNode.isSynthetic: Boolean
    get() = (access and Opcodes.ACC_SYNTHETIC) != 0

val MethodNode.isPrivate: Boolean
    get() = (access and Opcodes.ACC_PRIVATE) != 0

val MethodNode.isStatic: Boolean
    get() = (access and Opcodes.ACC_STATIC) != 0

val MethodNode.isFinal: Boolean
    get() = (access and Opcodes.ACC_FINAL) != 0

inline fun <reified T> AnnotationNode.getOptionalArgument(name: String, default: T?): T {
    if (values == null) {
        // Вообще без аргументов null, но да бывает и такое
        return default!!
    }

    val args = values.size / 2
    for (i in 0 until args) {
        val key = values[i * 2]
        val value = values[i * 2 + 1]

        if (key == name) {
            if (value !is T) {
                throw IllegalArgumentException("Found key \"$name\" but its of a different type! Expected ${T::class.java.simpleName} but got ${value.javaClass.simpleName}.")
            }

            return value
        }
    }

    return default!!
}

inline fun <reified T> AnnotationNode.getRequiredArgument(name: String): T {
    val value = getOptionalArgument<T>(name, null)
    if (value != null) return value

    throw IllegalArgumentException("Key \"$name\" was not found!")
}

inline fun <reified T> List<AnnotationNode>.getOptionalAnnotation(): AnnotationNode? {
    val t = Type.getType(T::class.java)

    for (node in this) {
        if (node.type == t)
            return node
    }

    return null
}