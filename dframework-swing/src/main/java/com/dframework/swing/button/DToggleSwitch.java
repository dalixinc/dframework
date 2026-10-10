package com.dframework.swing.button;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.io.Serial;

import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * An on/off switch: a rounded track with a sliding knob, and an optional label
 * to its right.
 *
 * <p>It is a {@link JToggleButton}, so {@code isSelected()}, item and action
 * listeners, {@code Action}s, keyboard (Space) and accessibility all work as
 * usual. The knob slides when toggled, and colours follow the theme's accent
 * colour unless set with {@link #setOnColor(Color)}.
 *
 * <pre>{@code
 * DToggleSwitch wifi = new DToggleSwitch("Wi-Fi", true);
 * wifi.addItemListener(e -> network.setEnabled(wifi.isSelected()));
 * }</pre>
 */
public class DToggleSwitch extends JToggleButton {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int ANIMATION_MS = 140;
    private static final int TEXT_GAP = 8;
    private static final Color FALLBACK_ACCENT = new Color(0x2F80ED);

    private Color onColor;
    private boolean animated = true;
    /** Knob position from 0 (off) to 1 (on). */
    private float position;
    private transient Timer timer;

    public DToggleSwitch() {
        this(null, false);
    }

    public DToggleSwitch(String text) {
        this(text, false);
    }

    public DToggleSwitch(String text, boolean selected) {
        super(text, selected);
        position = selected ? 1f : 0f;
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        addItemListener(e -> slideTo(e.getStateChange() == ItemEvent.SELECTED ? 1f : 0f));
    }

    /** Track colour when on, or {@code null} for the theme's accent colour. */
    public Color getOnColor() {
        return onColor;
    }

    public void setOnColor(Color onColor) {
        Color old = this.onColor;
        this.onColor = onColor;
        firePropertyChange("onColor", old, onColor);
        repaint();
    }

    public boolean isAnimated() {
        return animated;
    }

    /** Whether the knob slides; turn off for reduced-motion preferences. Default on. */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    /** Knob position from 0 (off) to 1 (on); between the two while sliding. */
    public float getKnobPosition() {
        return position;
    }

    private void slideTo(float target) {
        if (timer != null) {
            timer.stop();
        }
        if (!animated || !isShowing()) {
            position = target;
            repaint();
            return;
        }
        float start = position;
        long began = System.nanoTime();
        timer = new Timer(15, e -> {
            float t = Math.min(1f, (System.nanoTime() - began) / (ANIMATION_MS * 1_000_000f));
            float eased = 1 - (1 - t) * (1 - t);
            position = start + (target - start) * eased;
            repaint();
            if (t >= 1f) {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        FontMetrics fm = getFontMetrics(getFont());
        int trackHeight = trackHeight(fm);
        int width = trackHeight * 9 / 5;
        int height = Math.max(trackHeight, fm.getHeight());
        String text = getText();
        if (text != null && !text.isEmpty()) {
            width += TEXT_GAP + fm.stringWidth(text);
        }
        Insets insets = getInsets();
        return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            Insets insets = getInsets();
            int trackHeight = trackHeight(fm);
            int trackWidth = trackHeight * 9 / 5;
            int contentHeight = getHeight() - insets.top - insets.bottom;
            int x = insets.left;
            int y = insets.top + (contentHeight - trackHeight) / 2;

            Color foreground = color("Label.foreground", Color.BLACK);
            Color background = getParent() != null ? getParent().getBackground() : Color.WHITE;
            Color offTrack = blend(background, foreground, 0.35f);
            Color onTrack = onColor != null ? onColor : accentColor();
            Color track = blend(offTrack, onTrack, position);
            if (!isEnabled()) {
                track = blend(background, track, 0.45f);
            }

            g2.setColor(track);
            g2.fillRoundRect(x, y, trackWidth, trackHeight, trackHeight, trackHeight);

            int pad = Math.max(2, trackHeight / 8);
            int knob = trackHeight - pad * 2;
            int knobX = x + pad + Math.round((trackWidth - knob - pad * 2) * position);
            if (getModel().isRollover() && isEnabled()) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillOval(knobX - 1, y + pad - 1, knob + 2, knob + 2);
            }
            g2.setColor(isEnabled() ? Color.WHITE : blend(background, Color.WHITE, 0.7f));
            g2.fillOval(knobX, y + pad, knob, knob);

            if (isFocusOwner()) {
                g2.setColor(blend(onTrack, background, 0.35f));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x - 1, y - 1, trackWidth + 1, trackHeight + 1, trackHeight + 2, trackHeight + 2);
            }

            String text = getText();
            if (text != null && !text.isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? foreground : color("Label.disabledForeground", Color.GRAY));
                int textY = insets.top + (contentHeight - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x + trackWidth + TEXT_GAP, textY);
            }
        } finally {
            g2.dispose();
        }
    }

    private static int trackHeight(FontMetrics fm) {
        return Math.max(16, Math.round(fm.getHeight() * 1.1f));
    }

    private static Color accentColor() {
        for (String key : new String[] {"Component.accentColor", "ProgressBar.foreground", "textHighlight"}) {
            Color c = UIManager.getColor(key);
            if (c != null) {
                return c;
            }
        }
        return FALLBACK_ACCENT;
    }

    private static Color color(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private static Color blend(Color from, Color to, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * a),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * a),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * a));
    }
}
