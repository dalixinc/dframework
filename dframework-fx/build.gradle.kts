plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "DFramework for JavaFX (spike): DStage, themes and DTextField"

// JavaFX jars are per platform. Pick this machine's, as the OpenJFX Gradle plugin would.
val javafxVersion: String = libs.versions.javafx.get()
val javafxPlatform = System.getProperty("os.name").lowercase().let { os ->
    val arm = System.getProperty("os.arch") == "aarch64"
    when {
        os.contains("win") -> "win"
        os.contains("mac") -> if (arm) "mac-aarch64" else "mac"
        else -> if (arm) "linux-aarch64" else "linux"
    }
}

dependencies {
    for (module in listOf("base", "graphics", "controls")) {
        api("org.openjfx:javafx-$module:$javafxVersion:$javafxPlatform")
    }
    implementation(libs.atlantafx)
    // Spike only: reuse Validator and InputFilter from the Swing library, to prove the logic
    // can be shared. Step 1 of the plan moves them into a toolkit-neutral core module.
    api(project(":dframework-swing"))
}

// JavaFX's version is pinned for every module in buildSrc (dframework.java-conventions).

tasks.withType<JavaCompile>().configureEach {
    // The Swing library is an automatic module (spike only), which -Xlint:all would flag.
    options.compilerArgs.add("-Xlint:-requires-automatic,-requires-transitive-automatic")
}
