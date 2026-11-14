plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":api"))
//    implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.keycloak:keycloak-admin-client:${project.properties["keycloakAdminClient"]}")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Since Boot 4, Flyway auto-configuration lives in its own starter; flyway-core alone no longer runs migrations.
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}
