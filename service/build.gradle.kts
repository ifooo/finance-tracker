val poiVersion = providers.gradleProperty("poiVersion").get()
val flyingSaucerVersion = providers.gradleProperty("flyingSaucerVersion").get()
val openPdfVersion = providers.gradleProperty("openPdfVersion").get()

dependencies {
    api(project(":model"))
    api("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-freemarker")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")
    // HTML (FreeMarker) -> PDF; the extra fonts bring Liberation Sans, which covers Cyrillic.
    implementation("org.xhtmlrenderer:flying-saucer-pdf:$flyingSaucerVersion")
    implementation("com.github.librepdf:openpdf-fonts-extra:$openPdfVersion")

    testImplementation(project(":application"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}
