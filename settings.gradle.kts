pluginManagement{
    val springBootVersion: String by settings
    val springBootDependencyManagementVersion: String by settings
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
