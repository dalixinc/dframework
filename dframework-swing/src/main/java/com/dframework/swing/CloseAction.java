package com.dframework.swing;

/** What a {@link DFrame} does when the user clicks its close button. */
public enum CloseAction {

    /** Exit the application immediately. */
    EXIT,
    /** Ask first, then exit the application. (The 2012 {@code QCLOSE}.) */
    CONFIRM_EXIT,
    /** Dispose of the window immediately. */
    DISPOSE,
    /** Ask first, then dispose of the window. (The 2012 {@code DCLOSE}.) */
    CONFIRM_DISPOSE,
    /** Hide the window, keeping it for reuse. */
    HIDE,
    /** Do nothing; the application handles closing itself. */
    NOTHING
}
