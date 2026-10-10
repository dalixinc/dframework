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

application {
    mainClass = "com.dframework.demo.DemoApp"
}
