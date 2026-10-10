package com.dframework.swing.dialog;

import java.util.Locale;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.dframework.swing.util.DResources;

/**
 * Chooses the icons shown on {@link javax.swing.JOptionPane} dialogs across the
 * whole application.
 *
 * <p>The icons are installed as {@link UIManager} overrides, so they apply to
 * every option pane, including plain {@code JOptionPane.showMessageDialog}
 * calls and {@link com.dframework.swing.close.ConfirmCloser}, and they survive
 * theme changes.
 *
 * <pre>{@code
 * MessageIcons.use(MessageIcons.Style.ANIMATED);   // spinning road signs
 * MessageIcons.use(MessageIcons.Style.LOOK_AND_FEEL);   // back to the theme's own
 * }</pre>
 */
public final class MessageIcons {

    /** A set of dialog icons. */
    public enum Style {
        /** Whatever the current look and feel provides. */
        LOOK_AND_FEEL,
        /** DFramework's classic animated GIFs. Eye-catching; perhaps best kept for demos. */
        ANIMATED
    }

    private static volatile Style current = Style.LOOK_AND_FEEL;

    private MessageIcons() {
    }

    /** Switches every option pane to {@code style}. Affects dialogs opened afterwards. */
    public static void use(Style style) {
        Objects.requireNonNull(style, "style");
        for (MessageType type : MessageType.values()) {
            UIManager.put(type.uiKey(), style == Style.ANIMATED ? animated(type) : null);
        }
        current = style;
    }

    /** The style most recently passed to {@link #use(Style)}. */
    public static Style current() {
        return current;
    }

    /**
     * Overrides the icon for one message type, e.g. to put your own logo on
     * information dialogs. {@code null} reverts that type to the look and feel.
     */
    public static void set(MessageType type, Icon icon) {
        UIManager.put(type.uiKey(), icon);
    }

    /** The icon dialogs of {@code type} will currently show. */
    public static Icon get(MessageType type) {
        return UIManager.getIcon(type.uiKey());
    }

    /** DFramework's animated icon for {@code type}, for one-off use on a single dialog. */
    public static ImageIcon animated(MessageType type) {
        String name = "animated/" + type.name().toLowerCase(Locale.ROOT) + ".gif";
        return DResources.icon(name).orElseThrow();
    }
}
