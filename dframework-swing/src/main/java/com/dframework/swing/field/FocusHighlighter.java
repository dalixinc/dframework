package com.dframework.swing.field;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;

/**
 * Temporarily changes a component's background while it has the keyboard
 * focus, so it is obvious where typing will go.
 *
 * <p>If the highlight would leave the text hard to read (yellow behind a dark
 * theme's white text, say), the foreground is swapped for black or white too.
 *
 * <pre>{@code
 * FocusHighlighter.install(myField, Color.YELLOW);           // one component
 * FocusHighlighter.enableGlobally(FocusHighlighter.getDefaultColor());   // every text field
 * }</pre>
 *
 * <p>{@link DTextField} does this out of the box; see
 * {@link DTextField#setFocusBackground(Color)}.
 */
public class FocusHighlighter implements FocusListener {

    /** Minimum WCAG contrast ratio before the foreground is swapped. */
    private static final double MIN_CONTRAST = 4.5;

    /** Client property a component can set to {@code false} to opt out of the global highlight. */
    public static final String GLOBAL_OPT_OUT = "DFramework.focusHighlight";

    private static volatile Color defaultColor = new Color(0xFFFF99);

    private static FocusHighlighter global;
    private static PropertyChangeListener globalListener;

    private Color color;
    private boolean autoContrast = true;

    private JComponent target;
    private Color savedBackground;
    private Color savedForeground;
    private LookAndFeel savedLookAndFeel;

    /** @param color the background to show while focused */
    public FocusHighlighter(Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /** Highlights {@code component} whenever it has focus, and returns the highlighter. */
    public static FocusHighlighter install(JComponent component, Color color) {
        FocusHighlighter highlighter = new FocusHighlighter(color);
        component.addFocusListener(highlighter);
        if (component.isFocusOwner()) {
            highlighter.highlight(component);
        }
        return highlighter;
    }

    /** Stops highlighting {@code component}, restoring its colours if it is highlighted now. */
    public void uninstall(JComponent component) {
        component.removeFocusListener(this);
        if (target == component) {
            restore();
        }
    }

    /** The colour {@link DTextField}s and {@link #enableGlobally} use unless told otherwise. A pale yellow. */
    public static Color getDefaultColor() {
        return defaultColor;
    }

    /** Changes the application-wide default highlight colour for fields created afterwards. */
    public static void setDefaultColor(Color color) {
        defaultColor = Objects.requireNonNull(color, "color");
    }

    /**
     * Highlights every editable text component in the application as it gains
     * focus, including plain {@code JTextField}s you have not touched. Calling
     * it again just changes the colour. {@link DTextField}s keep their own
     * setting, and any component can opt out with
     * {@code putClientProperty(FocusHighlighter.GLOBAL_OPT_OUT, false)}.
     */
    public static synchronized void enableGlobally(Color color) {
        if (global != null) {
            global.setColor(color);
            return;
        }
        global = new FocusHighlighter(color);
        globalListener = e -> {
            global.restore();
            if (e.getNewValue() instanceof JTextComponent c
                    && !(c instanceof DTextField)
                    && !Boolean.FALSE.equals(c.getClientProperty(GLOBAL_OPT_OUT))) {
                global.highlight(c);
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addPropertyChangeListener("permanentFocusOwner", globalListener);
    }

    /** Turns off {@link #enableGlobally}. */
    public static synchronized void disableGlobally() {
        if (global == null) {
            return;
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removePropertyChangeListener("permanentFocusOwner", globalListener);
        global.restore();
        global = null;
        globalListener = null;
    }

    public Color getColor() {
        return color;
    }

    /** Changes the highlight colour, updating the highlighted component immediately. */
    public void setColor(Color color) {
        this.color = Objects.requireNonNull(color, "color");
        if (target != null) {
            JComponent current = target;
            restore();
            highlight(current);
        }
    }

    public boolean isAutoContrast() {
        return autoContrast;
    }

    /** Whether to swap the text colour for black or white when it would be hard to read. Default on. */
    public void setAutoContrast(boolean autoContrast) {
        this.autoContrast = autoContrast;
    }

    /** Applies the highlight to {@code component}, unless it is disabled or read-only. */
    public void highlight(JComponent component) {
        if (target != null) {
            restore();
        }
        if (!component.isEnabled() || component instanceof JTextComponent text && !text.isEditable()) {
            return;
        }
        target = component;
        savedBackground = component.getBackground();
        savedForeground = component.getForeground();
        savedLookAndFeel = UIManager.getLookAndFeel();
        component.setBackground(color);
        if (autoContrast && contrast(savedForeground, color) < MIN_CONTRAST) {
            component.setForeground(contrast(Color.BLACK, color) >= contrast(Color.WHITE, color) ? Color.BLACK : Color.WHITE);
        }
    }

    /** Puts back the colours of the currently highlighted component, if any. */
    public void restore() {
        JComponent component = target;
        if (component == null) {
            return;
        }
        target = null;
        boolean themeChanged = UIManager.getLookAndFeel() != savedLookAndFeel;
        if (themeChanged && savedBackground instanceof UIResource) {
            // The saved colours belong to the old theme; let the new one reinstall its own.
            component.setBackground(null);
            component.setForeground(null);
            component.updateUI();
        } else {
            component.setBackground(savedBackground);
            component.setForeground(savedForeground);
        }
    }

    /** Whether this highlighter is currently colouring {@code component}. */
    public boolean isHighlighting(Component component) {
        return target == component;
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (e.getComponent() instanceof JComponent component) {
            highlight(component);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (!e.isTemporary()) {
            restore();
        }
    }

    /** WCAG 2 contrast ratio between two colours, from 1 to 21. */
    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        return 0.2126 * channel(c.getRed()) + 0.7152 * channel(c.getGreen()) + 0.0722 * channel(c.getBlue());
    }

    private static double channel(int value) {
        double v = value / 255.0;
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }
}
