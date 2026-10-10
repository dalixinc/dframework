plugins {
    id("dframework.java-conventions")
    application
}

description = "Showcase application for DFramework"

dependencies {
    implementation(project(":dframework-swing"))
    // Kunststoff (2004) is not published to Maven Central, so it is vendored in libs/.
    runtimeOnly(files("$rootDir/libs/kunststoff.jar"))
}

// The User Guide's examples are compiled by a test, so it must re-run when the guide changes.
tasks.test {
    inputs.file(rootProject.file("docs/USER_GUIDE.md"))
}

application {
    mainClass = "com.dframework.demo.DemoApp"
}
