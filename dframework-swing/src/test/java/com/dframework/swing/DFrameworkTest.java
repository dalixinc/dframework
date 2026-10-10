package com.dframework.swing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DFrameworkTest {

    @Test
    void theBuildStampsTheProjectVersion() {
        // Gradle passes the project version to the tests, so this follows gradle.properties.
        assertThat(DFramework.version()).isEqualTo(System.getProperty("dframework.expectedVersion"));
        assertThat(DFramework.nameAndVersion()).isEqualTo("DFramework " + DFramework.version());
    }
}
