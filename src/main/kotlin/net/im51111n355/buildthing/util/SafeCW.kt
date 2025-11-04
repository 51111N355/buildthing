package net.im51111n355.buildthing.util

import net.im51111n355.buildthing.processing.BuildThingProcessor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

class ClassPathIndex(
    val master: BuildThingProcessor
) {
    // Изначально загружаются только строчки и то в каком файле они есть
    private val notLoadedClasses = mutableMapOf<String, File>()
    // Тут классы которые уже были нужны хотябы 1
    private val loadedClasses = mutableMapOf<String, ClassNode>()

    // Индексация всего что на compileClasspath
    fun index(file: File) {
        synchronized(this) {
            if (file.extension == "jar") {
                ZipFile(file).use {
                    val entries = it.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue
                        if (!entry.name.endsWith(".class")) continue

                        // <Internal Name Класса>.class + файл где его найти
                        notLoadedClasses.put(entry.name, file)
                    }
                }
            } else if (file.isDirectory) {
                // Что-то типо `implementation project(":submodule")` будет ссылаться на классы (?) не знаю, пусть будет.
                file.walkTopDown().forEach { childFile ->
                    val relPath = childFile.relativeTo(file)
                    if (childFile.extension == "class")
                        notLoadedClasses.put(relPath.invariantSeparatorsPath, childFile)
                }
            }
        }
    }

    // Индексация самого проекта который билдим будет вызывать,
    // а ещё из под загрузки классов вызывается тоже
    fun index(bytes: ByteArray) {
        synchronized(this) {
            val classNode = ClassNode()
            ClassReader(bytes).accept(classNode, 0)

            loadedClasses.put(classNode.name, classNode)
        }
    }

    fun findClass(internalName: String): ClassNode {
        synchronized(this) {
            val loadedClass = loadedClasses[internalName]
            if (loadedClass != null) return loadedClass

            val classFileName = "$internalName.class"


            val bytes = if (classFileName in notLoadedClasses) {
                // Попытка загрузить из скана ClassPath
                val from = notLoadedClasses.remove(classFileName)!!

                when (from.extension) {
                    "jar", "zip" ->
                        ZipFile(from).use {
                            val entry = it.getEntry(classFileName)
                            it.getInputStream(entry).use(InputStream::readBytes)
                        }
                    "class" -> from.readBytes()
                    else -> throw AssertionError()
                }
            } else {
                // Попробовать загрузить из ресурсов доступных тут, например java/lang/Object и подобные
                javaClass.classLoader.getResourceAsStream(classFileName)!!
                    .use(InputStream::readBytes)
            }

            index(bytes)
            return loadedClasses[internalName]!!
        }
    }
}

class SafeCW(
    flags: Int,
    val index: ClassPathIndex
) : ClassWriter(flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        if (isClass1AssignableFrom2(type1, type2))
            return type1
        if (isClass1AssignableFrom2(type2, type1))
            return type2

        var cl1 = index.findClass(type1)
        val cl1iface = cl1.access and Opcodes.ACC_INTERFACE != 0

        val cl2 = index.findClass(type2)
        val cl2iface = cl2.access and Opcodes.ACC_INTERFACE != 0

        if (cl1iface || cl2iface)
            return "java/lang/Object"

        do {
            cl1 = index.findClass(cl1.superName)
        } while (!isClass1AssignableFrom2(cl1.name, cl2.name))

        return cl1.name
    }

    private fun isClass1AssignableFrom2(type1: String, type2: String): Boolean {
        if (type1 == type2)
            return true

        val t2 = index.findClass(type2) // returns ASM ClassNode

        val allT2ParentLayers = mutableListOf<String>()
        val indexingQueue = ArrayDeque<String>()
        t2.superName?.let(indexingQueue::add)
        t2.superName?.let(allT2ParentLayers::add)
        indexingQueue.addAll(t2.interfaces)
        allT2ParentLayers.addAll(t2.interfaces)

        while (!indexingQueue.isEmpty()) {
            val toIndex = indexingQueue.removeFirst()
            val cTI = index.findClass(toIndex)
            cTI.superName?.let(indexingQueue::add)
            cTI.superName?.let(allT2ParentLayers::add)
            indexingQueue.addAll(cTI.interfaces)
            allT2ParentLayers.addAll(cTI.interfaces)
        }

        return type1 in allT2ParentLayers
    }
}