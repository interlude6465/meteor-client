// Simple Java project that compiles the addon against the meteor-client jar and Minecraft dependencies
plugins {
    java
}

base {
    archivesName = "meteor-seed-explorer"
    group = "me.seedexplorer"
    version = "1.0.0"
}

repositories {
    mavenLocal()
    mavenCentral()
}

// Find all jars from the main project's classpath
val classpathJars: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // Core mod dependencies (files)
    val meteorClientJar = file("/home/engine/project/build/libs/meteor-client-26.1.2-local.jar")
    val minecraftMergedJar = file("/home/engine/project/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-ea76bb5afc/26.1.2/minecraft-merged-ea76bb5afc-26.1.2.jar")
    val brigadierJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.mojang/brigadier/1.3.10/d15b53a14cf20fdcaa98f731af5dda654452c010/brigadier-1.3.10.jar")
    val orbitJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/meteordevelopment/orbit/0.2.4/3a5ba3b5d3d5a43eda3b145bbe81cd5607f86422/orbit-0.2.4.jar")
    val fabricLoaderJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loader/0.19.2/cc647a5b22dbc49b9a3267cebcee25fc4882b108/fabric-loader-0.19.2.jar")
    val jspecifyJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.jspecify/jspecify/1.0.0/7425a601c1c7ec76645a78d22b8c6a627edee507/jspecify-1.0.0.jar")
    val fastutilJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/it.unimi.dsi/fastutil/8.5.18/a6cff377eecc19c2037bf31568a6d7106b50ba1f/fastutil-8.5.18.jar")
    val gsonJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.13.2/48b8230771e573b54ce6e867a9001e75977fe78e/gson-2.13.2.jar")
    val guavaJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.google.guava/guava/33.5.0-jre/8699de25f2f979108d6c1b804a7ba38cda1116bc/guava-33.5.0-jre.jar")
    val commonsLang3Jar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-lang3/3.19.0/d6524b169a6574cd253760c472d419b47bfd37e6/commons-lang3-3.19.0.jar")
    val log4jApiJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.apache.logging.log4j/log4j-api/2.25.2/292c1a2b1702f1e1e3adb13e1c57e5bff60335ff/log4j-api-2.25.2.jar")
    val log4jCoreJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.apache.logging.log4j/log4j-core/2.25.2/f6564fc7ec28106ac6ca5b3b9b8adc2b3e99ec27/log4j-core-2.25.2.jar")
    val slf4jJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api/2.0.17/d9e58ac9c7779ba3bf8142aff6c830617a7fe60f/slf4j-api-2.0.17.jar")
    val datafixerupperJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.mojang/datafixerupper/9.0.19/4e91f9712fa1e83231d1501625381b0210a977da/datafixerupper-9.0.19.jar")
    val jomlJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/org.joml/joml/1.10.8/c07e3aac5e8db9e56d04b59c4d15f60cac5f99ce/joml-1.10.8.jar")
    val icu4jJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.ibm.icu/icu4j/77.1/60f778f70f2a7fe6c8bddb78b9c0cf7f8e3defc6/icu4j-77.1.jar")
    val authlibJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.mojang/authlib/7.0.63/c1ed3faebecf9b8a2defab8da8acb4a7f6e81d9b/authlib-7.0.63.jar")

    compileOnly(files(
        meteorClientJar, minecraftMergedJar, brigadierJar, orbitJar, fabricLoaderJar,
        jspecifyJar, fastutilJar, gsonJar, guavaJar, commonsLang3Jar,
        log4jApiJar, log4jCoreJar, slf4jJar, datafixerupperJar, jomlJar,
        icu4jJar, authlibJar
    ))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "jdk_version" to "25",
            "minecraft_version" to "~1.21.4",
            "loader_version" to "0.19.2"
        )

        inputs.properties(propertyMap)
        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        from("src/main/resources")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )
    }
}