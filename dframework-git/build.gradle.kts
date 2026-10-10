plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "Git Browser: a DFramework plugin for exploring git repositories read-only, showing every command it runs"

dependencies {
    implementation(project(":dframework-swing"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.dframework.git")
    }
}
