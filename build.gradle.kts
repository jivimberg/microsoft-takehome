plugins {
    id("java")
}

group = "com.microsoft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:24.0.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.2")
    implementation("ch.qos.logback:logback-classic:1.4.12")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation ("org.awaitility:awaitility-kotlin:4.2.2")
}

tasks.test {
    useJUnitPlatform()
}