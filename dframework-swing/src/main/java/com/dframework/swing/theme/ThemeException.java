package com.dframework.swing.theme;

import java.io.Serial;

/** Thrown when a {@link DTheme} cannot be installed. */
public class ThemeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ThemeException(String message, Throwable cause) {
        super(message, cause);
    }
}
