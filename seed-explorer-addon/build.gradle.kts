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

val meteorClientJar = file("/home/engine/.m2/repository/meteordevelopment/meteor-client/26.1.2-SNAPSHOT/meteor-client-26.1.2-SNAPSHOT.jar")
val minecraftMergedJar = file("/home/engine/project/.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-ea76bb5afc/26.1.2/minecraft-merged-ea76bb5afc-26.1.2.jar")

dependencies {
    compileOnly(files(meteorClientJar, minecraftMergedJar))
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