package com.dframework.swing.util;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;

/**
 * Classpath-based access to DFramework's bundled icons and localised text.
 *
 * <p>Everything is loaded from the classpath rather than the working directory,
 * so it works the same from an IDE, a jar or a jlink image.
 */
public final class DResources {

    private static final String ICON_ROOT = "/com/dframework/swing/icons/";
    private static final String MESSAGES = "com.dframework.swing.messages";

    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private DResources() {
    }

    /**
     * Loads one of DFramework's own icons, e.g. {@code "app.gif"}.
     *
     * @param name file name within the DFramework icon folder
     * @return the icon, or empty if no such resource exists
     */
    public static Optional<ImageIcon> icon(String name) {
        return icon(DResources.class, ICON_ROOT + name);
    }

    /**
     * Loads an icon relative to {@code anchor}, using the same rules as
     * {@link Class#getResource(String)}. Results are cached.
     *
     * @param anchor class whose loader and package resolve the path
     * @param path   resource path; a leading {@code /} makes it absolute
     * @return the icon, or empty if no such resource exists
     */
    public static Optional<ImageIcon> icon(Class<?> anchor, String path) {
        URL url = anchor.getResource(path);
        if (url == null) {
            return Optional.empty();
        }
        return Optional.of(CACHE.computeIfAbsent(url.toExternalForm(), k -> new ImageIcon(url)));
    }

    /** The default DFramework application icon. */
    public static ImageIcon appIcon() {
        return icon("app.gif").orElseThrow();
    }

    /**
     * Looks up a localised message from DFramework's resource bundle.
     *
     * @param key message key
     * @return the message for the default locale
     */
    public static String message(String key) {
        return ResourceBundle.getBundle(MESSAGES).getString(key);
    }
}
