
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
    
    pom {
        name.set("Calcite LINQ4J")
        description.set("DingoDB LINQ4J module providing LINQ support for Calcite")
        url.set("https://www.dingodb.com/")
        
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        
        developers {
            developer {
                id.set("dingodb")
                name.set("DingoDB develop team")
                email.set("dingodb@zetyun.com")
            }
        }
        
        scm {
            connection.set("scm:git:git://github.com/dingodb/calcite.git")
            developerConnection.set("scm:git:ssh://github.com/dingodb/calcite.git")
            url.set("https://github.com/dingodb/calcite")
        }
    }
    
    publishToMavenCentral()
    signAllPublications()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
