plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "DNS Dig: a DFramework plugin for running and explaining dig lookups"

dependencies {
    implementation(project(":dframework-swing"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.dframework.dig")
    }
}
