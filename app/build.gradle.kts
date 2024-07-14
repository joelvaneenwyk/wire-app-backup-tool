import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.konan.properties.saveToFile
import java.util.Properties
import org.jetbrains.kotlin.konan.file.File

plugins {
    id("buildlogic.java-application-conventions")
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("net.nemerosa.versioning") version "3.1.0"
}

dependencies {
    implementation("org.apache.commons:commons-text")

    implementation(libs.wire.xenon)
    implementation(libs.wire.helium)

    implementation(libs.konan)
    implementation(libs.kotlin)

    implementation(libs.github.johnrengelman.shadow)
    implementation(libs.nemerosa.versioning)

    testImplementation(libs.test)
    testImplementation(libs.test.junit5)
    testImplementation(libs.script.runtime)

    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly(libs.junit.platform.launcher)

    // ------- Java dependencies -------

    api(libs.jakarta.ws.rs.jakarta.ws.rs.api)
    api(libs.org.glassfish.tyrus.bundles.tyrus.standalone.client)
    api(libs.org.glassfish.jersey.core.jersey.client)
    api(libs.org.flywaydb.flyway.core)
    api(libs.org.glassfish.jersey.inject.jersey.hk2)
    api(libs.com.fasterxml.jackson.jaxrs.jackson.jaxrs.json.provider)

    api(libs.katlib)

    // command line arguments parsing
    implementation(libs.picocli)

    // html compilation
    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.autolink)

    implementation(libs.openhtmltopdf.core)
    implementation(libs.openhtmltopdf.pdfbox)
    implementation(libs.openhtmltopdf.svg.support)
    implementation(libs.spullara.mustache.java.compiler)

    // ------- Common dependencies -------
    implementation(libs.lingala.zip4j)
    implementation(libs.lazycode.lazysodium.java) {
       // otherwise the application won't start, the problem is combination of Dropwizard and sl4j 2.0
       exclude("org.slf4j", "slf4j-api")
    }
    implementation(libs.microutils.kotlin.logging)

    // ------- Kotlin dependencies -------
    implementation(libs.pw.forst.tools.katlib)
    implementation(libs.java.dev.jna)

    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java)
    implementation(libs.xerial.sqlite.jdbc)

    // jackson for kotlin
    implementation(libs.fasterxml.jackson.kotlin)

    // correct reflect lib until jackson fixes theirs
    implementation(libs.jetbrains.kotlin.reflect)
}

group = "com.wire.backups"
version = (versioning.info.tag ?: versioning.info.lastTag) + if (versioning.info.dirty) "-dirty" else ""

private val mClass: String = "com.wire.backups.exports.Service"

application {
    // Define the main class for the application.
    mainClass.set(mClass)
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

tasks {
    compileKotlin {}
    compileTestKotlin {}

    withType<Test> {
        systemProperties["jna.library.path"] = "${projectDir}/libs"
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<ShadowJar> {
        setProperty("zip64", true)
        setProperty("enableRelocation", true)

        mergeServiceFiles()

        manifest {
            attributes(mapOf("Main-Class" to mClass))
        }

        // because there's some conflict (LICENSE already exists) during the unzipping process
        // by excluding it from the shadow jar we try to fix problem on Oracle JVM 8
        exclude("LICENSE")
        // standard Dropwizard excludes
        exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveFileName.set("backup-export.jar")
    }

    test {
        useJUnitPlatform()
    }

    classes {
        dependsOn("createVersionFile")
    }

    register("createVersionFile") {
        dependsOn(processResources)
        doLast {
            Properties().apply {
                setProperty("version", project.version.toString())
                saveToFile(File("${layout.buildDirectory.get().asFile}/resources/main/version.properties"))
            }
        }
    }

    register("resolveDependencies") {
        doLast {
            project.allprojects.forEach { subProject ->
                with(subProject) {
                    buildscript.configurations.forEach { if (it.isCanBeResolved) it.resolve() }
                    configurations.compileClasspath.get().resolve()
                    configurations.testCompileClasspath.get().resolve()
                }
            }
        }
    }
}
