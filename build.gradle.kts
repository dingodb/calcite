
plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.kotlin.jvm") version "1.9.20" apply false
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

group = "io.dingodb"
version = project.property("calcite.version") as String

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.register("publishToMaven") {
    dependsOn("publishMavenJavaPublicationToMavenRepository")
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}