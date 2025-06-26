plugins {
    id("java")
    kotlin("jvm") version "1.9.20"
}

group = "io.dingodb"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":calcite-core"))
    implementation("org.checkerframework:checker-qual:3.10.0")

    implementation(platform("org.junit:junit-bom:5.9.1"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.hydromatic:quidem:0.11")
    implementation("net.hydromatic:foodmart-data-hsqldb:0.5")
    implementation("net.hydromatic:foodmart-queries:0.4.1")
    implementation("net.hydromatic:scott-data-hsqldb:0.2")
    implementation("org.apache.commons:commons-dbcp2:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.13.0")
    implementation("org.apache.commons:commons-pool2:2.12.0")
    implementation("org.hamcrest:hamcrest:2.1")
    implementation("org.hsqldb:hsqldb:2.7.2")
    annotationProcessor("org.immutables:value:2.8.8")
    compileOnly("org.immutables:value-annotations:2.8.8")
    implementation("org.incava:java-diff:1.1.2")
    implementation("org.junit.jupiter:junit-jupiter:5.9.1")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}