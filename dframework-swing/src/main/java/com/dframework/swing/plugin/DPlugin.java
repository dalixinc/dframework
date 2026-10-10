package com.dframework.swing.plugin;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * A self-contained application that a DFramework host can display, in the
 * spirit of an ActiveX control: the host knows only this interface, and the
 * plugin brings its own user interface and behaviour.
 *
 * <p>Plugins are found with {@link java.util.ServiceLoader}. To ship one, put
 * the implementation in a jar along with a file
 * {@code META-INF/services/com.dframework.swing.plugin.DPlugin} naming the class.
 * On the classpath, it is then picked up by {@link DPluginHost#discover()}
 * with no change to the host application. The class needs a public no-argument
 * constructor.
 *
 * <p>Each instance is one running copy of the app. Its view is created lazily,
 * the first time the user opens it, and always on the event dispatch thread.
 */
public interface DPlugin {

    /**
     * A stable, unique identifier such as {@code "com.dframework.dig"}. It
     * names the plugin's saved settings and its audit records, so it shouldn't
     * change between versions. It must not contain {@code /}.
     */
    String id();

    /** Display name, e.g. "DNS Dig". */
    String name();

    /** One line saying what the plugin does. */
    default String description() {
        return "";
    }

    /** Icon for navigation, ideally 32×32; {@code null} for none. */
    default Icon icon() {
        return null;
    }

    /** Navigation group the host shows the plugin in. Default "Plugins". */
    default String category() {
        return "Plugins";
    }

    /**
     * Builds the plugin's user interface. Called once, on the event dispatch
     * thread, when the user first opens the plugin. If it throws, the host
     * shows the error in place of the plugin and carries on.
     */
    JComponent createView(PluginContext context);

    /** Called each time the plugin's view becomes visible. */
    default void onShow() {
    }

    /** Called each time the plugin's view is hidden. */
    default void onHide() {
    }

    /** Called when the host shuts the plugin down; release processes, threads and files here. */
    default void dispose() {
    }
}
