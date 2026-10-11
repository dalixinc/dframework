package com.dframework.fx.theme;

import java.util.function.Supplier;

import javafx.application.Application;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;

/**
 * The JavaFX themes DFramework knows: AtlantaFX's (the JavaFX counterpart to
 * FlatLaf) and JavaFX's own Modena and Caspian.
 */
public enum FxTheme {
    PRIMER_LIGHT("Primer Light", false, () -> new PrimerLight().getUserAgentStylesheet()),
    PRIMER_DARK("Primer Dark", true, () -> new PrimerDark().getUserAgentStylesheet()),
    NORD_LIGHT("Nord Light", false, () -> new NordLight().getUserAgentStylesheet()),
    NORD_DARK("Nord Dark", true, () -> new NordDark().getUserAgentStylesheet()),
    CUPERTINO_LIGHT("Cupertino Light", false, () -> new CupertinoLight().getUserAgentStylesheet()),
    CUPERTINO_DARK("Cupertino Dark", true, () -> new CupertinoDark().getUserAgentStylesheet()),
    DRACULA("Dracula", true, () -> new Dracula().getUserAgentStylesheet()),
    MODENA("Modena (JavaFX)", false, () -> Application.STYLESHEET_MODENA),
    CASPIAN("Caspian (JavaFX 2)", false, () -> Application.STYLESHEET_CASPIAN);

    private final String displayName;
    private final boolean dark;
    private final Supplier<String> stylesheet;

    FxTheme(String displayName, boolean dark, Supplier<String> stylesheet) {
        this.displayName = displayName;
        this.dark = dark;
        this.stylesheet = stylesheet;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isDark() {
        return dark;
    }

    /** Whether this is an AtlantaFX theme, which understands AtlantaFX's colour variables and states. */
    public boolean isAtlantaFx() {
        return this != MODENA && this != CASPIAN;
    }

    String stylesheet() {
        return stylesheet.get();
    }
}
