plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "Time in Java: a DFramework plugin that walks through Java's date and time APIs, running every example live"

dependencies {
    implementation(project(":dframework-swing"))
    // The Joda-Time lessons run real Joda-Time code.
    implementation(libs.joda.time)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.dframework.time")
    }
}
