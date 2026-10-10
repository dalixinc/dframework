package com.dframework.time;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.Serial;

import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * An editable, syntax-coloured code area for the examples. Lines don't wrap,
 * colours follow the theme (light or dark), and Page Up / Page Down are left
 * for the presentation clicker rather than scrolling the code.
 */
final class CodeArea extends JTextPane {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient boolean highlightPending;

    CodeArea() {
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleHighlight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleHighlight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // attribute changes are our own highlighting
            }
        });
        letKeyThrough(this);
    }

    /** Removes Page Up / Page Down from a text component, so they reach the view's lesson navigation. */
    static void letKeyThrough(JTextPane pane) {
        pane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "none");
        pane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "none");
    }

    /** Replaces the code, scrolled to the top. */
    void setCode(String code) {
        setText(code);
        setCaretPosition(0);
        highlight();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // No wrapping: wide lines scroll sideways instead.
        return getParent() == null || getUI().getPreferredSize(this).width <= getParent().getWidth();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (getDocument() != null) {
            SwingUtilities.invokeLater(this::highlight);   // the theme changed: recolour
        }
    }

    private void scheduleHighlight() {
        if (!highlightPending) {
            highlightPending = true;
            // Not during the document event: attributes can't be changed while it's being delivered.
            SwingUtilities.invokeLater(() -> {
                highlightPending = false;
                highlight();
            });
        }
    }

    private void highlight() {
        StyledDocument doc = getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }
        boolean dark = isDark(getBackground());
        SimpleAttributeSet plain = new SimpleAttributeSet();
        StyleConstants.setForeground(plain, getForeground());
        StyleConstants.setBold(plain, false);
        StyleConstants.setItalic(plain, false);
        doc.setCharacterAttributes(0, text.length(), plain, true);
        for (JavaSyntax.Span span : JavaSyntax.spans(text)) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            switch (span.kind()) {
                case KEYWORD -> {
                    StyleConstants.setForeground(a, dark ? new Color(0xCC7832) : new Color(0x0033B3));
                    StyleConstants.setBold(a, true);
                }
                case TYPE -> StyleConstants.setForeground(a, dark ? new Color(0x6FAFBD) : new Color(0x0E7C86));
                case STRING -> StyleConstants.setForeground(a, dark ? new Color(0x6A8759) : new Color(0x067D17));
                case NUMBER -> StyleConstants.setForeground(a, dark ? new Color(0x6897BB) : new Color(0x1750EB));
                case COMMENT -> {
                    StyleConstants.setForeground(a, dark ? new Color(0x808080) : new Color(0x8C8C8C));
                    StyleConstants.setItalic(a, true);
                }
            }
            doc.setCharacterAttributes(span.start(), span.length(), a, false);
        }
    }

    static boolean isDark(Color c) {
        Color color = c != null ? c : UIManager.getColor("TextPane.background");
        return color != null && (color.getRed() + color.getGreen() + color.getBlue()) / 3 < 128;
    }
}
