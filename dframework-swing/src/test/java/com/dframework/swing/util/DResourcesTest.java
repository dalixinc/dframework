package com.dframework.swing.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DResourcesTest {

    @Test
    void loadsClasspathResourceWithOrWithoutLeadingSlash() {
        assertThat(DResources.load("com/dframework/swing/icons/app.gif")).isPresent();
        assertThat(DResources.load("/com/dframework/swing/icons/app.gif")).isPresent();
    }

    @Test
    void loadsFilePath(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("icon.gif");
        try (InputStream in = DResources.class.getResourceAsStream("/com/dframework/swing/icons/app.gif")) {
            Files.copy(in, file);
        }

        assertThat(DResources.load(file.toString())).hasValueSatisfying(icon ->
                assertThat(icon.getIconWidth()).isEqualTo(32));
    }

    @Test
    void missingLocationIsEmpty() {
        assertThat(DResources.load("no/such/icon.gif")).isEmpty();
    }
}
