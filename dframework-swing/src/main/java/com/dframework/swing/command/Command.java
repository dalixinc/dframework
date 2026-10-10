package com.dframework.swing.command;

import java.awt.event.ActionEvent;
import java.util.Objects;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * Something the user can run from a {@link DCommandPalette}.
 *
 * @param name     what the user searches for and sees, e.g. "FlatLaf Dark"
 * @param category optional grouping shown before the name, e.g. "Theme"; may be {@code null}
 * @param icon     optional icon; may be {@code null}
 * @param shortcut optional keyboard shortcut to display; may be {@code null}
 * @param checked  for on/off commands, whether it is currently on; {@code null} otherwise
 * @param action   what to do when chosen
 */
public record Command(String name, String category, Icon icon, KeyStroke shortcut, Boolean checked, Runnable action) {

    public Command {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");
    }

    /** A command with just a name. */
    public static Command of(String name, Runnable action) {
        return new Command(name, null, null, null, null, action);
    }

    /** A command with a category, e.g. {@code Command.of("Edit", "Undo", this::undo)}. */
    public static Command of(String category, String name, Runnable action) {
        return new Command(name, category, null, null, null, action);
    }

    /**
     * A command that runs a Swing {@link Action}, using its name, small icon
     * and accelerator.
     */
    public static Command of(String category, Action action) {
        return new Command(
                String.valueOf(action.getValue(Action.NAME)),
                category,
                (Icon) action.getValue(Action.SMALL_ICON),
                (KeyStroke) action.getValue(Action.ACCELERATOR_KEY),
                null,
                () -> action.actionPerformed(new ActionEvent(action, ActionEvent.ACTION_PERFORMED,
                        String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)))));
    }

    /** The text matched against the query: category then name. */
    String searchText() {
        return category == null ? name : category + " › " + name;
    }
}
