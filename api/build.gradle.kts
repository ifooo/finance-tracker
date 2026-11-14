val springdocVersion = providers.gradleProperty("springdocVersion").get()
val commonsIoVersion = providers.gradleProperty("commonsIoVersion").get()

dependencies {
    implementation(project(":service"))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
}
