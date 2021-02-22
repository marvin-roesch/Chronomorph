/*
 * Chronomorph for IntelliJ
 *
 * Copyright (c) 2018 PaleoCrafter
 *
 * MIT License
 */

import org.gradle.internal.jvm.Jvm
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.31" // kept in sync with IntelliJ's bundled dep
    groovy
    idea
    id("org.jetbrains.intellij") version "0.7.0"
    id("net.minecrell.licenser") version "0.4.1"
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
val publishPlugin: PublishTask by tasks
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
    // IntelliJ IDEA dependency
    version = ideaVersion
    // Bundled plugin dependencies
    setPlugins(
        // needed dependencies for unit tests
        "properties", "junit")

    pluginName = "Chronomorph"
    updateSinceUntilBuild = false

    downloadSources = !CI && downloadIdeaSources.toBoolean()

    sandboxDirectory = project.rootDir.canonicalPath + "/.sandbox"
}

publishPlugin {
    if (properties["publish"] != null) {
        project.version = "${project.version}-${properties["buildNumber"]}"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs = listOf("-proc:none")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion
}

tasks.withType<GroovyCompile> {
    options.compilerArgs = listOf("-proc:none")
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
        excludeDirs.add(file(intellij.sandboxDirectory))
    }
}

// License header formatting
license {
    header = file("copyright.txt")
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

