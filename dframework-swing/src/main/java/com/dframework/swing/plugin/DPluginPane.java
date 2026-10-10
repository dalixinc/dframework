package com.dframework.swing.plugin;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.HierarchyEvent;
import java.io.Serial;
import java.util.Map;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/**
 * Hosts one {@link DPlugin}: a slim header with the plugin's icon, name and
 * description, and the plugin's own view beneath it.
 *
 * <p>The view is created the first time the pane is shown, so unused plugins
 * cost nothing. If the plugin fails while starting, the pane shows what went
 * wrong instead, and the rest of the application is unaffected. Created by
 * {@link DPluginHost#mount(DPlugin)}.
 */
public class DPluginPane extends JPanel {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final System.Logger LOG = System.getLogger(DPluginPane.class.getName());

    private final transient DPlugin plugin;
    private final transient PluginContext context;
    private final JPanel header = new JPanel(new BorderLayout(8, 0));
    private JComponent view;
    private boolean started;
    private boolean failed;
    private boolean showing;

    DPluginPane(DPlugin plugin, PluginContextFactory contexts) {
        super(new BorderLayout());
        this.plugin = plugin;
        this.context = contexts.create(plugin, this);

        JLabel title = new JLabel(plugin.name(), small(plugin.icon()), SwingConstants.LEADING);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() * 1.15f));
        title.setIconTextGap(8);
        JLabel description = new JLabel(plugin.description());
        description.setEnabled(false);
        header.add(title, BorderLayout.WEST);
        header.add(description, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        add(header, BorderLayout.NORTH);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                visibilityChanged(isShowing());
            }
        });
    }

    /** Creates the plugin's view and hands it the context; the factory is the host's. */
    @FunctionalInterface
    interface PluginContextFactory {
        PluginContext create(DPlugin plugin, DPluginPane pane);
    }

    public DPlugin getPlugin() {
        return plugin;
    }

    public PluginContext getContext() {
        return context;
    }

    /** Whether the plugin's view has been created (successfully or not). */
    public boolean isStarted() {
        return started;
    }

    /** Whether the plugin threw while creating its view. */
    public boolean isFailed() {
        return failed;
    }

    /** The plugin's view, once started. */
    public Optional<JComponent> getView() {
        return Optional.ofNullable(failed ? null : view);
    }

    /** Shows or hides the name-and-description header. */
    public void setHeaderVisible(boolean visible) {
        header.setVisible(visible);
    }

    /** Starts the plugin now rather than waiting for it to be shown. */
    public void start() {
        if (started) {
            return;
        }
        started = true;
        try {
            view = plugin.createView(context);
            if (view == null) {
                throw new IllegalStateException(plugin.getClass().getName() + ".createView returned null");
            }
            context.audit("start", Map.of("plugin", plugin.name()));
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            LOG.log(System.Logger.Level.ERROR, "Plugin " + plugin.id() + " failed to start", e);
            context.audit("start-failed", Map.of("plugin", plugin.name(), "error", String.valueOf(e)));
            view = failureView(e);
        }
        add(view, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Shuts the plugin down. The host calls this; it is safe to call twice. */
    void dispose() {
        if (started && !failed) {
            try {
                plugin.dispose();
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Plugin " + plugin.id() + " failed while disposing", e);
            }
            context.audit("stop", Map.of("plugin", plugin.name()));
        }
        failed = true;   // no further callbacks
    }

    private void visibilityChanged(boolean nowShowing) {
        if (nowShowing == showing) {
            return;
        }
        showing = nowShowing;
        if (nowShowing) {
            start();
        }
        if (failed) {
            return;
        }
        try {
            if (nowShowing) {
                plugin.onShow();
            } else {
                plugin.onHide();
            }
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Plugin " + plugin.id() + " failed in " + (nowShowing ? "onShow" : "onHide"), e);
        }
    }

    private JComponent failureView(Throwable error) {
        JTextArea text = new JTextArea("⚠  " + plugin.name() + " could not start.\n\n" + error
                + "\n\nThe rest of the application is unaffected. Details are in the log.");
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return new JScrollPane(text);
    }

    private static Icon small(Icon icon) {
        if (icon instanceof ImageIcon image && (icon.getIconWidth() > 20 || icon.getIconHeight() > 20)) {
            return new ImageIcon(image.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        }
        return icon;
    }
}
