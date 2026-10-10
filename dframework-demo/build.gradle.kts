plugins {
    id("dframework.java-conventions")
    application
}

description = "Showcase application for DFramework"

dependencies {
    implementation(project(":dframework-swing"))
    // Kunststoff (2004) is not published to Maven Central, so it is vendored in libs/.
    runtimeOnly(files("$rootDir/libs/kunststoff.jar"))
    // Plugins are found at runtime through ServiceLoader; the demo's code never refers to them.
    runtimeOnly(project(":dframework-dig"))
    // The Claude-backed AI assistant, also found at runtime (needs ANTHROPIC_API_KEY to answer).
    runtimeOnly(project(":dframework-ai"))
}

// The User Guide's examples are compiled by a test, so it must re-run when the guide changes.
tasks.test {
    inputs.file(rootProject.file("docs/USER_GUIDE.md"))
}

application {
    mainClass = "com.dframework.demo.DemoApp"
}
