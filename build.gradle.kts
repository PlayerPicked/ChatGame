plugins {
    java
    id("com.gradleup.shadow") version "8.3.10"
    id("io.github.revxrsal.zapper") version "1.0.3"
}

group = "com.playerpicked"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.playerpicked.com/releases")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.3")

    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.19") {
        exclude(module = "lamp.common")
        exclude(module = "lamp.brigadier")
    }
    implementation("dev.dejvokep:boosted-yaml:1.3.7")
    implementation("com.playerpicked:pickly-paper:1.0.1")

    zap("io.github.revxrsal:lamp.common:4.0.0-rc.19")
    zap("io.github.revxrsal:lamp.brigadier:4.0.0-rc.19")
    zap("com.mysql:mysql-connector-j:9.7.0")
    zap("com.h2database:h2:2.5.252")
    zap("com.zaxxer:HikariCP:7.1.0")
    zap("redis.clients:jedis:7.5.3")

    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("com.h2database:h2:2.5.252")
    testImplementation("com.zaxxer:HikariCP:7.1.0")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation,removal,unchecked"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.shadowJar {
    archiveClassifier = ""
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

zapper {
    libsFolder = "libs"
    relocationPrefix = "com.playerpicked.chatgame.libs"

    repositories { includeProjectRepositories() }

    relocate("dev.dejvokep.boostedyaml", "boostedyaml")
    relocate("com.playerpicked.pickly", "pickly")
}
