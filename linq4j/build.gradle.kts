
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
    implementation("org.apiguardian:apiguardian-api:1.1.2")
    implementation("org.checkerframework:checker-qual:0.5.16")
    implementation("com.google.guava:guava:33.3.0-jre")
    implementation("org.apache.calcite.avatica:avatica-core:1.26.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.dingodb"
            artifactId = "calcite-linq4j"
            version = "1.33.0-SNAPSHOT"
        }
    }

    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

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
