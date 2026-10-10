package com.dframework.dig;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.dframework.swing.command.Command;
import com.dframework.swing.plugin.DPlugin;
import com.dframework.swing.plugin.PluginContext;

/**
 * DNS Dig, a DFramework plugin: look up DNS records with the {@code dig}
 * command, see the parsed answers and the raw output, and see (and copy)
 * exactly which command runs. Every run is recorded in the audit log.
 *
 * <p>Registered in {@code META-INF/services/com.dframework.swing.plugin.DPlugin},
 * so any DFramework host with this jar on its classpath finds it.
 */
public final class DigPlugin implements DPlugin {

    private DigView view;

    @Override
    public String id() {
        return "com.dframework.dig";
    }

    @Override
    public String name() {
        return "DNS Dig";
    }

    @Override
    public String description() {
        return "Look up DNS records with dig, and see exactly what runs.";
    }

    @Override
    public Icon icon() {
        return new ImageIcon(DigPlugin.class.getResource("dig.gif"));
    }

    @Override
    public JComponent createView(PluginContext context) {
        view = new DigView(context);
        context.addCommand(Command.of("DNS Dig", "Run Lookup", view::run));
        context.addCommand(Command.of("DNS Dig", "Copy Command", view::copyCommand));
        context.addCommand(Command.of("DNS Dig", "Show or Hide Under the Hood", view::toggleUnderTheHood));
        return view;
    }

    @Override
    public void onShow() {
        view.focusName();
    }

    @Override
    public void dispose() {
        if (view != null) {
            view.dispose();
        }
    }
}
