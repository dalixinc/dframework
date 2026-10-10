plugins {
    id("dframework.java-conventions")
    `java-library`
}

description = "DFramework for Swing: batteries-included extensions of the standard Swing components"

dependencies {
    implementation(libs.flatlaf)
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "com.dframework.swing")
    }
}
