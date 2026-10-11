/** DFramework for JavaFX (spike). */
module com.dframework.fx {
    requires transitive javafx.controls;
    requires atlantafx.base;
    requires java.prefs;
    requires transitive com.dframework.swing;   // spike only: Validator, InputFilter, CloseAction, DFramework

    exports com.dframework.fx;
    exports com.dframework.fx.theme;
    exports com.dframework.fx.field;
}
