package com.dframework.swing;

import java.awt.Frame;

/** The extended states a {@link DFrame} can be put in. */
public enum WindowState {

    NORMAL(Frame.NORMAL),
    MAXIMIZED(Frame.MAXIMIZED_BOTH),
    MINIMIZED(Frame.ICONIFIED),
    MAXIMIZED_HORIZONTAL(Frame.MAXIMIZED_HORIZ),
    MAXIMIZED_VERTICAL(Frame.MAXIMIZED_VERT);

    private final int frameState;

    WindowState(int frameState) {
        this.frameState = frameState;
    }

    /** The matching {@link Frame#setExtendedState(int)} bits. */
    public int frameState() {
        return frameState;
    }

    /**
     * Maps {@link Frame#getExtendedState()} bits to a state. Minimised wins over
     * maximised, since a minimised window is not visible either way.
     */
    public static WindowState fromFrameState(int extendedState) {
        if ((extendedState & Frame.ICONIFIED) != 0) {
            return MINIMIZED;
        }
        return switch (extendedState & Frame.MAXIMIZED_BOTH) {
            case Frame.MAXIMIZED_BOTH -> MAXIMIZED;
            case Frame.MAXIMIZED_HORIZ -> MAXIMIZED_HORIZONTAL;
            case Frame.MAXIMIZED_VERT -> MAXIMIZED_VERTICAL;
            default -> NORMAL;
        };
    }
}
