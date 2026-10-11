plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

// AtlantaFX 2.1.0 was built against JavaFX 23, and Gradle picks the highest version it sees,
// so without this the JavaFX modules would quietly run JavaFX 23. Hold every JavaFX
// artifact at the catalog's version (the JavaFX 21 LTS line, matching Java 21).
val javafxVersion = libs.findVersion("javafx").get().requiredVersion
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.openjfx") {
            useVersion(javafxVersion)
            because("DFramework for JavaFX targets JavaFX 21 LTS")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all,-this-escape", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        addStringOption("Xdoclint:all,-missing", "-quiet")
    }
}

tasks.test {
    useJUnitPlatform()
}
