package net.im51111n355.buildthing.dsl

import net.im51111n355.buildthing.task.IBuildThingTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.AbstractArchiveTask

abstract class BuildThingGroovyDsl(
    objects: ObjectFactory,
    private val project: Project
) {
    @JvmOverloads
    fun buildProfile(
        name: String,
        fromTask: Task? = null,
        configure: Action<IBuildThingTask>? = null
    ) {
        project.buildthing {
            if (fromTask == null) {
                this.buildProfile(name) {
                    configure?.execute(this)
                }
            } else {
                this.buildProfile(name, fromTask) {
                    configure?.execute(this)
                }
            }
        }
    }

    fun processJarForExec(
        name: String,
        beforeTask: JavaExec,
        fromTask: AbstractArchiveTask?,
        configure: Action<IBuildThingTask>? = null
    ) {
        project.buildthing {
            if (fromTask == null) {
                this.processJarForExec(name, beforeTask) {
                    configure?.execute(this)
                }
            } else {
                this.processJarForExec(name, beforeTask, fromTask) {
                    configure?.execute(this)
                }
            }
        }
    }

    fun processClassesForExec(
        name: String,
        beforeTask: JavaExec,
        fromTask: AbstractArchiveTask?,
        configure: Action<IBuildThingTask>? = null
    ) {
        project.buildthing {
            if (fromTask == null) {
                this.processClassesForExec(name, beforeTask) {
                    configure?.execute(this)
                }
            } else {
                this.processClassesForExec(name, beforeTask, fromTask) {
                    configure?.execute(this)
                }
            }
        }
    }
}