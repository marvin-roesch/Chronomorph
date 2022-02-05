/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10" // kept in sync with IntelliJ's bundled dep
    groovy
    idea
    id("org.jetbrains.intellij") version "1.3.1"
    id("org.cadixdev.licenser") version "0.6.1"
}

defaultTasks("build")

val CI = System.getenv("CI") != null

val ideaVersion: String by extra
val javaVersion: String by extra
val downloadIdeaSources: String by extra

val compileKotlin by tasks
val processResources: AbstractCopyTask by tasks
val test: Test by tasks
val runIde: JavaExec by tasks
val clean: Delete by tasks

val testLibs by configurations.creating {
    isTransitive = false
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    implementation("org.shredzone.commons:commons-suncalc:3.4")

    // Add tools.jar for the JDI API
    implementation(files(Jvm.current().toolsJar))

    "testLibs"("org.jetbrains.idea:mockJDK:1.7-4d76c50")
}

intellij {
    pluginName.set("Chronomorph")
    version.set(ideaVersion)
    updateSinceUntilBuild.set(false)
    downloadSources.set(!CI && downloadIdeaSources.toBoolean())

    sandboxDir.set(project.rootDir.canonicalPath + "/.sandbox")
}

tasks {
    publishPlugin {
        if (project.properties["publish"] != null) {
            project.version = "${project.version}-${project.properties["buildNumber"]}"
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs = listOf("-proc:none")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }

    withType<GroovyCompile> {
        options.compilerArgs = listOf("-proc:none")
    }
}

processResources {
    for (lang in arrayOf("", "_en")) {
        from("src/main/resources/messages.Chronomorph_en_US.properties") {
            rename { "messages.Chronomorph$lang.properties" }
        }
    }
}

test {
    dependsOn(configurations["testLibs"])
    doFirst {
        configurations["testLibs"].resolvedConfiguration.resolvedArtifacts.forEach {
            systemProperty("testLibs.${it.name}", it.file.absolutePath)
        }
    }
}

idea {
    module {
        excludeDirs.add(file(intellij.sandboxDir))
    }
}

// License header formatting
license {
    setHeader(file("copyright.txt"))
    include("**/*.java", "**/*.kt", "**/*.groovy", "**/*.gradle", "**/*.xml", "**/*.properties", "**/*.html")
}

runIde {
    maxHeapSize = "2G"

    (findProperty("intellijJre") as? String)?.let(this::setExecutable)

    System.getProperty("debug")?.let {
        systemProperty("idea.ProcessCanceledException", "disabled")
        systemProperty("idea.debug.mode", "true")
    }
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)

