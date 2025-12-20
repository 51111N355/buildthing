package net.im51111n355.buildthing

import net.im51111n355.buildthing.dsl.BuildThingGroovyDsl
import net.im51111n355.buildthing.dsl.buildthing
import net.im51111n355.buildthing.task.build.BuildThingJarTask
import net.im51111n355.buildthing.task.devruntime.BuildThingProcessInPlaceTask
import net.im51111n355.buildthing.util.sha256
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import java.io.File
import java.io.InputStream
import java.util.concurrent.Callable

class BuildThingPlugin : Plugin<Project> {
    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project

        injectStd()
        setupGroovyDsl()
    }

    private fun setupGroovyDsl() {
        val extension = project.extensions.create("buildthing", BuildThingGroovyDsl::class.java)

        project.afterEvaluate {
            buildthing {
                for (creation in extension.profiles) {
                    val (name, from, configure) = creation

                    if (from == null) {
                        buildProfile(name, configure=configure::accept)
                    } else {
                        buildProfile(name, from, configure::accept)
                    }
                }

                for (creation in extension.jarBeforeTask) {
                    val (name, before, from, configure) = creation

                    if (from == null) {
                        processJarBeforeTask(name, before, configure=configure::accept)
                    } else {
                        processJarBeforeTask(name, before, from, configure::accept)
                    }
                }

                for (creation in extension.classesBeforeTask) {
                    val (name, before, from, configure) = creation

                    if (from == null) {
                        processClassesBeforeTask(name, before, configure=configure::accept)
                    } else {
                        processClassesBeforeTask(name, before, from, configure::accept)
                    }
                }
            }
        }
    }

    // Добавляет standard.jar в зависимости, обрабатывает то что может быть несколько версий плагина с разными standard.jar (привязка к sha256)
    private fun injectStd() {
        val classLoader = javaClass.classLoader

        val bytes = classLoader.getResourceAsStream("standard.jar")!!
            .use(InputStream::readBytes)
        val hash = bytes.sha256()

        val gradleUserHome = project.gradle.gradleUserHomeDir
        val cacheDir = File(gradleUserHome, "caches/buildthing-std")
            .also(File::mkdirs)
        val standardJar = File(cacheDir, "buildthing-std-$hash.jar")

        if (!standardJar.exists())
            standardJar.writeBytes(bytes)

        // FIXME: Я точно помню в 1.21 neoforge проекте с похожим кодом у Gradle плагина - были особенности...
        project.afterEvaluate {
            project.dependencies {
                // FIXME: Если пользователи используют maven publish то я не знаю, не получится ли что standard добавится в pom.xml.
                // FIXME: Может быть это не произойдет потому std - файл, но я не уверен, проверить бы.
                add("implementation", project.files(standardJar))
            }
        }
    }

    @JvmOverloads
    fun sideTask(
        name: String,
        fromTask: DefaultTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingJarTask.() -> Unit
    ) {
        project.logger.error("!! You are using BuildThingPlugin#sideTask which is deprecated! Please use building { buildProfile(...) { ... } }. See docs for more info")
        project.logger.error("!! Вы используете BuildThingPlugin#sideTask что не рекомендуется! Используйте { buildProfile(...) { ... } }. См. документацию за дополнительной информацией.")

        project.tasks.create<BuildThingJarTask>("build$name") {
            description  = "Build $name"
            group = "buildthing build profile"
            archiveClassifier.set("[$name]")

            dependsOn(fromTask)

            from(Callable {
                fromTask
                    .outputs.files
                    .map { project.zipTree(it) }
            })

            configure()
        }
    }

    @JvmOverloads
    fun processClassesBeforeTask(
        name: String,
        beforeTask: DefaultTask,

        fromTask: DefaultTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingProcessInPlaceTask.() -> Unit
    ) {
        project.logger.error("!! You are using BuildThingPlugin#processClassesBeforeTask which is deprecated! Please use building { processClassesBeforeTask(...) { ... } }. See docs for more info")
        project.logger.error("!! Вы используете BuildThingPlugin#processClassesBeforeTask что не рекомендуется! Используйте { processClassesBeforeTask(...) { ... } }. См. документацию за дополнительной информацией.")

        project.buildthing {
            processClassesBeforeTask(name, beforeTask, fromTask, configure)
        }
    }

    @JvmOverloads
    fun processJarBeforeTask(
        name: String,
        beforeTask: DefaultTask,

        fromTask: AbstractArchiveTask = project.tasks.named<Jar>("jar").get(),
        configure: BuildThingJarTask.() -> Unit
    ) {
        project.logger.error("!! You are using BuildThingPlugin#processJarBeforeTask which is deprecated! Please use building { processJarBeforeTask(...) { ... } }. See docs for more info")
        project.logger.error("!! Вы используете BuildThingPlugin#processJarBeforeTask что не рекомендуется! Используйте { processJarBeforeTask(...) { ... } }. См. документацию за дополнительной информацией.")

        project.buildthing {
            processJarBeforeTask(name, beforeTask, fromTask, configure)
        }
    }
}