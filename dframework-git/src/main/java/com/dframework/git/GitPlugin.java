package com.dframework.git;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.dframework.swing.command.Command;
import com.dframework.swing.plugin.DPlugin;
import com.dframework.swing.plugin.PluginContext;

/**
 * Git Browser, a DFramework plugin: explore a git repository through common
 * read-only commands and its raw objects (commits, trees, blobs, tags), seeing
 * and auditing every git command run, with an optional AI explanation.
 *
 * <p>Read-only by design: there's a fixed menu of commands that can't change
 * the repository, no way to type an arbitrary one, and git runs with
 * {@code GIT_OPTIONAL_LOCKS=0} so even status refreshes aren't written.
 */
public final class GitPlugin implements DPlugin {

    private GitView view;

    @Override
    public String id() {
        return "com.dframework.git";
    }

    @Override
    public String name() {
        return "Git Browser";
    }

    @Override
    public String description() {
        return "Explore a git repository, read-only, and see every command that runs.";
    }

    @Override
    public Icon icon() {
        return new ImageIcon(GitPlugin.class.getResource("git.gif"));
    }

    @Override
    public JComponent createView(PluginContext context) {
        view = new GitView(context);
        context.addCommand(Command.of("Git Browser", "Open Repository", view::openRepository));
        context.addCommand(Command.of("Git Browser", "Run Command", view::runPreset));
        context.addCommand(Command.of("Git Browser", "Copy Command", view::copyCommand));
        context.addCommand(Command.of("Git Browser", "Show or Hide Under the Hood", view::toggleUnderTheHood));
        if (view.canExplain()) {
            context.addCommand(Command.of("Git Browser", "Explain Output with AI", view::explainLast));
        }
        return view;
    }

    @Override
    public void onShow() {
        view.openDefaultRepository();
        view.focusFirstField();
    }

    @Override
    public void dispose() {
        if (view != null) {
            view.dispose();
        }
    }
}
