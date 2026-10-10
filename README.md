# DFramework

A Swing metaframework that extends the standard Swing components to give a
better experience out of the box. First written in 2001, revived in 2026 for
Java 21.

## Modules

| Module | Contents |
|---|---|
| `dframework-swing` | The library (`com.dframework.swing`) |
| `dframework-demo` | Showcase app: Outlook-bar navigation, live theme switcher, validated fields |

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

- **`DFrame`**: a `JFrame` with a fluent builder, centring, simple
  window-state control (`maximize()`, `minimize()`, `restore()`) and a
  configurable `CloseAction`: `EXIT`, `CONFIRM_EXIT`, `DISPOSE`,
  `CONFIRM_DISPOSE`, `HIDE` or `NOTHING`.
  - **Window icons:** an application-wide default
    (`DFrame.setDefaultIcon("com/myapp/logo.png")`), per-frame
    `setIcon(...)` from a classpath resource, file path, `Image` or `Icon`,
    and `useDefaultIcon()` to go back.
  - **Themed title bars:** `lafDecorated(true)` lets FlatLaf, Metal or
    Kunststoff draw the title bar (which shows the icon, unlike GNOME's). It
    falls back to the OS title bar if you switch to a theme that can't.
- **`MessageIcons`**: one call, `MessageIcons.use(MessageIcons.Style.ANIMATED)`,
  puts DFramework's classic spinning road-sign icons on *every*
  `JOptionPane` in the app, including plain `JOptionPane.showMessageDialog`
  calls. They survive theme switches; `Style.LOOK_AND_FEEL` turns them off.
- **`DTextField`** (rebuilt from 2007): a text field with input filtering,
  live validation and a focus highlight.
  - **Input filtering:** `field.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)))`
    blocks keystrokes and pastes that would break the filter, with a beep.
    Filters: `digits`, `integer`, `maxLength`, `chars`, `regex`. Use a filter
    to keep bad input out, and a validator to judge the finished value.
  - **Validation:** `field.setValidator(Validator.required().and(Validator.email()))`.
    Errors show as a red outline with the message in a tooltip, but only once
    the user has visited the field. Listen for `INPUT_VALID_PROPERTY` to
    enable an OK button. Built-in validators: `required`, `email`, `integer`,
    `integer(min, max)`, `maxLength`, `matches(regex, message)`, or your own
    lambda.
  - **Focus highlight:** the background turns pale yellow while focused.
    Change it with `setFocusBackground(color)`, or pass `null` to turn it off.
    If the colour would make the text unreadable (on a dark theme, say), the
    text switches to black or white.
  - **`FocusHighlighter`** brings the same highlight to any component, or to
    every text field in the app with `FocusHighlighter.enableGlobally(color)`.
- **`DOutlookBar`** (rebuilt from 2007, after the VB6 Tracker 2 design): an
  Outlook-style navigation bar. Groups stack as headers, and the open
  group's items show as large icons with captions. `applyClassicStyle()`
  recreates the original Windows XP khaki look.
  ```java
  DOutlookBar bar = new DOutlookBar();
  bar.addGroup("Logging").addItem("Log Call", icon, e -> cards.show(main, "log"));
  bar.addGroup("Reports").addItem("Customer\nGroups", icon, e -> ...);
  ```
- **`DToggleSwitch`**: an animated on/off switch with an optional label.
  It's a `JToggleButton`, so `isSelected()`, listeners, `Action`s and
  keyboard all work as usual. Uses the theme's accent colour unless you call
  `setOnColor(...)`.
- **`DToast`**: non-blocking notifications that slide into a corner of the
  window and fade away. They stack, pause on hover and close on click.
  ```java
  DToast.success(frame, "Saved");
  DToast.builder(frame).type(DToast.Type.WARNING).title("Deleted 3 calls")
          .message("They are in the bin for 30 days.").action("Undo", this::undo).show();
  ```
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
