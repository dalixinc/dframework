package com.dframework.time;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.UIManager;

import com.dframework.time.Lesson.Era;

/**
 * The tour's eras as stations on a line: 1996, 1997, 2005, 2014, then GPS.
 * Eras already covered are filled, the current one is highlighted, and
 * clicking a station jumps to it.
 */
final class EraTimeline extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Era[] ERAS = Era.values();

    private transient Era current = Era.DATE;
    private transient Consumer<Era> onChoose = era -> { };
    private float scale = 1f;

    EraTimeline() {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onChoose.accept(eraAt(e.getX()));
            }
        });
    }

    void setOnChoose(Consumer<Era> onChoose) {
        this.onChoose = onChoose;
    }

    void setCurrent(Era era) {
        current = era;
        repaint();
    }

    void setScale(float scale) {
        this.scale = scale;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, Math.round(58 * scale));
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Era era = eraAt(e.getX());
        return era.year() + ", " + era.label() + ": " + era.summary();
    }

    private Era eraAt(int x) {
        int best = 0;
        for (int i = 1; i < ERAS.length; i++) {
            if (Math.abs(x - xOf(i)) < Math.abs(x - xOf(best))) {
                best = i;
            }
        }
        return ERAS[best];
    }

    private int xOf(int i) {
        int margin = Math.round(70 * scale);
        return margin + (getWidth() - 2 * margin) * i / (ERAS.length - 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color text = getForeground() != null ? getForeground() : Color.DARK_GRAY;
            Color muted = UIManager.getColor("Label.disabledForeground") != null
                    ? UIManager.getColor("Label.disabledForeground") : Color.GRAY;
            Color accent = UIManager.getColor("Component.accentColor") != null
                    ? UIManager.getColor("Component.accentColor") : new Color(0x3B82F6);
            Font base = UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font") : getFont();
            Font yearFont = base.deriveFont(Font.BOLD, base.getSize2D() * scale);
            Font labelFont = base.deriveFont(base.getSize2D() * 0.95f * scale);

            int lineY = Math.round(26 * scale);
            int r = Math.round(6 * scale);
            g2.setStroke(new BasicStroke(2f * scale));
            g2.setColor(muted);
            g2.drawLine(xOf(0), lineY, xOf(ERAS.length - 1), lineY);
            g2.setColor(accent);
            g2.drawLine(xOf(0), lineY, xOf(current.ordinal()), lineY);

            for (int i = 0; i < ERAS.length; i++) {
                Era era = ERAS[i];
                int x = xOf(i);
                boolean isCurrent = era == current;
                boolean done = i <= current.ordinal();
                int radius = isCurrent ? r + Math.round(2 * scale) : r;
                g2.setColor(getBackground() != null ? getBackground() : Color.WHITE);
                g2.fillOval(x - radius, lineY - radius, 2 * radius, 2 * radius);
                g2.setColor(done ? accent : muted);
                if (done) {
                    g2.fillOval(x - radius, lineY - radius, 2 * radius, 2 * radius);
                } else {
                    g2.drawOval(x - radius, lineY - radius, 2 * radius, 2 * radius);
                }

                g2.setFont(yearFont);
                g2.setColor(isCurrent ? accent : done ? text : muted);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(era.year(), x - fm.stringWidth(era.year()) / 2, lineY - radius - Math.round(5 * scale));

                g2.setFont(isCurrent ? labelFont.deriveFont(Font.BOLD) : labelFont);
                fm = g2.getFontMetrics();
                g2.setColor(isCurrent ? text : muted);
                g2.drawString(era.label(), x - fm.stringWidth(era.label()) / 2, lineY + radius + fm.getAscent() + Math.round(3 * scale));
            }
        } finally {
            g2.dispose();
        }
    }
}
