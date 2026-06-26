// Simple Java project that compiles the addon against the meteor-client jar
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

val meteorClientJar = file("/home/engine/project/build/libs/meteor-client-26.1.2-local.jar")
val minecraftMergedJar = file("/home/engine/project/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-ea76bb5afc/26.1.2/minecraft-merged-ea76bb5afc-26.1.2.jar")
val brigadierJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/com.mojang/brigadier/1.3.10/d15b53a14cf20fdcaa98f731af5dda654452c010/brigadier-1.3.10.jar")
val orbitJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/meteordevelopment/orbit/0.2.4/3a5ba3b5d3d5a43eda3b145bbe81cd5607f86422/orbit-0.2.4.jar")
val fabricLoaderJar = file("/home/engine/.gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loader/0.19.2/cc647a5b22dbc49b9a3267cebcee25fc4882b108/fabric-loader-0.19.2.jar")

dependencies {
    compileOnly(files(meteorClientJar, minecraftMergedJar, brigadierJar, orbitJar, fabricLoaderJar))
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