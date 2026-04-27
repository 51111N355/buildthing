package net.im51111n355.buildthing.processing.process

import net.im51111n355.buildthing.processing.ProcessingProject
import net.im51111n355.buildthing.processing.ProcessingResult
import net.im51111n355.buildthing.standard.FlagCuttable
import net.im51111n355.buildthing.standard.RemoveAtCallsite
import net.im51111n355.buildthing.util.FlagExpressionEval
import net.im51111n355.buildthing.util.getOptionalAnnotation
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
    val project: ProcessingProject
): IProcessingStep {
    // Владелец Класс -> Его внутренний класс на удаление
    private val classesOwnedByClasses = mutableSetOf<Pair<String, String>>()
    // Владелец МЕТОД -> Внутренний класс на удаление
    private val classesOwnedByMethods = mutableSetOf<Pair<MemberInfo, String>>()
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

    override fun process() {
        classesOwnedByClasses.clear()
        classesOwnedByMethods.clear()
        classesToRemove.clear()
        methodsToRemove.clear()
        methodsToRemoveAtCallsite.clear()
        javaStyleLambdaMethodsToRemove.clear()
        kotlinStyleLambdaMethodsToRemove.clear()
        fieldsToRemove.clear()

        if (project.config.disableCutter)
            return

        // Скан удаляемых классов на outerClass / outerMethod+outerMethodDesc
        project.processAllClasses { classNode ->
            val outerClass = classNode.outerClass
            val outerMethod = classNode.outerMethod
            val outerMethodDesc = classNode.outerMethodDesc

            if (outerClass != null && outerMethod != null && outerMethodDesc != null) {
                val ownerInfo = MemberInfo(outerClass, outerMethod, outerMethodDesc)
                classesOwnedByMethods.add(Pair(ownerInfo, classNode.name))
            } else if (outerClass != null) {
                classesOwnedByClasses.add(Pair(outerClass, classNode.name))
            }

            return@processAllClasses ProcessingResult.NOT_MODIFIED
        }

        // Сначала найти что удалять.
        // Вносит в classesToRemove, methodsToRemove, methodsToRemoveAtCallsite, fieldsToRemove цели для сноса
        project.processAllClasses { classNode ->
            // Проверка на вырезание класса
            if (!isCuttable(classNode.visibleAnnotations))
                return@processAllClasses ProcessingResult.NOT_MODIFIED

            classesToRemove.add(classNode.name)

            // Классы которые относятся именно к этому классу
            classesOwnedByClasses
                .filter { it.first == classNode.name }
                .map { it.second }
                .forEach(classesToRemove::add)

            // Классы которые относятся к методам этого класса тоже
            classesOwnedByMethods
                .filter { it.first.className == classNode.name }
                .map { it.second }
                .forEach(classesToRemove::add)

            return@processAllClasses ProcessingResult.NOT_MODIFIED
        }

        project.processAllMethods { classNode, it ->
            if (!isCuttable(it.visibleAnnotations))
                return@processAllMethods ProcessingResult.NOT_MODIFIED

            val atCallsite = it.visibleAnnotations.getOptionalAnnotation<RemoveAtCallsite>() != null

            val listTo = if (atCallsite)
                methodsToRemoveAtCallsite
            else
                methodsToRemove

            val member = MemberInfo(
                classNode.name,
                it.name,
                it.desc
            )
            listTo.add(member)

            // Классы которые относятся к этому методу тоже
            classesOwnedByMethods
                .filter { it.first == member }
                .map { it.second }
                .forEach(classesToRemove::add)

            return@processAllMethods ProcessingResult.NOT_MODIFIED
        }

        project.processAllFields { classNode, it ->
            if (!isCuttable(it.visibleAnnotations))
                return@processAllFields ProcessingResult.NOT_MODIFIED

            fieldsToRemove.add(MemberInfo(
                classNode.name,
                it.name,
                it.desc
            ))
            return@processAllFields ProcessingResult.NOT_MODIFIED
        }

        // im51111n355 FIXME: Переделать на processAllMethods/processAllFields в одбновлении
        // Скан лямбды на удаление, зависит от ПОЛНОГО прошлого шага
        // Если включено то снос (уже?) не использованных синтетиков, и private static final методов с "$lambda" в названии
        project.processAllClasses { classNode ->
            if (classNode.name in classesToRemove)
                return@processAllClasses ProcessingResult.NOT_MODIFIED

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
                    if (insn is InvokeDynamicInsnNode
                        && insn.bsm.tag == Opcodes.H_INVOKESTATIC
                        && insn.bsm.owner == "java/lang/invoke/LambdaMetafactory"
                        && insn.bsm.name == "metafactory"
                        && insn.bsm.desc == "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                        && !insn.bsm.isInterface) {
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

            return@processAllClasses ProcessingResult.NOT_MODIFIED
        }

        // im51111n355 FIXME: Переделать на processAllMethods/processAllFields в одбновлении
        // Дальше проверить что оно не вызывается из кода который не будет удалён.
        // Ищет если где-то есть доступ к чему-либо что будет удалено, игнорирует использования из других удалённых мест
        var validationIssues = false

        project.processAllClasses { classNode ->
            if (classNode.name in classesToRemove)
                return@processAllClasses ProcessingResult.NOT_MODIFIED

            // Проверить если реализуемые интерфейсы будут удалены
            classNode.interfaces.forEach {
                if (it in classesToRemove) {
                    project.gradleProject.logger.error("Validation error: Class \"${classNode.type.className}\" implements an interface that will be removed! (${it})")
                    validationIssues = true
                }
            }

            // Проверить если родительский класс будет удалён
            if (classNode.superName in classesToRemove) {
                project.gradleProject.logger.error("Validation error: Class \"${classNode.type.className}\" extends a class that will be removed (${classNode.superName}).")
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
                    project.gradleProject.logger.error("Validation error: Field \"${it.name}\" in class \"${classNode.type.className}\" is of type that will be removed (${it.type.internalName}).")
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
                            project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has an argument of type that will be removed (${argType.internalName}).")
                            validationIssues = true
                        }
                    }

                // Тип возврата
                if (Type.getReturnType(it.desc).internalName in classesToRemove) {
                    project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" returns a type that will be removed (${Type.getReturnType(it.desc).internalName}).")
                    validationIssues = true
                }

                // Код
                it.instructions.forEach { insn ->
                    // Вызовы
                    if (insn is MethodInsnNode && insn.owner in classesToRemove) {
                        project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a call to a class that will be removed (${insn.owner}).")
                        validationIssues = true
                    }

                    if (insn is MethodInsnNode) {
                        val info = MemberInfo(
                            insn.owner,
                            insn.name,
                            insn.desc
                        )

                        if (!isMethodMemberGoingToExist(info, false)) {
                            project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a call to a method that will be removed (${info}).")
                            validationIssues = true
                        }
                    }

                    // Поля
                    if (insn is FieldInsnNode && insn.owner in classesToRemove) {
                        project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has access of a field of a type that will be removed (${insn.owner}).")
                        validationIssues = true
                    }

                    if (insn is FieldInsnNode) {
                        val info = MemberInfo(
                            insn.owner,
                            insn.name,
                            insn.desc
                        )

                        if (info in fieldsToRemove) {
                            project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a field access to a field that will be removed (${info}).")
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
                                project.gradleProject.logger.warn("Unknown opcode ${insn.opcode} for TypeInsnNode!")
                                "an operation with"
                            }
                        }

                        project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has $descMsg a class that will be removed (${insn.desc}).")
                        validationIssues = true
                    }
                }

                it.localVariables?.forEach { local ->
                    if (local.desc in classesToRemove) {
                        project.gradleProject.logger.error("Validation error: Method \"${it.name}\" in class \"${classNode.type.className}\" has a local variable \"${local.name}\" of type that will be removed (${local.desc}).")
                        validationIssues = true
                    }
                }
            }

            return@processAllClasses ProcessingResult.NOT_MODIFIED
        }

        if (validationIssues)
            throw GradleException("Validation issues were found!")

        // Дальше примернить основное вырезание
        // Классы
        project.processAllClasses { classNode ->
            return@processAllClasses ProcessingResult.fromIsDeleted(classNode.name in classesToRemove)
        }

        // Методы (Основное вырезание)
        project.processAllMethods { classNode, it ->
            return@processAllMethods ProcessingResult.fromIsDeleted(!isMethodGoingToExist(classNode, it, true))
        }

        project.processAllMethods { classNode, it ->
            var modified = false

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
                    else -> throw AssertionError()
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

            return@processAllMethods ProcessingResult.fromIsModified(modified)
        }

        project.processAllFields { classNode, it ->
            // Поля
            val info = MemberInfo(
                classNode.name,
                it.name,
                it.desc
            )

            return@processAllFields ProcessingResult.fromIsDeleted(info in fieldsToRemove)
        }
    }

    private fun isCuttable(annotations: List<AnnotationNode>?): Boolean {
        if (annotations == null)
            return false

        val annotation = annotations.getOptionalAnnotation<FlagCuttable>()
            ?: return false

        val flag = annotation.getRequiredArgument<String>("value")
        val value = FlagExpressionEval.eval(flag) {
            it in project.config.flags
        }

        return !value // <- value - есть ли флаг, а удаление если флага нет !!!
    }

    private data class MemberInfo(
        val className: String,
        val name: String,
        val desc: String
    )

    private fun isMethodGoingToExist(classNode: ClassNode, method: MethodNode, includeCallsiteRemoval: Boolean = true): Boolean {
        val info = MemberInfo(
            classNode.name,
            method.name,
            method.desc
        )

        return isMethodMemberGoingToExist(info, includeCallsiteRemoval)
    }

    private fun isMethodMemberGoingToExist(
        info: MemberInfo,
        // Из валидации не нужно писать ошибки на RemoveAtCallsite методы
        includeCallsiteRemoval: Boolean = true
    ): Boolean {
        val removal = info in methodsToRemove
                || (info in methodsToRemoveAtCallsite && includeCallsiteRemoval)
                || (info in javaStyleLambdaMethodsToRemove && project.config.deleteJavaStyleLambdas)
                || (info in kotlinStyleLambdaMethodsToRemove && project.config.deleteKotlinStyleLambdas)

        return !removal
    }
}