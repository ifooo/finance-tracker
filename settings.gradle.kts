pluginManagement{
    val springBootVersion = providers.gradleProperty("springBootVersion").get()
    val springBootDependencyManagementVersion = providers.gradleProperty("springBootDependencyManagementVersion").get()
    repositories {
        mavenCentral()
    }
    plugins{
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version springBootDependencyManagementVersion
    }
}
rootProject.name = "finance-tracker"
include("application")
include("api")
include("model")
include("service")
