plugins {
    `kotlin-dsl`
    id("idea")
    id("maven-publish")
}

group = "net.im51111n355.buildthing"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":standard"))
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-util:9.7")
}

gradlePlugin {
    plugins {
        create("im51111n355-buildthing") {
            id = "net.im51111n355.buildthing"
            implementationClass = "net.im51111n355.buildthing.BuildThingPlugin"
            displayName = "The BuildThing Gradle plugin."
            description = "A gradle plugin implementing build-time utilities (things)."
        }
    }
}

tasks.register<Copy>("bundleStandardJar") {
    dependsOn(":standard:jar")

    from(
        project(":standard")
        .tasks
        .named<Jar>("jar")
        .get()
        .archiveFile
    )
    into("$buildDir/bundle")
}

tasks.named<Jar>("jar") {
    dependsOn(":bundleStandardJar")
    from("$buildDir/bundle/standard.jar")

    // Аннотации нужны запакованные для Type.getType(...),
    // мне лень разбираться с maven local для этой штуки поэтому я запакую.
    from(zipTree("$buildDir/bundle/standard.jar"))
}

// Чтобы само подтянуло ASM, Kotlin (?), и ещё что нужно
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}