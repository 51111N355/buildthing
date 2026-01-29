package net.im51111n355.buildthing.dsl

import groovy.lang.Closure
import net.im51111n355.buildthing.config.BuildThingConfig
import net.im51111n355.buildthing.task.IBuildThingTask
import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessInPlaceTask
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.util.function.Consumer

abstract class BuildThingGroovyDsl(objects: ObjectFactory) {
    internal val profiles = mutableListOf<BuildProfileCreation>()
    internal val jarBeforeTask = mutableListOf<JarBeforeTaskCreation>()
    internal val classesBeforeTask = mutableListOf<ClassesBeforeTaskCreation>()
    internal val jarJavaExecs = mutableListOf<JavaExecCreation>()
    internal val classesJavaExecs = mutableListOf<JavaExecCreation>()

    @JvmOverloads
    fun buildProfile(
        name: String,
        fromTask: Task? = null,
        configure: Action<IBuildThingTask>? = null
    ) {
        profiles.add(BuildProfileCreation(
            name,
            fromTask
        ) {
            configure?.execute(it)
        })
    }

    @JvmOverloads
    fun processClassesBeforeTask(
        name: String,
        beforeTask: Task,

        fromTask: Task?,
        configure: Action<IBuildThingTask>? = null
    ) {
        classesBeforeTask.add(ClassesBeforeTaskCreation(
            name,
            beforeTask,
            fromTask
        ) {
            configure?.execute(it)
        })
    }

    @JvmOverloads
    fun processJarBeforeTask(
        name: String,
        beforeTask: Task,

        fromTask: AbstractArchiveTask?,
        configure: Action<IBuildThingTask>? = null
    ) {
        jarBeforeTask.add(JarBeforeTaskCreation(
            name,
            beforeTask,
            fromTask
        ) {
            configure?.execute(it)
        })
    }

    fun processJarForExec(
        name: String,
        beforeTask: JavaExec,
        fromTask: AbstractArchiveTask?,
        configure: Action<IBuildThingTask>? = null
    ) {
        jarJavaExecs.add(JavaExecCreation(
            name, beforeTask, fromTask
        ) {
            configure?.execute(it)
        })
    }

    fun processClassesForExec(
        name: String,
        beforeTask: JavaExec,
        fromTask: AbstractArchiveTask?,
        configure: Action<IBuildThingTask>? = null
    ) {
        classesJavaExecs.add(JavaExecCreation(
            name, beforeTask, fromTask
        ) {
            configure?.execute(it)
        })
    }

    internal data class BuildProfileCreation(
        val name: String,
        val fromTask: Task?,
        val configure: Consumer<IBuildThingTask>
    )

    internal data class ClassesBeforeTaskCreation(
        val name: String,
        val beforeTask: Task,
        val fromTask: Task?,
        val configure: Consumer<IBuildThingTask>
    )

    internal data class JarBeforeTaskCreation(
        val name: String,
        val beforeTask: Task,
        val fromTask: AbstractArchiveTask?,
        val configure: Consumer<IBuildThingTask>
    )

    internal data class JavaExecCreation(
        val name: String,
        val beforeTask: JavaExec,
        val archiveTask: AbstractArchiveTask?,
        val configure: Consumer<IBuildThingTask>
    )
}