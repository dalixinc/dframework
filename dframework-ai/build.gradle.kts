plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "Claude-backed AiAssistant for DFramework, using the Anthropic Java SDK"

dependencies {
    implementation(project(":dframework-swing"))
    implementation(libs.anthropic.java)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.dframework.ai")
    }
}
