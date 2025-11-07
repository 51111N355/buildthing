package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.BuildThingProcessor
import net.im51111n355.buildthing.processing.BuildThingProcessor.ProcessAllAction
import net.im51111n355.buildthing.standard.FlagCuttable
import net.im51111n355.buildthing.standard.RemoveAtCallsite
import net.im51111n355.buildthing.util.getOptionalAnnotation
import net.im51111n355.buildthing.util.getOptionalArgument
import net.im51111n355.buildthing.util.getRequiredArgument
import net.im51111n355.buildthing.util.isFinal
import net.im51111n355.buildthing.util.isPrivate
import net.im51111n355.buildthing.util.isStatic
import net.im51111n355.buildthing.util.isSynthetic
import net.im51111n355.buildthing.util.type
import org.gradle.api.GradleException
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class FlagCuttingProcessor(
    val master: BuildThingProcessor
) {
    // Классы на удаление
    private val classesToRemove = mutableSetOf<String>()
    // Методы на удаление
    private val methodsToRemove = mutableSetOf<MemberInfo>()
    // Методы в которых удаление вызовов этих самых методов
    private val methodsToRemoveAtCallsite = mutableSetOf<MemberInfo>()
    // Лямбды в стиле Java которые стоит удалить (Если включено)
    private val javaStyleLambdaMethodsToRemove = mutableSetOf<MemberInfo>()
    // Лямбды в стиле Kotlin которые стоит удалить (Если включено)
    private val kotlinStyleLambdaMethodsToRemove = mutableSetOf<MemberInfo>()
    // Поля на удаление
    private val fieldsToRemove = mutableSetOf<MemberInfo>()

    fun process() {
        // Сначала найти что удалять.
        // Вносит в classesToRemove, methodsToRemove, methodsToRemoveAtCallsite, fieldsToRemove цели для сноса
        master.processAll { classNode ->
            // Проверка на вырезание класса
            if (isCuttable(classNode.visibleAnnotations)) {
                classesToRemove.add(classNode.name)
                return@processAll ProcessAllAction.NOT_MODIFIED
            }

            // Проверки у методов
            classNode.methods
                .filter { isCuttable(it.visibleAnnotations) }
                .forEach {
                    val atCallsite = it.visibleAnnotations.getOptionalAnnotation<RemoveAtCallsite>() != null

                    val listTo = if (atCallsite)
                        methodsToRemoveAtCallsite
                    else
                        methodsToRemove

                    listTo.add(MemberInfo(
                        classNode.name,
                        it.name,
                        it.desc
                    ))
                }

            // Проверка на удаление полей
            classNode.fields
                .filter { isCuttable(it.visibleAnnotations) }
                .forEach {
                    fieldsToRemove.add(MemberInfo(
                        classNode.name,
                        it.name,
                        it.desc
                    ))
                }

            return@processAll ProcessAllAction.NOT_MODIFIED
        }

        // Информация
        master.project.logger.info("BuildThing scan pass:")
        master.project.logger.info("  \\ ${classesToRemove.size} Classes marked for removal")
        master.project.logger.info("  \\ ${methodsToRemove.size} Methods marked for removal")
        master.project.logger.info("  \\ ${fieldsToRemove.size} Fields marked for removal")

        // Скан лямбды на удаление, зависит от ПОЛНОГО прошлого шага
        // Если включено то снос (уже?) не использованных синтетиков, и private static final методов с "$lambda" в названии
        master.processAll { classNode ->
            if (classNode.name in classesToRemove)
                return@processAll ProcessAllAction.NOT_MODIFIED

            // Кандидаты на снос
            val javaStyleCandidates = classNode.methods
                .filter { it.isPrivate && it.isSynthetic && it.name.contains("lambda$") }
                .map { MemberInfo(classNode.name, it.name, it.desc) }
                .toMutableList()
            val kotlinStyleCandidates = classNode.methods
                .filter { it.isPrivate && it.isStatic && it.isFinal && !it.isSynthetic && it.name.contains("\$lambda") }
                .map { MemberInfo(classNode.name, it.name, it.desc) }
                .toMutableList()

            // Поиск вызовов этих методов, если используются то отмена сноса
            classNode.methods.forEach {
                val info = MemberInfo(
                    classNode.name,
                    it.name,
                    it.desc
                )

                if (info in methodsToRemove || info in methodsToRemoveAtCallsite)
                    // Не считаем вызовы тут
                    return@forEach

                for (insn in it.instructions) {
                    if (insn is InvokeDynamicInsnNode) {
                        val target = insn.bsmArgs[1] as Handle

                        val info = MemberInfo(
                            target.owner,
                            target.name,
                            target.desc
                        )

                        // Если след. инструкция это methodsToRemoveAtCallsite метод - то эта invokedynamic будет вырезана
                        var next = insn.next

                        // Котлин любит генерировать эти инструкции, а в Java ни разу не попадались
                        while (next is LabelNode || next is LineNumberNode) {
                            next = next.next
                        }

                        if (next is MethodInsnNode) {
                            val info = MemberInfo(
                                next.owner,
                                next.name,
                                next.desc
                            )

                            if (info in methodsToRemoveAtCallsite)
                                continue
                        }

                        // Если реально вызов кандидата то оставляем
                        javaStyleCandidates.remove(info)
                        kotlinStyleCandidates.remove(info)
                    }
                }
            }

            javaStyleLambdaMethodsToRemove.addAll(javaStyleCandidates)
            kotlinStyleLambdaMethodsToRemove.addAll(kotlinStyleCandidates)

            return@processAll ProcessAllAction.NOT_MODIFIED
        }

        // Информация
        master.project.logger.info("BuildThing lambda scan pass:")
        master.project.logger.info("  \\ ${javaStyleLambdaMethodsToRemove.size} Java Style Lambda Implementation marked for removal")
        master.project.logger.info("  \\ ${kotlinStyleLambdaMethodsToRemove.size} Kotlin Style Lambda Implementation marked for removal")

        // Дальше проверить что оно не вызывается из кода который не будет удалён.
        // Ищет если где-то есть доступ к чему-либо что будет удалено, игнорирует использования из других удалённых мест
        var validationIssues = false

        master.processAll { classNode ->
            if (classNode.name in classesToRemove)
                return@processAll ProcessAllAction.NOT_MODIFIED

            // Проверить если реализуемые интерфейсы будут удалены
            classNode.interfaces.forEach {
                if (it in classesToRemove) {
                    master.project.logger.error("Validation error: Class \"${classNode.type.className}\" implements an interface that will be removed! (${it})")
                    validationIssues = true
                }
            }

            // Проверить если родительский класс будет удалён
            if (classNode.superName in classesToRemove) {
                master.project.logger.error("Validation error: Class \"${classNode.type.className}\" extends a class that will be removed (${classNode.superName}).")
                validationIssues = true
            }

            // Проверить если типы полей будут удалены
            classNode.fields.forEach {
                val info = MemberInfo(
                    classNode.name,
                    it.name,
                    it.desc
                )

                if (info in fieldsToRemove)
                    return@forEach

                if (it.type.internalName in classesToRemove) {
                    master.project.logger.error("Validation error: Field \"${it.name}\" in class \"${classNode.type.className}\" is of type that will be removed (${it.type.internalName}).")
                    validationIssues = true
                }
            }

            // Проверка методов (Аргументы/Тип возврата/Код)
            classNode.methods.forEach {
                if (!isMethodGoingToExist(classNode, it))
                    return@forEach

                // Аргументы
                Type.getMethodType(it.desc)
                    .argumentTypes
                    .forEach { argType ->
                        if (argType.internalName in classesToRemove) {
                            master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has an argument of type that will be removed (${argType.internalName}).")
                            validationIssues = true
                        }
                    }

                // Тип возврата
                if (Type.getReturnType(it.desc).internalName in classesToRemove) {
                    master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" returns a type that will be removed (${Type.getReturnType(it.desc).internalName}).")
                    validationIssues = true
                }

                // Код
                it.instructions.forEach { insn ->
                    // Вызовы
                    if (insn is MethodInsnNode && insn.owner in classesToRemove) {
                        master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a call to a class that will be removed (${insn.owner}).")
                        validationIssues = true
                    }

                    if (insn is MethodInsnNode) {
                        val info = MemberInfo(
                            insn.owner,
                            insn.name,
                            insn.desc
                        )

                        if (info in methodsToRemove || info in javaStyleLambdaMethodsToRemove || info in kotlinStyleLambdaMethodsToRemove) {
                            master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a call to a method that will be removed (${info}).")
                            validationIssues = true
                        }
                    }

                    // Поля
                    if (insn is FieldInsnNode && insn.owner in classesToRemove) {
                        master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has access of a field of a type that will be removed (${insn.owner}).")
                        validationIssues = true
                    }

                    if (insn is FieldInsnNode) {
                        val info = MemberInfo(
                            insn.owner,
                            insn.name,
                            insn.desc
                        )

                        if (info in fieldsToRemove) {
                            master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a field access to a field that will be removed (${info}).")
                            validationIssues = true
                        }
                    }

                    // Конструктор, каст, instanceof, создание массива
                    if (insn is TypeInsnNode && insn.desc in classesToRemove) {
                        val descMsg = when (insn.opcode) {
                            Opcodes.NEW -> "an instantiation of"
                            Opcodes.ANEWARRAY -> "an array creation of"
                            Opcodes.CHECKCAST -> "a cast to"
                            Opcodes.INSTANCEOF -> "an instanceof check for"
                            else -> {
                                master.project.logger.warn("Unknown opcode ${insn.opcode} for TypeInsnNode!")
                                "an operation with"
                            }
                        }

                        master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has $descMsg a class that will be removed (${insn.desc}).")
                        validationIssues = true
                    }
                }

                it.localVariables?.forEach { local ->
                    if (local.desc in classesToRemove) {
                        master.project.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a local variable \"${local.name}\" of type that will be removed (${local.desc}).")
                        validationIssues = true
                    }
                }
            }

            return@processAll ProcessAllAction.NOT_MODIFIED
        }

        if (validationIssues)
            throw GradleException("Validation issues were found!")

        // Дальше примернить основное вырезание
        master.processAll { classNode ->
            if (classNode.name in classesToRemove)
                return@processAll ProcessAllAction.DELETE

            var modified = false

            // Методы
            classNode.methods
                .removeIf {
                    val info = MemberInfo(
                        classNode.name,
                        it.name,
                        it.desc
                    )

                    val shouldRemove = info in methodsToRemove || info in methodsToRemoveAtCallsite
                    if (shouldRemove) modified = true

                    return@removeIf shouldRemove
                }

            // Снос вызовов RemoveAtCallsite методов
            classNode.methods.forEach {
                // Собрать инструкции на удаление
                val toRemove = mutableListOf<MethodInsnNode>()

                for (insn in it.instructions) {
                    if (insn is MethodInsnNode) {
                        val info = MemberInfo(
                            insn.owner,
                            insn.name,
                            insn.desc
                        )

                        if (info !in methodsToRemoveAtCallsite)
                            continue

                        toRemove.add(insn)
                    }
                }

                if (!toRemove.isEmpty())
                    modified = true

                // Снос! Ура!
                for (node in toRemove) {
                    val argTypes = Type.getArgumentTypes(node.desc)
                    val returntype = Type.getReturnType(node.desc)

                    val isStatic = node.opcode == Opcodes.INVOKESTATIC

                    // pop'ы на удаление загруженных push
                    val pops = InsnList()

                    for (arg in argTypes.reversed()) {
                        if (arg.size == 2)
                            pops.add(InsnNode(Opcodes.POP2))
                        else
                            pops.add(InsnNode(Opcodes.POP))
                    }

                    // "this"
                    if (!isStatic)
                        pops.add(InsnNode(Opcodes.POP))

                    // push на Возвращаемое значение по умолчанию. null для объектов, 0 для примитивов
                    val defaultPush = InsnList()
                    when (returntype.sort) {
                        Type.VOID -> {} // Ничего
                        Type.OBJECT, Type.ARRAY -> defaultPush.add(InsnNode(Opcodes.ACONST_NULL)) // null
                        Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> defaultPush.add(InsnNode(Opcodes.ICONST_0)) // 0
                        Type.FLOAT -> defaultPush.add(InsnNode(Opcodes.FCONST_0)) // 0.0F
                        Type.LONG -> defaultPush.add(InsnNode(Opcodes.LCONST_0)) // 0L
                        Type.DOUBLE -> defaultPush.add(InsnNode(Opcodes.DCONST_0)) // 0.0D
                        else -> AssertionError()
                    }

                    // Если прошлая insn - INVOKEDYNAMIC то ТАК УЖ И БЫТЬ снесу первый POP и ту инструкцию
                    // Это чтобы удалять лямбду которая оказалась первым параметром этой функции (Кто-то делает Invoke.ifServer(...) штучки в Java)
                    var prev = node.previous

                    // Котлин любит генерировать эти инструкции, а в Java ни разу не попадались
                    while (prev is LabelNode || prev is LineNumberNode) {
                        prev = prev.previous
                    }

                    if (prev is InvokeDynamicInsnNode) {
                        it.instructions.remove(prev)
                        pops.remove(pops.first)
                    }

                    // Удаление, Вставка оставшихся pop, Вставка push дефолта
                    val insertBefore = node.next
                    it.instructions.remove(node)
                    it.instructions.insertBefore(insertBefore, pops)
                    it.instructions.insertBefore(insertBefore, defaultPush)
                }
            }

            // Поля
            classNode.fields
                .removeIf {
                    val info = MemberInfo(
                        classNode.name,
                        it.name,
                        it.desc
                    )

                    val shouldRemove = info in fieldsToRemove
                    if (shouldRemove) modified = true

                    return@removeIf shouldRemove
                }

            // Лямбды если включены
            if (master.config.deleteJavaStyleLambdas) {
                classNode.methods
                    .removeIf {
                        val info = MemberInfo(
                            classNode.name,
                            it.name,
                            it.desc
                        )

                        modified = true
                        return@removeIf info in javaStyleLambdaMethodsToRemove
                    }
            }

            if (master.config.deleteKotlinStyleLambdas) {
                classNode.methods
                    .removeIf {
                        val info = MemberInfo(
                            classNode.name,
                            it.name,
                            it.desc
                        )

                        modified = true
                        return@removeIf info in kotlinStyleLambdaMethodsToRemove
                    }
            }

            return@processAll if (modified)
                ProcessAllAction.MODIFIED
            else
                ProcessAllAction.NOT_MODIFIED
        }
    }

    private fun isCuttable(annotations: List<AnnotationNode>?): Boolean {
        if (annotations == null)
            return false

        val annotation = annotations.getOptionalAnnotation<FlagCuttable>()
            ?: return false

        val flag = annotation.getRequiredArgument<String>("value")
        val presenceToCut = annotation.getOptionalArgument<Boolean>("isPresent", false)

        return master.config.flags.contains(flag) == presenceToCut
    }

    private data class MemberInfo(
        val className: String,
        val name: String,
        val desc: String
    )

    private fun isMethodGoingToExist(classNode: ClassNode, method: MethodNode): Boolean {
        val info = MemberInfo(
            classNode.name,
            method.name,
            method.desc
        )

        val removal = info in methodsToRemove
                || info in methodsToRemoveAtCallsite
                || info in javaStyleLambdaMethodsToRemove
                || info in kotlinStyleLambdaMethodsToRemove

        return !removal
    }
}