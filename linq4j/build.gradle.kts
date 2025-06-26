
plugins {
    id("java")
    id("maven-publish")
    id("com.vanniktech.maven.publish")
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

mavenPublishing {
    coordinates("io.dingodb", "calcite-linq4j", "1.33.0-SNAPSHOT")
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
