package com.dframework.swing.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JComponent;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.command.Command;
import com.dframework.swing.command.DCommandPalette;
import com.dframework.swing.nav.DOutlookBar;

/**
 * Finds {@link DPlugin}s and hosts them.
 *
 * <pre>{@code
 * DPluginHost host = new DPluginHost();
 * for (DPlugin plugin : DPluginHost.discover()) {
 *     DPluginPane pane = host.mount(plugin);
 *     cards.add(pane, plugin.id());
 * }
 * // ... on exit
 * host.disposeAll();
 * }</pre>
 *
 * <p>Or let it fill an Outlook bar: {@link #addToOutlookBar} adds each plugin
 * to the group named by its {@link DPlugin#category()}.
 */
public final class DPluginHost {

    private static final System.Logger LOG = System.getLogger(DPluginHost.class.getName());

    private final AuditLog auditLog;
    private final Preferences settingsRoot;
    private final List<DPluginPane> panes = new ArrayList<>();

    /** A host recording to {@link AuditLog#getDefault()}. */
    public DPluginHost() {
        this(AuditLog.getDefault());
    }

    /** A host keeping plugin settings under {@code com/dframework/plugins} in the user's preferences. */
    public DPluginHost(AuditLog auditLog) {
        this(auditLog, Preferences.userRoot().node("com/dframework/plugins"));
    }

    /**
     * @param settingsRoot where each plugin's settings node is created (one child per plugin id);
     *                     tests pass a throwaway node so they never touch real settings
     */
    public DPluginHost(AuditLog auditLog, Preferences settingsRoot) {
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
        this.settingsRoot = Objects.requireNonNull(settingsRoot, "settingsRoot");
    }

    /** Plugins on the classpath, sorted by category then name. */
    public static List<DPlugin> discover() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return discover(loader != null ? loader : DPluginHost.class.getClassLoader());
    }

    /**
     * Plugins visible to {@code loader}, sorted by category then name. A plugin
     * that can't be loaded is logged and skipped, so one broken jar can't stop
     * the others.
     */
    public static List<DPlugin> discover(ClassLoader loader) {
        List<DPlugin> found = new ArrayList<>();
        ServiceLoader.load(DPlugin.class, loader).stream().forEach(provider -> {
            try {
                DPlugin plugin = provider.get();
                validateId(plugin.id());
                found.add(plugin);
            } catch (ServiceConfigurationError | RuntimeException | LinkageError e) {
                LOG.log(System.Logger.Level.ERROR, "Skipping plugin " + provider.type().getName(), e);
            }
        });
        found.sort(Comparator.comparing(DPlugin::category).thenComparing(DPlugin::name, String.CASE_INSENSITIVE_ORDER));
        return found;
    }

    /** Creates a pane hosting {@code plugin}. Its view is created when the pane is first shown. */
    public DPluginPane mount(DPlugin plugin) {
        validateId(plugin.id());
        DPluginPane pane = new DPluginPane(plugin, Context::new);
        panes.add(pane);
        return pane;
    }

    /** Every pane this host has mounted. */
    public List<DPluginPane> getPanes() {
        return Collections.unmodifiableList(panes);
    }

    /**
     * Mounts each plugin and adds it to {@code bar}, in the group named by its
     * category (created if missing). Choosing the item calls {@code show} with
     * the plugin's pane, typically to switch a card layout to it.
     *
     * @return the panes, in the order given
     */
    public List<DPluginPane> addToOutlookBar(DOutlookBar bar, List<DPlugin> plugins, Consumer<DPluginPane> show) {
        Map<String, DOutlookBar.Group> groups = new LinkedHashMap<>();
        bar.getGroups().forEach(g -> groups.put(g.getTitle(), g));
        List<DPluginPane> mounted = new ArrayList<>();
        for (DPlugin plugin : plugins) {
            DPluginPane pane = mount(plugin);
            DOutlookBar.Group group = groups.computeIfAbsent(plugin.category(), bar::addGroup);
            group.addItem(plugin.name(), plugin.icon(), e -> show.accept(pane));
            mounted.add(pane);
        }
        return mounted;
    }

    /** Shuts every started plugin down, e.g. when the application exits. */
    public void disposeAll() {
        panes.forEach(DPluginPane::dispose);
    }

    private static void validateId(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.length() > 80) {
            throw new IllegalArgumentException("Plugin id must be 1-80 characters without '/': " + id);
        }
    }

    /** The context handed to each plugin. */
    private final class Context implements PluginContext {

        private final DPlugin plugin;
        private final DPluginPane pane;

        Context(DPlugin plugin, DPluginPane pane) {
            this.plugin = plugin;
            this.pane = pane;
        }

        @Override
        public DPlugin plugin() {
            return plugin;
        }

        @Override
        public JComponent component() {
            return pane;
        }

        @Override
        public AuditLog auditLog() {
            return auditLog;
        }

        @Override
        public Preferences preferences() {
            return settingsRoot.node(plugin.id());
        }

        @Override
        public void addCommand(Command command) {
            DCommandPalette.of(pane).ifPresent(palette -> palette.addCommand(command));
        }
    }
}
