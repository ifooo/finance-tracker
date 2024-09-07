plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    `java-library`
}

group = "com.parcels"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement{
    imports{
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {

    api("org.springframework.data:spring-data-jpa")
    api("jakarta.persistence:jakarta.persistence-api")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.apache.commons:commons-lang3")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}