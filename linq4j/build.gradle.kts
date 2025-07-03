
plugins {
    id("java")
    id("maven-publish")
    id("com.vanniktech.maven.publish")
}

group = "io.dingodb"
version = project.property("calcite.version") as String

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
    coordinates("io.dingodb", "calcite-linq4j", project.property("calcite.version") as String)
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
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/");
            }

            credentials {
                username = System.getenv("mavenCentralUsername")
                password = System.getenv("mavenCentralPassword")
            }
        }
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
