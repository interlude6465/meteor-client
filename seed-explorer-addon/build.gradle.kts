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
    fun cachedJar(group: String, artifact: String, version: String): File {
        val artifactDir = gradle.gradleUserHomeDir
            .resolve("caches/modules-2/files-2.1")
            .resolve(group)
            .resolve(artifact)
            .resolve(version)

        return artifactDir.walkTopDown()
            .firstOrNull { it.isFile && it.name == "$artifact-$version.jar" }
            ?: artifactDir.resolve("$artifact-$version.jar")
    }

    val meteorClientClasses = file("../build/classes/java/main")
    val meteorClientResources = file("../build/resources/main")
    val minecraftMergedJar = file("../.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-ea76bb5afc/26.1.2/minecraft-merged-ea76bb5afc-26.1.2.jar")
    val brigadierJar = cachedJar("com.mojang", "brigadier", "1.3.10")
    val orbitJar = cachedJar("meteordevelopment", "orbit", "0.2.4")
    val fabricLoaderJar = cachedJar("net.fabricmc", "fabric-loader", "0.19.2")
    val mixinJar = cachedJar("net.fabricmc", "sponge-mixin", "0.17.2+mixin.0.8.7")
    val jspecifyJar = cachedJar("org.jspecify", "jspecify", "1.0.0")
    val fastutilJar = cachedJar("it.unimi.dsi", "fastutil", "8.5.18")
    val gsonJar = cachedJar("com.google.code.gson", "gson", "2.13.2")
    val guavaJar = cachedJar("com.google.guava", "guava", "33.5.0-jre")
    val failureAccessJar = cachedJar("com.google.guava", "failureaccess", "1.0.3")
    val commonsLang3Jar = cachedJar("org.apache.commons", "commons-lang3", "3.19.0")
    val log4jApiJar = cachedJar("org.apache.logging.log4j", "log4j-api", "2.25.2")
    val log4jCoreJar = cachedJar("org.apache.logging.log4j", "log4j-core", "2.25.2")
    val slf4jJar = cachedJar("org.slf4j", "slf4j-api", "2.0.17")
    val datafixerupperJar = cachedJar("com.mojang", "datafixerupper", "9.0.19")
    val jomlJar = cachedJar("org.joml", "joml", "1.10.8")
    val icu4jJar = cachedJar("com.ibm.icu", "icu4j", "77.1")
    val authlibJar = cachedJar("com.mojang", "authlib", "7.0.63")
    val mojangLoggingJar = cachedJar("com.mojang", "logging", "1.6.11")
    val nettyCommonJar = cachedJar("io.netty", "netty-common", "4.2.7.Final")
    val nettyBufferJar = cachedJar("io.netty", "netty-buffer", "4.2.7.Final")
    val nettyTransportJar = cachedJar("io.netty", "netty-transport", "4.2.7.Final")
    val nettyResolverJar = cachedJar("io.netty", "netty-resolver", "4.2.7.Final")
    val nettyCodecBaseJar = cachedJar("io.netty", "netty-codec-base", "4.2.7.Final")
    val nettyHandlerJar = cachedJar("io.netty", "netty-handler", "4.2.7.Final")

    compileOnly(files(
        meteorClientClasses, meteorClientResources, minecraftMergedJar, brigadierJar, orbitJar, fabricLoaderJar,
        mixinJar, jspecifyJar, fastutilJar, gsonJar, guavaJar, failureAccessJar, commonsLang3Jar,
        log4jApiJar, log4jCoreJar, slf4jJar, datafixerupperJar, jomlJar,
        icu4jJar, authlibJar, mojangLoggingJar,
        nettyCommonJar, nettyBufferJar, nettyTransportJar, nettyResolverJar, nettyCodecBaseJar, nettyHandlerJar
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
            "minecraft_version" to "26.1.2",
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

    register<JavaExec>("offlineEndCityReport") {
        group = "verification"
        description = "Generates an offline End city prediction report without launching Minecraft."
        dependsOn(classes)
        mainClass.set("me.seedexplorer.addon.tools.EndCityOfflineReport")
        classpath = sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
        args(
            providers.gradleProperty("seed").orElse("4717879387438598985").get(),
            providers.gradleProperty("x").orElse("0").get(),
            providers.gradleProperty("z").orElse("0").get(),
            providers.gradleProperty("radiusChunks").orElse("2048").get()
        )
    }

    register<JavaExec>("offlineOreEspCheck") {
        group = "verification"
        description = "Checks the ore ESP predictor around a point without launching Minecraft."
        dependsOn(classes)
        mainClass.set("me.seedexplorer.addon.tools.OreEspOfflineCheck")
        classpath = sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
        args(
            providers.gradleProperty("seed").orElse("4717879387438598985").get(),
            providers.gradleProperty("x").orElse("5289").get(),
            providers.gradleProperty("y").orElse("70").get(),
            providers.gradleProperty("z").orElse("-6879").get(),
            providers.gradleProperty("radiusChunks").orElse("2").get(),
            providers.gradleProperty("maxBoxes").orElse("80").get(),
            providers.gradleProperty("ore").orElse("DIAMOND").get()
        )
    }
}
