# DFramework

A Swing metaframework that extends the standard Swing components to give a
better experience out of the box. First written in 2001, revived in 2026 for
Java 21.

## Documentation

- **[User Guide](docs/USER_GUIDE.md)**: what DFramework does and how to use
  every component, with examples.
- **[Technical Manual](docs/TECHNICAL_MANUAL.md)**: architecture, design
  decisions, the traps we hit, and conventions for contributors.

Both are also published together as the *DFramework Handbook*, a pair of
web pages generated from these files. To regenerate them after editing the
docs:

```sh
python3 docs/site/build_handbook.py   # writes build/handbook/index.html and technical-manual.html
```

The script needs the Python Markdown package. If it isn't installed, the
script downloads the pinned release itself and verifies its checksum, so
`pip` isn't required. The guide's Java examples are compiled by the
build (`UserGuideExamplesTest`), so they can't drift from the API.

## Modules

| Module | Contents |
|---|---|
| `dframework-swing` | The library (`com.dframework.swing`) |
| `dframework-demo` | Showcase app: Outlook-bar navigation, live theme switcher, validated fields |
| `dframework-dig` | **DNS Dig**, a plugin: run `dig` lookups and see exactly what runs |
| `dframework-ai` | Claude-backed `AiAssistant` (Anthropic Java SDK), found at runtime; powers DNS Dig's **Explain with AI** |

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
  - **Remembers where you left it:** `.rememberBounds("myapp.main")` saves
    position, size and maximised state between runs. If the saved spot is no
    longer on a connected monitor, the window opens in its default position
    instead. `WindowMemory` does the same for any window or dialog.
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
- **`DTable`** / **`DTablePanel`**: a `JTable` that's ready to use, with
  sorting, striped rows, an "empty" message and `packColumns()`. It also
  filters live as you type, highlighting matches in the cells. The filter
  syntax:
  - words: `acme printer` (all must match)
  - phrases: `"printer fault"`
  - exclusions: `-closed`
  - single columns: `status:open` or `pri:high`

  `DTablePanel` adds a search box (Ctrl+F) and a "12 of 300 rows" count:
  ```java
  DTablePanel calls = new DTablePanel(new DTable(callModel));
  ```
- **`DSearchField`**: a search box with a magnifier, a clear button, Esc to
  clear, and debounced `onSearch(...)` callbacks.
- **`DBusyPanel`**: wraps any component in a busy overlay. While it's up,
  the panel is dimmed and shows a spinner (or a progress ring when the work
  reports a percentage) and a message. Mouse and keyboard input to that
  panel is blocked; the rest of the window stays usable.
  ```java
  DBusyPanel busy = new DBusyPanel(callsScreen);
  busy.run("Loading calls…", progress -> repository.load(progress::setProgress), table::setModel);
  ```
  The work runs on a background thread, and the result is passed back on the
  Swing event thread. Esc can cancel it. The overlay waits briefly before
  appearing, so quick jobs don't flicker.
- **`DBreadcrumbBar`**: a breadcrumb trail such as
  `Home › Reports › Customers`. Each earlier crumb is a link back to its
  level. When the bar is too narrow, middle crumbs fold into a "…" menu.
  `push`, `pop` and `setPath` build the trail; `addCrumbListener` reports
  clicks.
- **`DCommandPalette`**: <kbd>Ctrl+Shift+P</kbd> or <kbd>F1</kbd> opens a searchable list
  of everything the app can do, as in modern editors. Fuzzy matching means
  `fdark` finds *Theme › FlatLaf Dark*. Recently used commands come first.
  It picks up every menu bar item, with its shortcut, so one line gives an
  existing app a palette:
  ```java
  DCommandPalette.install(frame)
          .addOutlookBar(nav)                                  // "Go to" every screen
          .addCommand(Command.of("Data", "Refresh", this::refresh));
  ```
- **Plugins** (`DPlugin`, `DPluginHost`): self-contained apps that a host
  finds on the classpath and displays, in the spirit of ActiveX controls.
  Each starts only when first opened, a plugin that fails is contained in
  its own pane, and each gets its own saved settings and audit records.
  **DNS Dig** is the first. Plugins can also use an AI assistant
  (`AiAssistant`); with `dframework-ai` and an `ANTHROPIC_API_KEY`, DNS Dig
  can have Claude explain a result in plain English.
- **`AuditLog`**: an append-only JSON Lines record of what the application
  did, in `~/.dframework/audit.log`, readable by its owner only.
- **`DCollapsiblePane`**: a titled section that folds away and remembers
  whether it was open.
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
