package com.dframework.time;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.dframework.swing.command.Command;
import com.dframework.swing.plugin.DPlugin;
import com.dframework.swing.plugin.PluginContext;

/**
 * Time in Java, a DFramework plugin for talks: a walk through Java's date and
 * time APIs, from java.util.Date (1996) through Calendar and Joda-Time to
 * java.time (2014), then on to leap seconds, GPS time and relativity. Each
 * lesson sets an old pitfall beside its fix, and every example is compiled and
 * run live, so the code on screen is exactly what runs.
 */
public final class TimePlugin implements DPlugin {

    private TimeView view;

    @Override
    public String id() {
        return "com.dframework.time";
    }

    @Override
    public String name() {
        return "Time in Java";
    }

    @Override
    public String description() {
        return "Why time is hard: Java's date and time APIs from 1996 to today, then GPS and Einstein, run live.";
    }

    @Override
    public Icon icon() {
        return new ImageIcon(clock(32));
    }

    @Override
    public JComponent createView(PluginContext context) {
        view = new TimeView(context);
        context.addCommand(Command.of("Time in Java", "Next Lesson", view::nextLesson));
        context.addCommand(Command.of("Time in Java", "Previous Lesson", view::previousLesson));
        context.addCommand(Command.of("Time in Java", "Run Both Examples", view::runBothExamples));
        context.addCommand(Command.of("Time in Java", "Presenter Mode On or Off", view::togglePresenterMode));
        context.addCommand(Command.of("Time in Java", "Show or Hide Under the Hood", view::toggleUnderTheHood));
        if (view.canExplain()) {
            context.addCommand(Command.of("Time in Java", "Explain Last Example with AI", view::explainLast));
        }
        return view;
    }

    @Override
    public void onShow() {
        view.warmUp();
        view.onShow();
    }

    @Override
    public void dispose() {
        if (view != null) {
            view.dispose();
        }
    }

    /** A clock face at ten past ten, drawn rather than loaded: there was no clock in the 2007 icon set. */
    static BufferedImage clock(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            double s = size / 32.0;
            double c = size / 2.0;
            Ellipse2D face = new Ellipse2D.Double(2 * s, 2 * s, 28 * s, 28 * s);
            g.setColor(new Color(0xFFF8E1));
            g.fill(face);
            g.setStroke(new BasicStroke((float) (2.4 * s)));
            g.setColor(new Color(0x8D6E63));
            g.draw(face);
            g.setColor(new Color(0x5D4037));
            for (int h = 0; h < 12; h++) {
                double a = Math.toRadians(h * 30);
                double inner = h % 3 == 0 ? 9.5 : 11;
                g.setStroke(new BasicStroke((float) ((h % 3 == 0 ? 1.8 : 1.1) * s)));
                g.draw(new Line2D.Double(c + Math.sin(a) * inner * s, c - Math.cos(a) * inner * s,
                        c + Math.sin(a) * 12.5 * s, c - Math.cos(a) * 12.5 * s));
            }
            g.setStroke(new BasicStroke((float) (2.2 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            hand(g, c, Math.toRadians(10 * 30 + 5), 6.5 * s);   // hour hand, just past ten
            g.setStroke(new BasicStroke((float) (1.6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            hand(g, c, Math.toRadians(2 * 30), 10 * s);          // minute hand on two
            g.setColor(new Color(0xD9534F));
            g.setStroke(new BasicStroke((float) (0.9 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            hand(g, c, Math.toRadians(6 * 30 + 12), 11 * s);     // second hand
            g.fill(new Ellipse2D.Double(c - 1.6 * s, c - 1.6 * s, 3.2 * s, 3.2 * s));
        } finally {
            g.dispose();
        }
        return image;
    }

    private static void hand(Graphics2D g, double c, double angle, double length) {
        g.draw(new Line2D.Double(c, c, c + Math.sin(angle) * length, c - Math.cos(angle) * length));
    }
}
