plugins {
    id("dframework.java-conventions")
    `java-library`
    `maven-publish`
}

description = "DFramework for Swing: batteries-included extensions of the standard Swing components"

dependencies {
    implementation(libs.flatlaf)
}

// Stamp the project version into the library, for DFramework.version().
val libraryVersion = project.version.toString()
tasks.processResources {
    val stamp = mapOf("version" to libraryVersion)   // a local, so the configuration cache can store it
    inputs.properties(stamp)
    filesMatching("com/dframework/swing/version.properties") {
        expand(stamp)
    }
}

tasks.jar {
    manifest {
        attributes(
            "Automatic-Module-Name" to "com.dframework.swing",
            "Implementation-Title" to "DFramework for Swing",
            "Implementation-Version" to libraryVersion,
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "dframework-swing"
            from(components["java"])
        }
    }
}

tasks.test {
    systemProperty("dframework.expectedVersion", libraryVersion)
}
