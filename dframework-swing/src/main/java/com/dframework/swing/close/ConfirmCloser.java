package com.dframework.swing.close;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import com.dframework.swing.util.DResources;

/**
 * Asks the user to confirm before running a close action, such as exiting the
 * application or disposing of a window.
 *
 * <p>It can be registered as a {@link java.awt.event.WindowListener} (to react
 * to the window's close button) and as an {@link ActionListener} (for an "Exit"
 * menu item or button), so all the ways of closing go through the same prompt.
 *
 * <pre>{@code
 * ConfirmCloser closer = ConfirmCloser.exitApplication(frame);
 * frame.addWindowListener(closer);
 * exitMenuItem.addActionListener(closer);
 * }</pre>
 *
 * <p>Replaces the 2012 {@code QCloser} (exit) and {@code DCloser} (dispose).
 */
public class ConfirmCloser extends WindowAdapter implements ActionListener {

    /** Shows the confirmation and returns whether the user agreed. */
    @FunctionalInterface
    public interface Prompt {
        boolean confirm(Component parent, String title, String message, Icon icon);
    }

    /** The default prompt: a Yes/No {@link JOptionPane}. */
    public static final Prompt OPTION_PANE = (parent, title, message, icon) ->
            JOptionPane.showConfirmDialog(parent, message, title,
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION;

    private final Component parent;
    private final Runnable onConfirm;
    private String title;
    private String message;
    private Icon icon;
    private Prompt prompt = OPTION_PANE;

    /**
     * @param parent    component the dialog is centred on; may be {@code null}
     * @param title     dialog title
     * @param message   question to ask
     * @param onConfirm what to do when the user says yes
     */
    public ConfirmCloser(Component parent, String title, String message, Runnable onConfirm) {
        this.parent = parent;
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
    }

    /** A closer that terminates the JVM once confirmed. */
    public static ConfirmCloser exitApplication(Component parent) {
        return new ConfirmCloser(parent,
                DResources.message("close.exit.title"),
                DResources.message("close.exit.message"),
                () -> System.exit(0));
    }

    /** A closer that disposes of {@code window} once confirmed. */
    public static ConfirmCloser disposeWindow(Window window) {
        Objects.requireNonNull(window, "window");
        return new ConfirmCloser(window,
                DResources.message("close.dispose.title"),
                DResources.message("close.dispose.message"),
                window::dispose);
    }

    /**
     * Shows the prompt and runs the close action if confirmed.
     *
     * @return {@code true} if the user confirmed
     */
    public boolean requestClose() {
        if (!prompt.confirm(parent, title, message, icon)) {
            return false;
        }
        onConfirm.run();
        return true;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        requestClose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        requestClose();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "title");
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = Objects.requireNonNull(message, "message");
    }

    /** The dialog icon, or {@code null} to use the look and feel's question icon. */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Sets the dialog icon. Pass {@code null} (the default) to use the look and
     * feel's own question icon, or e.g. {@code DResources.icon("question.gif")}
     * for the classic DFramework one.
     */
    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    /** Replaces how the confirmation is asked; useful for custom dialogs and tests. */
    public void setPrompt(Prompt prompt) {
        this.prompt = Objects.requireNonNull(prompt, "prompt");
    }
}
