package com.dframework.swing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Facts about the DFramework library itself. */
public final class DFramework {

    /** Shown when the version isn't known, e.g. when running from unprocessed sources. */
    public static final String UNKNOWN_VERSION = "development";

    private static final String VERSION = loadVersion();

    private DFramework() {
    }

    /**
     * The library's version, such as {@code 4.0.0} or {@code 4.0.0-SNAPSHOT}.
     * The build writes it from {@code gradle.properties}, so it's always the
     * version of the jar in use.
     */
    public static String version() {
        return VERSION;
    }

    /** "DFramework 4.0.0", for title bars, status bars and About boxes. */
    public static String nameAndVersion() {
        return "DFramework " + VERSION;
    }

    private static String loadVersion() {
        try (InputStream in = DFramework.class.getResourceAsStream("version.properties")) {
            if (in == null) {
                return UNKNOWN_VERSION;
            }
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("version", "").strip();
            return v.isEmpty() || v.contains("${") ? UNKNOWN_VERSION : v;
        } catch (IOException e) {
            return UNKNOWN_VERSION;
        }
    }
}
