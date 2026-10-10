package com.dframework.swing.dialog;

import javax.swing.JOptionPane;

/** The {@link JOptionPane} message types that carry an icon. */
public enum MessageType {

    QUESTION(JOptionPane.QUESTION_MESSAGE, "OptionPane.questionIcon"),
    INFORMATION(JOptionPane.INFORMATION_MESSAGE, "OptionPane.informationIcon"),
    WARNING(JOptionPane.WARNING_MESSAGE, "OptionPane.warningIcon"),
    ERROR(JOptionPane.ERROR_MESSAGE, "OptionPane.errorIcon");

    private final int optionPaneType;
    private final String uiKey;

    MessageType(int optionPaneType, String uiKey) {
        this.optionPaneType = optionPaneType;
        this.uiKey = uiKey;
    }

    /** The matching {@code JOptionPane.*_MESSAGE} constant. */
    public int optionPaneType() {
        return optionPaneType;
    }

    /** The {@link javax.swing.UIManager} key the look and feel reads the icon from. */
    public String uiKey() {
        return uiKey;
    }
}
