package com.dframework.swing.plugin;

import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JComponent;

import com.dframework.swing.audit.AuditLog;
import com.dframework.swing.command.Command;

/** What the host provides to a running {@link DPlugin}. */
public interface PluginContext {

    /** The plugin this context belongs to. */
    DPlugin plugin();

    /** The component hosting the plugin; use it as the owner of dialogs and toasts. */
    JComponent component();

    /** The application's audit log. */
    AuditLog auditLog();

    /** Records an audit entry with this plugin's id as the source. */
    default AuditLog.Entry audit(String action, Map<String, ?> details) {
        return auditLog().record(plugin().id(), action, details);
    }

    /** This plugin's own saved settings, kept between runs. */
    Preferences preferences();

    /** Adds a command to the window's command palette, if it has one. */
    void addCommand(Command command);
}
