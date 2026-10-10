# DFramework

A Swing metaframework that extends the standard Swing components to give a
better experience out of the box. First written in 2001, revived in 2026 for
Java 21.

## Modules

| Module | Contents |
|---|---|
| `dframework-swing` | The library (`com.dframework.swing`) |
| `dframework-demo` | Showcase app with a live theme switcher |

A JavaFX module is planned.

## Quick start

```java
ThemeManager.apply(DTheme.FLAT_LIGHT);

SwingUtilities.invokeLater(() -> DFrame.builder()
        .title("My App")
        .closeAction(CloseAction.CONFIRM_EXIT)   // asks "Are you sure?" first
        .content(new MyPanel())
        .size(800, 600)                          // omit to pack; centred either way
        .show());
```

### What's in the box

- **`DFrame`**: a `JFrame` with a default icon, a fluent builder, centring,
  simple window-state control (`maximize()`, `minimize()`, `restore()`) and a
  configurable `CloseAction`: `EXIT`, `CONFIRM_EXIT`, `DISPOSE`,
  `CONFIRM_DISPOSE`, `HIDE` or `NOTHING`.
- **`ConfirmCloser`**: an "are you sure?" prompt that works as both a
  `WindowListener` and an `ActionListener`, so the close button and a
  *File → Exit* menu item ask the same question.
- **`DTheme` / `ThemeManager`**: installs FlatLaf (Light, Dark, IntelliJ,
  Darcula, macOS Light/Dark), the JDK look and feels (System, Metal, Ocean,
  Nimbus, GTK+, Motif, Windows, Aqua) and Kunststoff. Switching theme at
  runtime restyles every open window. `DTheme.available()` lists what works on
  the current platform.
- **`DResources`**: classpath-based loading of icons and localised messages.

## Building

Requires JDK 21 (`JAVA_HOME` must point at it).

```sh
./gradlew build                      # compile, test, javadoc
./gradlew :dframework-demo:run       # launch the showcase
```

`DFrameTest` needs a display and skips itself when headless.

## Third-party look and feels

[Kunststoff](http://www.incors.org/) (2004) isn't on Maven Central, so it is
vendored in `libs/` and added to the demo's runtime classpath. To use it in
your own app, put `libs/kunststoff.jar` on the classpath; `DTheme.KUNSTSTOFF`
becomes available automatically.

## History

The original 2001–2012 sources (`DFrame`, `QCloser`, `DCloser`,
`DLookAndFeelManager`, `KnownLookAndFeels`, …) are preserved in the commit
*"Import original 2012 DFramework sources"* and in `dframework.zip`.
