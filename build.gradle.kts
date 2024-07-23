@file:Suppress("UnstableApiUsage", "PropertyName")

import org.polyfrost.gradle.util.noServerRunConfigs
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.concurrent.atomic.AtomicReference

// Adds support for kotlin, and adds the Polyfrost Gradle Toolkit
// which we use to prepare the environment.
plugins {
    kotlin("jvm") apply false
    id("org.polyfrost.multi-version")
    id("org.polyfrost.defaults.repo")
    id("org.polyfrost.defaults.java")
    id("org.polyfrost.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom") version "1.3.2"
    id("signing")
    `maven-publish`
    java
}

// Gets the mod name, version and id from the `gradle.properties` file.
val mod_name: String by project
val mod_version: String by project
val mod_id: String by project
val mod_archives_name: String by project

// Replaces the variables in `ExampleMod.java` to the ones specified in `gradle.properties`.
blossom {
    replaceToken("@VER@", mod_version)
    replaceToken("@NAME@", mod_name)
    replaceToken("@ID@", mod_id)
}

// Sets the mod version to the one specified in `gradle.properties`. Make sure to change this following semver!
version = mod_version
// Sets the group, make sure to change this to your own. It can be a website you own backwards or your GitHub username.
// e.g. com.github.<your username> or com.<your domain>
group = "org.polyfrost"

// Sets the name of the output jar (the one you put in your mods folder and send to other people)
// It outputs all versions of the mod into the `versions/{mcVersion}/build` directory.
base {
    archivesName.set(mod_archives_name)
}

java {
    withSourcesJar()
}

// Configures Polyfrost Loom, our plugin fork to easily set up the programming environment.
loom {
    // Removes the server configs from IntelliJ IDEA, leaving only client runs.
    noServerRunConfigs()

    // Adds the tweak class if we are building legacy version of forge as per the documentation (https://docs.polyfrost.org)
    if (project.platform.isLegacyForge) {
        runConfigs {
            "client" {
                property("mixin.debug.export", "true") // Outputs all mixin changes to `versions/{mcVersion}/run/.mixin.out/class`
                programArgs("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            }
        }
    }
    // Configures the mixins if we are building for forge
    if (project.platform.isForge) {
        forge {
            mixinConfig("mixins.${mod_id}.json")
        }
    }
    // Configures the name of the mixin "refmap"
    mixin.defaultRefmapName.set("mixins.${mod_id}.refmap.json")
}

// Creates the shade/shadow configuration, so we can include libraries inside our mod, rather than having to add them separately.
val shade: Configuration by configurations.creating {
    configurations.api.get().extendsFrom(this)
}

val shadeNoPom: Configuration by configurations.creating {
    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(this@creating) }
}

// Configures the output directory for when building from the `src/resources` directory.
sourceSets {
    main {
        output.setResourcesDir(java.classesDirectory)
    }
}

// Adds the Polyfrost maven repository so that we can get the libraries necessary to develop the mod.
repositories {
    maven("https://repo.polyfrost.org/releases")
}

// Configures the libraries/dependencies for your mod.
dependencies {
    shadeNoPom("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

    shade("it.unimi.dsi:fastutil:8.5.13")
}

tasks {
    val atomicLines = AtomicReference(listOf<String>())
    val atomicRefmapLines = AtomicReference(listOf<String>())
    // Processes the `src/resources/mcmod.info`, `fabric.mod.json`, or `mixins.${mod_id}.json` and replaces
    // the mod id, name and version with the ones in `gradle.properties`
    processResources {
        inputs.property("id", mod_id)
        inputs.property("name", mod_name)
        val java = if (project.platform.mcMinor >= 18) {
            17 // If we are playing on version 1.18, set the java version to 17
        } else {
            // Else if we are playing on version 1.17, use java 16.
            if (project.platform.mcMinor == 17)
                16
            else
                8 // For all previous versions, we **need** java 8 (for Forge support).
        }
        val compatLevel = "JAVA_${java}"
        inputs.property("java", java)
        inputs.property("java_level", compatLevel)
        inputs.property("version", mod_version)
        inputs.property("mcVersionStr", project.platform.mcVersionStr)
        filesMatching(listOf("mcmod.info", "mixins.${mod_id}.json", "mods.toml")) {
            expand(
                mapOf(
                    "id" to mod_id,
                    "name" to mod_name,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to mod_version,
                    "mcVersionStr" to project.platform.mcVersionStr
                )
            )
        }
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to mod_id,
                    "name" to mod_name,
                    "java" to java,
                    "java_level" to compatLevel,
                    "version" to mod_version,
                    "mcVersionStr" to project.platform.mcVersionStr.substringBeforeLast(".") + ".x"
                )
            )
        }
    }

    withType(Jar::class) {
        // This removes the 10th line in mixins.legacycraftycrashes.json,
        // aka the test mixin
        doFirst {
            val mixinJson = layout.buildDirectory.asFile.get().resolve("classes")
                .resolve("java")
                .resolve("main")
                .resolve("mixins.${mod_id}.json")
            if (mixinJson.exists()) {
                val lines = mixinJson.readLines()
                if (lines[9].contains("_Test")) {
                    atomicLines.set(lines)
                    mixinJson.delete()
                    mixinJson.writeText(
                        lines.subList(0, 9).joinToString("\n").substringBeforeLast(",") + "\n" + lines.subList(
                            10,
                            lines.size
                        ).joinToString("\n")
                    )
                }
            }
            val refmapJson = layout.buildDirectory.asFile.get().resolve("classes")
                .resolve("java")
                .resolve("main")
                .resolve("mixins.${mod_id}.refmap.json")
            if (refmapJson.exists()) {
                // Remove lines 3-5, and lines 15-17
                val lines = refmapJson.readLines()
                if (lines[2].contains("_Test")) {
                    atomicRefmapLines.set(lines)
                    refmapJson.delete()
                    refmapJson.writeText(
                        lines.subList(0, 2).joinToString("\n") + "\n" + lines.subList(5, 14).joinToString("\n") + "\n" + lines.subList(
                            17,
                            lines.size
                        ).joinToString("\n")
                    )
                }
            }
        }

        doLast {
            val lines = atomicLines.get()
            val refmapLines = atomicRefmapLines.get()
            if (lines.isNotEmpty()) {
                val mixinJson = layout.buildDirectory.asFile.get().resolve("classes")
                    .resolve("java")
                    .resolve("main")
                    .resolve("mixins.${mod_id}.json")
                if (mixinJson.exists()) {
                    mixinJson.delete()
                    mixinJson.writeText(lines.joinToString("\n"))
                }
            }
            if (refmapLines.isNotEmpty()) {
                val refmapJson = layout.buildDirectory.asFile.get().resolve("classes")
                    .resolve("java")
                    .resolve("main")
                    .resolve("mixins.${mod_id}.refmap.json")

                if (refmapJson.exists()) {
                    refmapJson.delete()
                    refmapJson.writeText(refmapLines.joinToString("\n"))
                }
            }
        }

        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("javadoc", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
        }

        if (project.platform.isFabric) {
            exclude("mcmod.info", "mods.toml")
        } else {
            exclude("fabric.mod.json")
            if (project.platform.isLegacyForge) {
                exclude("mods.toml")
            } else {
                exclude("mcmod.info")
            }
        }
    }

    // Configures our shadow/shade configuration, so we can
    // include some dependencies within our mod jar file.
    shadowJar {
        from(remapJar)
        archiveClassifier.set("mod")
        configurations = listOf(shade, shadeNoPom)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    remapJar {
        archiveClassifier.set("")
    }

    assemble {
        dependsOn(shadowJar)
    }

    jar {
        manifest.attributes += mapOf(
            "ModSide" to "CLIENT", // We aren't developing a server-side mod
            "ForceLoadAsMod" to true, // We want to load this jar as a mod, so we force Forge to do so.
            "TweakOrder" to "0", // Makes sure that the OneConfig launch wrapper is loaded as soon as possible.
            "MixinConfigs" to "mixins.${mod_id}.json", // We want to use our mixin configuration, so we specify it here.
            "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker", // We want to use mixins, so we specify the MixinTweaker here.
        )
        archiveClassifier.set("dev")
    }
}

afterEvaluate {
    val configuration = configurations.getByName("shadowRuntimeElements")
    val jarTask = tasks.getByName("shadowJar") as ShadowJar
    for (artifact in configuration.artifacts) {
        if (artifact.file.absolutePath == jarTask.archiveFile.get().asFile.absolutePath && artifact.buildDependencies.getDependencies(null).contains(jarTask)) {
            configuration.artifacts.remove(artifact)
        }
    }
}

val mavenUsername = findProperty("polyfrost.publishing.maven.username")?.toString()
val mavenPassword = findProperty("polyfrost.publishing.maven.password")?.toString()

if (mavenUsername?.isNotBlank() == true && mavenPassword?.isNotBlank() == true) {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            register<MavenPublication>("maven") {
                from(components.getByName("java"))

                artifactId = rootProject.name.lowercase()
            }
        }

        repositories {
            maven("https://repo.polyfrost.org/releases") {
                name = "releases"
                credentials {
                    this@credentials.username = mavenUsername
                    this@credentials.password = mavenPassword
                }
            }
        }
    }
}