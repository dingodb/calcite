
plugins {
    id("java")
    id("maven-publish")
}

group = "io.dingodb"
version = "1.33.0-SNAPSHOT"

repositories {
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