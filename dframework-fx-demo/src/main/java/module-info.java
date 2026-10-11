/** The JavaFX showcase for DFramework (spike). */
module com.dframework.fxdemo {
    requires com.dframework.fx;
    requires java.prefs;
    requires atlantafx.base;

    // JavaFX creates the Application subclass reflectively.
    exports com.dframework.fxdemo to javafx.graphics;
}
