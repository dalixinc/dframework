package com.dframework.swing.theme;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

/**
 * The look and feels DFramework knows how to install.
 *
 * <p>The FlatLaf themes ship with DFramework. The JDK look and feels are always
 * present, though some only work on their own platform. Third-party look and
 * feels such as Kunststoff are used when their jar is on the classpath. Check
 * {@link #isAvailable()} before offering a theme to users.
 *
 * <p>Successor to the 2012 {@code KnownLookAndFeels} enum.
 */
public enum DTheme {

    FLAT_LIGHT("FlatLaf Light", "com.formdev.flatlaf.FlatLightLaf", false),
    FLAT_DARK("FlatLaf Dark", "com.formdev.flatlaf.FlatDarkLaf", true),
    FLAT_INTELLIJ("FlatLaf IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf", false),
    FLAT_DARCULA("FlatLaf Darcula", "com.formdev.flatlaf.FlatDarculaLaf", true),
    FLAT_MAC_LIGHT("FlatLaf macOS Light", "com.formdev.flatlaf.themes.FlatMacLightLaf", false),
    FLAT_MAC_DARK("FlatLaf macOS Dark", "com.formdev.flatlaf.themes.FlatMacDarkLaf", true),

    SYSTEM("System", UIManager::getSystemLookAndFeelClassName, false),
    METAL("Metal", "javax.swing.plaf.metal.MetalLookAndFeel", false),
    OCEAN("Metal Ocean", "javax.swing.plaf.metal.MetalLookAndFeel", false),
    NIMBUS("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel", false),
    MOTIF("Motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel", false),
    GTK("GTK+", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel", false),
    WINDOWS("Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel", false),
    AQUA("macOS Aqua", "com.apple.laf.AquaLookAndFeel", false),

    KUNSTSTOFF("Kunststoff", "com.incors.plaf.kunststoff.KunststoffLookAndFeel", false);

    private final String displayName;
    private final Supplier<String> className;
    private final boolean dark;

    DTheme(String displayName, String className, boolean dark) {
        this(displayName, () -> className, dark);
    }

    DTheme(String displayName, Supplier<String> className, boolean dark) {
        this.displayName = displayName;
        this.className = className;
        this.dark = dark;
    }

    /** Human-readable name, suitable for menus. */
    public String displayName() {
        return displayName;
    }

    /** Fully qualified {@link LookAndFeel} class name. */
    public String className() {
        return className.get();
    }

    /** Whether this theme uses light text on a dark background. */
    public boolean isDark() {
        return dark;
    }

    /**
     * Whether the look and feel is on the classpath and supported on this platform.
     * For example, {@link #WINDOWS} reports {@code false} on Linux.
     */
    public boolean isAvailable() {
        String name = className();
        // The JDK's platform look and feels live in packages java.desktop does not
        // export, so they cannot be instantiated here, but UIManager can load them.
        if (Arrays.stream(UIManager.getInstalledLookAndFeels()).anyMatch(info -> info.getClassName().equals(name))) {
            return true;
        }
        try {
            return newLookAndFeel().isSupportedLookAndFeel();
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    /** All themes for which {@link #isAvailable()} is {@code true}, in declaration order. */
    public static List<DTheme> available() {
        return Arrays.stream(values()).filter(DTheme::isAvailable).toList();
    }

    /** Applies any per-theme setup that must happen before the look and feel is installed. */
    void prepare() {
        switch (this) {
            case METAL -> MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
            case OCEAN -> MetalLookAndFeel.setCurrentTheme(new OceanTheme());
            default -> {
            }
        }
    }

    private LookAndFeel newLookAndFeel() throws ReflectiveOperationException {
        Class<?> type = Class.forName(className(), true, Thread.currentThread().getContextClassLoader());
        return (LookAndFeel) type.getDeclaredConstructor().newInstance();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
