import com.github.vlsi.gradle.crlf.CrLfSpec
import com.github.vlsi.gradle.crlf.LineEndings
import com.github.vlsi.gradle.ide.dsl.settings
import com.github.vlsi.gradle.ide.dsl.taskTriggers

plugins {
    id("java")
    id("maven-publish")
    kotlin("jvm") version "1.9.20"
    id("com.github.vlsi.crlf") version "1.0.0"
    id("com.github.vlsi.ide") version "1.0.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.5"
    id("org.javacc.javacc") version "3.0.0"
    id("com.vanniktech.maven.publish")
}

group = "io.dingodb"
version = "1.33.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":calcite-linq4j"))
    api("org.locationtech.jts:jts-core:1.19.0")
    api("org.locationtech.jts.io:jts-io-common:1.19.0")
    api("org.locationtech.proj4j:proj4j:1.2.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.15.0")
    api("com.google.errorprone:error_prone_annotations:2.5.1")
    api("com.google.guava:guava:33.3.0-jre")
    api("org.apache.calcite.avatica:avatica-core:1.26.0")
    api("org.apiguardian:apiguardian-api:1.1.2")
    api("org.checkerframework:checker-qual:3.10.0")
    api("org.slf4j:slf4j-api:1.7.25")

    implementation("com.fasterxml.jackson.core:jackson-core:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0")
    implementation("com.google.uzaygezen:uzaygezen-core:0.2") {
        exclude("log4j", "log4j").because("conflicts with log4j-slf4j-impl which uses log4j2 and" +
                " also leaks transitively to projects depending on calcite-core")
    }
    implementation("com.jayway.jsonpath:json-path:2.7.0")
    implementation("com.yahoo.datasketches:sketches-core:0.9.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("net.hydromatic:aggdesigner-algorithm:6.0")
    implementation("org.apache.commons:commons-dbcp2:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-io:commons-io:2.15.0")
    implementation("org.codehaus.janino:commons-compiler:3.1.11")
    implementation("org.codehaus.janino:janino:3.1.11")
    annotationProcessor("org.immutables:value:2.8.8")
    compileOnly("org.immutables:value-annotations:2.8.8")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testAnnotationProcessor("org.immutables:value:2.8.8")
    testCompileOnly("org.immutables:value-annotations:2.8.8")
    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("commons-lang:commons-lang:2.0")
    testImplementation("net.bytebuddy:byte-buddy:1.14.15")
    testImplementation("net.hydromatic:foodmart-queries:0.4.1")
    testImplementation("net.hydromatic:quidem:0.11")
    testImplementation("org.apache.calcite.avatica:avatica-server:1.26.0")
    testImplementation("org.apache.commons:commons-pool2:2.12.0")
    testImplementation("org.hsqldb:hsqldb:2.7.2")
    testImplementation("sqlline:sqlline:1.12.0")
    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))

    // proj4j-epsg must not be converted to 'implementation' due to its license
    testRuntimeOnly("org.locationtech.proj4j:proj4j-epsg:1.2.2")

    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

mavenPublishing {
    coordinates("io.dingodb", "calcite-core", "1.33.0-SNAPSHOT")
    publishToMavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")

            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_TOKEN")
            }
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
