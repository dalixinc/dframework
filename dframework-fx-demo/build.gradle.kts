plugins {
    id("dframework.java-conventions")
    application
}

description = "JavaFX showcase for DFramework (spike)"

dependencies {
    implementation(project(":dframework-fx"))
    implementation(libs.atlantafx)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:-requires-automatic,-requires-transitive-automatic")
}

application {
    mainModule = "com.dframework.fxdemo"
    mainClass = "com.dframework.fxdemo.FxDemoApp"
}
