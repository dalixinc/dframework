# The DFramework User Guide

> **Swing, with a better experience out of the box.**
> First written in 2001, rebuilt in 2026 for Java 21.

DFramework is a set of ready-made extensions to Java Swing. Standard Swing
gives you powerful building blocks with dated defaults. DFramework gives you
the same building blocks with sensible ones: modern themes, confirmation on
close, windows that remember where you left them, text fields that validate
and filter as you type, searchable tables, notifications, and more. Each
component is a normal Swing component, so it fits into existing code.

```java
ThemeManager.apply(DTheme.FLAT_LIGHT);

SwingUtilities.invokeLater(() -> DFrame.builder()
        .title("Tracker 2")
        .closeAction(CloseAction.CONFIRM_EXIT)
        .rememberBounds("tracker2.main")
        .content(new DTablePanel(new DTable(callModel)))
        .show());
```

That's a themed, confirm-before-exit window that reopens where you left it,
containing a sortable, searchable table with a live row count.

---

## Contents

1. [Getting started](#1-getting-started)
2. [The showcase demo](#2-the-showcase-demo)
3. [Windows: `DFrame`](#3-windows-dframe)
4. [Themes: `DTheme` and `ThemeManager`](#4-themes-dtheme-and-thememanager)
5. [Closing safely: `ConfirmCloser`](#5-closing-safely-confirmcloser)
6. [Dialog icons: `MessageIcons`](#6-dialog-icons-messageicons)
7. [Text input: `DTextField`, `Validator`, `InputFilter`](#7-text-input-dtextfield-validator-inputfilter)
8. [Focus highlighting: `FocusHighlighter`](#8-focus-highlighting-focushighlighter)
9. [Tables: `DTable`, `DTablePanel`, `DSearchField`](#9-tables-dtable-dtablepanel-dsearchfield)
10. [Navigation: `DOutlookBar`](#10-navigation-doutlookbar)
11. [Navigation: `DBreadcrumbBar`](#11-navigation-dbreadcrumbbar)
12. [The command palette: `DCommandPalette`](#12-the-command-palette-dcommandpalette)
13. [Notifications: `DToast`](#13-notifications-dtoast)
14. [Switches: `DToggleSwitch`](#14-switches-dtoggleswitch)
15. [Background work: `DBusyPanel`](#15-background-work-dbusypanel)
16. [Resources: `DResources`](#16-resources-dresources)
17. [Putting it together](#17-putting-it-together)
18. [Tips, rules and FAQ](#18-tips-rules-and-faq)

---

## 1. Getting started

### Requirements

- **Java 21** or later
- **Gradle** is optional: the project includes the Gradle wrapper (`./gradlew`)

### Build it

```sh
git clone https://github.com/dalixinc/dframework.git
cd dframework
export JAVA_HOME=/path/to/jdk-21
./gradlew build                   # compile, run the tests, build the javadoc
./gradlew :dframework-demo:run    # launch the showcase
```

### Use it in your own project

DFramework isn't on Maven Central yet. Publish it to your local Maven
repository:

```sh
./gradlew :dframework-swing:publishToMavenLocal
```

Then depend on it:

**Gradle (Kotlin DSL)**
```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation("com.dframework:dframework-swing:4.0.0-SNAPSHOT")
}
```

**Maven**
```xml
<dependency>
    <groupId>com.dframework</groupId>
    <artifactId>dframework-swing</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

DFramework's only dependency is [FlatLaf](https://www.formdev.com/flatlaf/),
which comes along automatically.

### Your first window

```java
import com.dframework.swing.CloseAction;
import com.dframework.swing.DFrame;
import com.dframework.swing.theme.DTheme;
import com.dframework.swing.theme.ThemeManager;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class Hello {
    public static void main(String[] args) {
        ThemeManager.apply(DTheme.FLAT_LIGHT);
        SwingUtilities.invokeLater(() -> DFrame.builder()
                .title("Hello, DFramework")
                .closeAction(CloseAction.CONFIRM_EXIT)
                .content(new JLabel("Welcome back!", JLabel.CENTER))
                .size(400, 200)
                .show());
    }
}
```

Run it and try closing the window. You'll be asked first.

---

## 2. The showcase demo

`./gradlew :dframework-demo:run` opens a tour of the whole framework. The
**Outlook bar** on the left switches between screens:

| Group | Screen | Shows |
|---|---|---|
| **Showcase** | Controls | Standard Swing components under the current theme, plus a row of `DToggleSwitch`es, including a working **Dark mode** switch |
| | Fields | `DTextField` validation, input filtering and focus colours. Submit shows a busy overlay, then a toast |
| | Toasts | Every `DToast` type, position and option |
| | Table | A 300-call log in a `DTablePanel`. Try `printer -closed pri:high` |
| | Notes | A plain text area |
| **Tracker 2** | My Calls | The call log pre-filtered to `engineer:dale -closed`. **Refresh** shows a cancellable progress ring; double-click a call to drill in |
| | Log Call, Open Call, Time Sheet | Placeholders honouring the 2005 VB6 original |
| **Admin** | Users, Settings, Backup, About | Placeholders, and an About box with a spinning sign |

Things to try:

- **Theme menu**: switch between 13 themes, live. Tick *Classic Outlook Bar*
  for the Windows XP look of Tracker 2.
- **Dialogs menu**: the classic animated road-sign icons on real
  `JOptionPane`s.
- **Window Icon menu**: aliens, skulls, a burger, a flying saucer…
- **Ctrl+Shift+P**: the command palette. Type `fdark`, `go toast` or
  `forget`.
- **The breadcrumb** above the screens: drill into a call, then click your
  way back.
- **Move and resize the window, then quit and relaunch**: it comes back
  where you left it.

---

## 3. Windows: `DFrame`

`DFrame` is a `JFrame` with better defaults:

- the DFramework icon
- it exits the application when closed
- it's centred on screen
- maximise, minimise and restore are one call each

### The builder

```java
DFrame frame = DFrame.builder()
        .title("Tracker 2")
        .icon("com/myapp/icons/logo.png")      // classpath resource or file path
        .closeAction(CloseAction.CONFIRM_EXIT)
        .windowState(WindowState.MAXIMIZED)
        .size(1000, 700)                        // or .bounds(rect); omit both to pack
        .menuBar(menuBar)
        .content(mainPanel)
        .lafDecorated(true)                     // themed title bar (see below)
        .rememberBounds("tracker2.main")        // reopen where the user left it
        .build();                               // or .show() to build and display
```

Every option is optional. Without `size` or `bounds`, the frame is packed
to fit its content. Unless you give exact `bounds`, it's centred on screen.

### Close actions

| `CloseAction` | When the user clicks ✕ |
|---|---|
| `EXIT` *(default)* | The application exits |
| `CONFIRM_EXIT` | "Are you sure you want to shut down?", then exit |
| `DISPOSE` | The window closes; the application carries on |
| `CONFIRM_DISPOSE` | "Are you sure you want to close this window?", then close |
| `HIDE` | The window hides, ready to show again |
| `NOTHING` | Nothing; your code decides |

Change it at any time with `frame.setCloseAction(...)`. Your own window
listeners are never disturbed.

For the confirming actions, `frame.getCloser()` returns the
[`ConfirmCloser`](#5-closing-safely-confirmcloser). Attach it to your
*File → Exit* item so both ways of quitting ask the same question:

```java
frame.getCloser().ifPresent(exitMenuItem::addActionListener);
```

### Window state

```java
frame.maximize();
frame.minimize();
frame.restore();
frame.setWindowState(WindowState.MAXIMIZED_VERTICAL);
WindowState now = frame.getWindowState();   // read live from the window
```

If the platform can't maximise, `MAXIMIZED` falls back to filling the
usable screen area.

### Window icons

```java
DFrame.setDefaultIcon("com/myapp/logo.png");   // every new DFrame, application-wide
frame.setIcon("./icons/alien10.gif");           // this frame: resource or file path
frame.setIcon(someSwingIcon);                   // any Icon
frame.useDefaultIcon();                         // back to the application default
```

A missing image throws `IllegalArgumentException` naming the path, so typos
don't fail silently.

### Remembering position and size

```java
frame.rememberBounds("tracker2.main");    // returns true if saved bounds were restored
```

The window's position, size and maximised state are saved as the user moves
it, and again on close, and restored next time. A few safeguards:

- **Unplugged monitors:** if the saved position is on a monitor that's no
  longer connected, the window opens at its default position instead.
- **Minimised:** a window never reopens minimised.
- **Maximised:** while maximised, the *normal* size is what's remembered,
  so un-maximising next time goes back to the right size.

`frame.getWindowMemory().ifPresent(WindowMemory::forget)` clears what was
saved. For dialogs and other windows, use `WindowMemory` directly:

```java
WindowMemory.install(myDialog, "tracker2.callDialog").restore();
```

> Use ids unique to your application, such as `"tracker2.main"`. Values are
> stored with Java Preferences under `com/dframework/windows/<id>`.

### Themed title bars

`lafDecorated(true)` lets the theme draw the window's title bar instead of
the operating system. With FlatLaf you get a title bar that matches the
theme and shows your icon. That matters on desktops like GNOME, which don't
show window icons at all.

If the user switches to a theme that can't draw title bars (GTK+, Nimbus,
Motif), the frame switches back to the system title bar, and returns to the
themed one when a capable theme comes back.

---

## 4. Themes: `DTheme` and `ThemeManager`

```java
ThemeManager.apply(DTheme.FLAT_DARK);
```

A Swing look and feel applies to the whole application, so themes are
managed in one place. Switching at runtime restyles **every open window**
immediately.

| Family | Themes |
|---|---|
| FlatLaf (bundled) | `FLAT_LIGHT`, `FLAT_DARK`, `FLAT_INTELLIJ`, `FLAT_DARCULA`, `FLAT_MAC_LIGHT`, `FLAT_MAC_DARK` |
| Built into the JDK | `SYSTEM`, `METAL`, `OCEAN`, `NIMBUS`, `MOTIF`, `GTK` (Linux), `WINDOWS` (Windows), `AQUA` (macOS) |
| Third party | `KUNSTSTOFF`, when `kunststoff.jar` is on the classpath |

```java
DTheme.available();                                     // what works on this machine
ThemeManager.applyOrFallback(DTheme.KUNSTSTOFF, DTheme.FLAT_LIGHT);
ThemeManager.current();                                 // Optional<DTheme>
ThemeManager.addThemeListener(theme -> status.setText(theme.displayName()));
theme.isDark();                                         // e.g. to pick matching icons
```

`apply` works from any thread, because it does the work on the Swing event
thread itself. If a theme isn't available it throws `ThemeException`
rather than failing silently.

### A theme menu in five lines

```java
for (DTheme theme : DTheme.available()) {
    JRadioButtonMenuItem item = new JRadioButtonMenuItem(theme.displayName());
    item.addActionListener(e -> ThemeManager.apply(theme));
    group.add(item);
    themeMenu.add(item);
}
```

### Kunststoff

Kunststoff (2004) isn't on Maven Central. A copy is in the repository's
`libs/` folder. Put it on your runtime classpath and `DTheme.KUNSTSTOFF`
becomes available.

---

## 5. Closing safely: `ConfirmCloser`

`ConfirmCloser` asks "are you sure?" before closing. It works both as a
window listener (the ✕ button) and as an action listener (menu items,
buttons):

```java
ConfirmCloser closer = ConfirmCloser.exitApplication(frame);   // or disposeWindow(window)
frame.addWindowListener(closer);
exitMenuItem.addActionListener(closer);
```

Customise it:

```java
closer.setTitle("Leaving so soon?");
closer.setMessage("Unsaved calls will be lost. Quit anyway?");
closer.setIcon(DResources.icon("question.gif").orElseThrow());   // classic icon; null = theme's
closer.requestClose();                                             // ask now, from code
```

Pass a `Runnable` to the constructor to do anything you like once the
user confirms:

```java
new ConfirmCloser(frame, "Log out", "Log out of Tracker 2?", session::logout);
```

> `DFrame` sets this up for you with `CloseAction.CONFIRM_EXIT` or
> `CONFIRM_DISPOSE`.

---

## 6. Dialog icons: `MessageIcons`

DFramework's classic **animated road-sign icons**, back from 2001:

| Message type | Icon |
|---|---|
| Question | a spinning "?" yield sign |
| Information | a spinning "INFO" sign |
| Warning | a spinning "!" stop sign |
| Error | a spinning skull-and-crossbones "DANGER" sign |

One line switches every `JOptionPane` in the application over to them,
including ones you already have:

```java
MessageIcons.use(MessageIcons.Style.ANIMATED);       // wow
MessageIcons.use(MessageIcons.Style.LOOK_AND_FEEL);  // back to the theme's icons
```

They stay in place when the theme changes. They're a bit much for
everyday use, but they make an impression in a demo.

```java
MessageIcons.set(MessageType.INFORMATION, myLogo);   // replace just one type
MessageIcons.animated(MessageType.ERROR);            // one icon, for a single dialog
```

---

## 7. Text input: `DTextField`, `Validator`, `InputFilter`

`DTextField` is a `JTextField` with three extras: **input filtering**,
**validation** and a **focus highlight**.

```java
DTextField age = new DTextField(6);
age.setInputFilter(InputFilter.digits().and(InputFilter.maxLength(3)));   // what can be typed
age.setValidator(Validator.integer(0, 130));                              // what is acceptable
```

### Filters keep bad input out

An `InputFilter` blocks any keystroke, paste or drop that would break it,
with a beep. A paste containing any bad character is rejected entirely.

| Filter | Allows |
|---|---|
| `InputFilter.digits()` | 0-9 only |
| `InputFilter.integer()` | digits with an optional leading minus |
| `InputFilter.maxLength(n)` | up to *n* characters |
| `InputFilter.chars("ABC…")` | only the listed characters |
| `InputFilter.regex("[A-Za-z0-9 ]*")` | text matching the pattern |

Combine with `.and(...)`, or write your own: `text -> text.length() < 10`.

> **Regex filters see partly-typed text.** Write `"[A-Z]{0,3}"`, not
> `"[A-Z]{3}"`, or the first letter will be rejected.

### Validators judge the result

A `Validator` decides whether the text is *acceptable*. You need both,
because a filter can't reject "200" for an age: "2" and "20" are valid
steps on the way to typing it.

| Validator | Accepts |
|---|---|
| `Validator.required()` | anything but blank |
| `Validator.email()` | `name@example.com`-style addresses |
| `Validator.integer()` | whole numbers, ASCII digits only |
| `Validator.integer(min, max)` | whole numbers in range |
| `Validator.maxLength(n)` | up to *n* characters |
| `Validator.matches(regex, message)` | text matching the pattern |
| `Validator.of(predicate, message)` | anything you can test |

Chain them with `.and(...)`. The first failure's message wins:

```java
email.setValidator(Validator.required().and(Validator.email()));
```

Every built-in except `required()` accepts **empty** text, so optional
fields are only checked once the user types something.

### How errors appear

Errors don't show the moment a form opens. They appear once the user has
visited a field and left it, as a **red outline** with the message in a
**tooltip**. After that, they update live as the user types.

```java
field.isInputValid();             // the current verdict
field.getValidationMessage();     // Optional<String>
field.validateInput();            // check now and show errors, e.g. when OK is pressed
field.resetValidation();          // hide errors again, e.g. after clearing a form
```

### Enabling an OK button

```java
Runnable update = () -> ok.setEnabled(fields.stream().allMatch(DTextField::isInputValid));
fields.forEach(f -> f.addPropertyChangeListener(DTextField.INPUT_VALID_PROPERTY, e -> update.run()));
update.run();

ok.addActionListener(e -> {
    if (fields.stream().allMatch(DTextField::validateInput)) {
        save();
    }
});
```

### Focus highlight

Every `DTextField` turns **pale yellow** while it has focus, so it's always
clear where typing will go.

```java
field.setFocusBackground(new Color(0xC8F7DC));            // mint, for this field
field.setFocusBackground(null);                           // off
FocusHighlighter.setDefaultColor(new Color(0xCFE8FF));    // default for new fields
```

On dark themes the text switches to black automatically if it would be hard
to read on the highlight.

---

## 8. Focus highlighting: `FocusHighlighter`

The highlight is also available on its own, for any component:

```java
FocusHighlighter.install(myCombo, Color.YELLOW);
```

Or turn it on for **every text field in the application**, including plain
`JTextField`s in code you haven't touched:

```java
FocusHighlighter.enableGlobally(FocusHighlighter.getDefaultColor());
FocusHighlighter.disableGlobally();
```

To exclude a particular field from the global highlight:

```java
field.putClientProperty(FocusHighlighter.GLOBAL_OPT_OUT, false);
```

Read-only and disabled fields are never highlighted.

---

## 9. Tables: `DTable`, `DTablePanel`, `DSearchField`

### `DTable`

A `JTable` that's ready to use:

```java
DTable table = new DTable(callModel);   // any TableModel, or new DTable(rows, columnNames)
table.packColumns(320);                  // fit columns to their content, at most 320 px
```

- **Sorting:** click a header. Numbers, dates and enums sort by value, if
  your model's `getColumnClass` says what they are.
- **Striped rows**, adjusted for light and dark themes (`setStriped(false)`
  to turn off).
- **"No rows"** and **"No matching rows"** messages when there's nothing to
  show (`setEmptyText`, `setNoMatchesText`).

### Filtering

```java
table.setFilterText("acme printer -closed status:open");
```

| You type | Shows rows… |
|---|---|
| `acme printer` | containing *both* words, in any columns |
| `"printer fault"` | containing the phrase |
| `-closed` | **not** containing the word |
| `status:open` | with the word in the *Status* column |
| `pri:high` | column names can be shortened to any unique start |

Matching ignores case. Matches are highlighted in the cells. If a word
before a colon isn't a column name, the whole thing is searched as text,
so `10:30` and web addresses work.

```java
table.getMatchingRowCount();   // rows passing the filter
table.getTotalRowCount();      // all rows
table.addPropertyChangeListener(DTable.FILTER_TEXT_PROPERTY, e -> ...);
```

### `DTablePanel`

A table with a search box and row count above it, ready to drop in:

```java
DTablePanel calls = new DTablePanel(new DTable(callModel));
frame.add(calls);
```

- The search box filters as you type. <kbd>Ctrl+F</kbd> jumps to it and
  <kbd>Esc</kbd> clears it.
- The count reads "300 rows", or "12 of 300 rows" when filtered, and stays
  correct as the data changes.
- Setting the filter from code (`table.setFilterText(...)`) updates the
  search box too.

### `DSearchField`

The search box on its own, for use anywhere. It has a magnifier icon,
placeholder text and a clear button. It waits until the user pauses typing
before searching, so big lists aren't re-filtered on every keystroke:

```java
DSearchField search = new DSearchField("Search customers");
search.onSearch(query -> customerList.filter(query));
search.setSearchDelay(250);   // milliseconds; 0 = every keystroke
```

Enter searches straight away. <kbd>Esc</kbd> clears the box, or passes
through to the dialog when the box is already empty.

---

## 10. Navigation: `DOutlookBar`

An Outlook-style navigation bar, rebuilt from DFramework's 2007 Swing
version of the bar in the VB6 app *Tracker 2* (2005).

```java
DOutlookBar bar = new DOutlookBar();

DOutlookBar.Group logging = bar.addGroup("Logging");
logging.addItem("Log Call", logIcon, e -> show("log"));
logging.addItem("Time\nSheet", sheetIcon, e -> show("timesheet"));   // \n splits a caption

bar.addGroup("Reports").addItem(customersAction);   // or a Swing Action
bar.addGroup("Admin");

frame.add(bar, BorderLayout.WEST);
```

- Group headers stack. Clicking one opens it, and the headers after it drop
  to the bottom of the bar.
- Items show as large icons with captions underneath. They highlight on
  hover, and the selected one stays highlighted.
- Long groups scroll.

```java
bar.setSelectedItem(item);     // as if clicked: opens its group and runs its action
bar.getSelectedItem();         // Optional<Item>
bar.addPropertyChangeListener(DOutlookBar.SELECTED_ITEM_PROPERTY, e -> ...);
```

### Classic style

By default the colours follow the theme. For the full 2005 Windows XP look,
with white captions on khaki and beige headers (colours sampled from
Tracker 2 screenshots):

```java
bar.applyClassicStyle();
bar.applyThemeStyle();     // back to the theme
bar.setContentBackground(color);
bar.setItemForeground(color);
bar.setHeaderBackground(color);
```

---

## 11. Navigation: `DBreadcrumbBar`

A breadcrumb trail: `DFramework › Tracker 2 › My Calls › Call #1283`.
Each earlier crumb is a link back to its level.

```java
DBreadcrumbBar crumbs = new DBreadcrumbBar();
crumbs.setPath(Crumb.of("Home", homeScreen), Crumb.of("Reports", reportsScreen));
crumbs.push(Crumb.of("Customers", customersScreen));

crumbs.addCrumbListener(crumb -> show(crumb.value()));   // value: anything you like
```

- Clicking a crumb trims the trail back to it, then notifies your listeners
  (`setTruncateOnClick(false)` to manage the trail yourself).
- `pop()` goes up one level, which is handy for a *Back* button.
- `Crumb` takes an optional icon: `new Crumb("Home", homeIcon, homeScreen)`.
- When space runs out, the middle crumbs fold into a **"…"** menu, keeping
  the first and last visible.
- `setSeparator("/")` changes the `›`.

**Drill-down pattern:** push a crumb when opening a detail view, and return
to the list when it leaves the trail:

```java
Crumb crumb = Crumb.of("Call #" + call.number());
crumbs.push(crumb);
showDetail(call);

crumbs.addPropertyChangeListener(DBreadcrumbBar.PATH_PROPERTY, e -> {
    if (!crumbs.getPath().contains(crumb)) {
        showList();
    }
});
```

---

## 12. The command palette: `DCommandPalette`

Press <kbd>Ctrl+Shift+P</kbd>, type a few letters, press <kbd>Enter</kbd>,
just like in modern code editors.

```java
DCommandPalette.install(frame);
```

That one line includes **every item in the window's menu bar**, with its
menu path, keyboard shortcut and tick state. It reads the menus each time
it opens, so it's never out of date.

### Add more

```java
DCommandPalette palette = DCommandPalette.install(frame)
        .addOutlookBar(navBar)                                       // "Go to" every screen
        .addCommand(Command.of("Data", "Refresh Calls", this::refresh))
        .addCommand(Command.of("Edit", undoAction))                  // a Swing Action
        .addSource(() -> openDocumentCommands());                    // recomputed on each open
```

### Searching

Matching is **fuzzy**: the letters you type must appear in order, and
matches at the start of words and runs of letters count most.

| You type | Finds |
|---|---|
| `fdark` | Theme › **F**latLaf **Dark** |
| `go toast` | **Go** to › Showcase › **Toast**s |
| `kun` | Theme › **Kun**ststoff |

Matched letters are shown in bold. With an empty query, the last five
commands you used come first.

### Keys

| Key | Does |
|---|---|
| <kbd>Ctrl+Shift+P</kbd> | open / close |
| <kbd>↑</kbd> <kbd>↓</kbd> <kbd>PgUp</kbd> <kbd>PgDn</kbd> | move |
| <kbd>Enter</kbd> or click | run |
| <kbd>Esc</kbd> or click outside | close |

```java
palette.setKeyStroke(KeyStroke.getKeyStroke("F1"));   // a different shortcut
palette.setIncludeMenuBar(false);                     // only your own commands
palette.open();                                       // e.g. from a menu item
```

---

## 13. Notifications: `DToast`

Short messages that slide into a corner of the window and fade away by
themselves, without interrupting the user.

```java
DToast.success(frame, "Saved");
DToast.info(frame, "3 new calls");
DToast.warning(frame, "Disk nearly full");
DToast.error(frame, "Couldn't connect to tracker2sqlb");
```

With more options:

```java
DToast.builder(frame)
        .type(DToast.Type.WARNING)
        .title("Deleted 3 calls")
        .message("They are in the bin for 30 days.")
        .action("Undo", this::undoDelete)          // a button; closes the toast when clicked
        .position(DToast.Position.TOP_CENTER)
        .duration(Duration.ofSeconds(8))           // or .sticky() to keep it until closed
        .show();
```

- Several toasts stack neatly, sharing a width.
- Hovering pauses the countdown; clicking closes the toast.
- They're drawn inside the window, so they move with it.
- They can be shown from any thread.

```java
DToast.setDefaultPosition(DToast.Position.BOTTOM_CENTER);
DToast.setDefaultDuration(Duration.ofSeconds(5));
DToast.setAnimated(false);                 // for users who prefer reduced motion
```

---

## 14. Switches: `DToggleSwitch`

An on/off switch with a sliding knob:

```java
DToggleSwitch wifi = new DToggleSwitch("Wi-Fi", true);
wifi.addItemListener(e -> network.setEnabled(wifi.isSelected()));
```

It's a `JToggleButton` underneath, so `isSelected()`, item and action
listeners, Swing `Action`s, <kbd>Space</kbd> and accessibility all work as
usual. It uses the theme's accent colour:

```java
wifi.setOnColor(new Color(0x27AE60));   // a colour of your own
wifi.setAnimated(false);                 // no slide
```

> **Tip:** use an `ActionListener` when the switch drives something global,
> like dark mode. Action listeners only fire on real clicks, not when your
> own code calls `setSelected`, which avoids feedback loops.

---

## 15. Background work: `DBusyPanel`

Wrap a component so long-running work can cover it with a **busy overlay**.
While the overlay is up, the component is dimmed and shows a spinner (or a
progress ring) with a message. Mouse and keyboard input to it is blocked,
while the rest of the window stays usable.

```java
DBusyPanel busy = new DBusyPanel(callsScreen);
frame.add(busy);                                  // add busy where callsScreen would go

busy.run("Loading calls…",
        progress -> repository.loadCalls(),      // runs on a background thread
        calls -> table.setModel(calls));          // back on the Swing thread
```

### Progress, cancelling and errors

```java
busy.run("Importing…", true,                      // true = Esc cancels
        progress -> {
            for (int i = 0; i < rows.size(); i++) {
                if (progress.isCancelled()) {
                    return null;
                }
                importRow(rows.get(i));
                progress.setProgress(100 * i / rows.size());   // shows a ring with a percentage
                progress.setMessage("Importing row " + i);
            }
            return rows.size();
        },
        count -> DToast.success(frame, "Imported " + count + " rows"),
        error -> DToast.error(frame, "Import failed: " + error.getMessage()));
```

- **Cancelled:** a cancelled task calls neither handler.
- **No error handler:** if you pass `null`, errors are logged.

### Driving it by hand

```java
busy.start("Connecting…");
// ... later, on the Swing thread
busy.stop();
```

Starts can overlap. The overlay stays until every `start` has had its
`stop`. It also waits a moment (`setShowDelay`, default 200 ms) before
appearing, so quick jobs don't flash it.

---

## 16. Resources: `DResources`

Icons and text load from the **classpath**, so they work the same from an
IDE, a jar or an installer:

```java
DResources.load("com/myapp/icons/logo.png");   // classpath or file path → Optional<ImageIcon>
DResources.icon(MyClass.class, "icons/x.gif");  // relative to a class
DResources.appIcon();                            // the DFramework icon
```

DFramework's own text, such as dialog titles and validation messages, is in
`com/dframework/swing/messages.properties`. To translate it, add a
`messages_fr.properties` (or any language) with the same keys.

---

## 17. Putting it together

A small but complete application using most of the framework:

```java
public final class Tracker {

    public static void main(String[] args) {
        ThemeManager.applyOrFallback(DTheme.FLAT_LIGHT, DTheme.SYSTEM);
        SwingUtilities.invokeLater(Tracker::start);
    }

    private static void start() {
        DTable table = new DTable(new CallModel());
        table.packColumns(300);
        DTablePanel calls = new DTablePanel(table);
        DBusyPanel busy = new DBusyPanel(calls);

        CardLayout cards = new CardLayout();
        JPanel screens = new JPanel(cards);
        screens.add(busy, "calls");
        screens.add(new SettingsScreen(), "settings");

        DOutlookBar nav = new DOutlookBar();
        DOutlookBar.Group work = nav.addGroup("Work");
        work.addItem("Calls", DResources.load("icons/calls.png").orElse(null), e -> cards.show(screens, "calls"));
        work.addItem("Settings", DResources.load("icons/settings.png").orElse(null), e -> cards.show(screens, "settings"));

        JPanel root = new JPanel(new BorderLayout());
        root.add(nav, BorderLayout.WEST);
        root.add(screens, BorderLayout.CENTER);

        DFrame frame = DFrame.builder()
                .title("Tracker")
                .closeAction(CloseAction.CONFIRM_EXIT)
                .rememberBounds("tracker.main")
                .lafDecorated(true)
                .content(root)
                .size(1000, 650)
                .build();

        DCommandPalette.install(frame)
                .addOutlookBar(nav)
                .addCommand(Command.of("Data", "Refresh", () -> refresh(busy, table, frame)));

        frame.setVisible(true);
        refresh(busy, table, frame);
    }

    private static void refresh(DBusyPanel busy, DTable table, DFrame frame) {
        busy.run("Loading calls…", progress -> CallModel.load(), model -> {
            table.setModel(model);
            DToast.success(frame, model.getRowCount() + " calls loaded");
        });
    }
}
```

---

## 18. Tips, rules and FAQ

**Do I have to use the Swing event thread?**
Yes, as with all Swing: create and change components on the event dispatch
thread (`SwingUtilities.invokeLater`). The exceptions, which are safe from
any thread, are `ThemeManager.apply`, `DToast` and `DBusyPanel`'s
`run`/`start`/`stop`/`setProgress`/`setMessage`.

**Can I mix DFramework with plain Swing?**
Yes. Every DFramework component *is* a Swing component (`DFrame` is a
`JFrame`, `DTable` a `JTable`, `DToggleSwitch` a `JToggleButton`…), and
some features, such as `MessageIcons`, `FocusHighlighter.enableGlobally`
and the command palette's menu harvesting, improve plain Swing code you
haven't changed.

**Why does my number column sort as text?**
Your `TableModel` needs to report the column's type from `getColumnClass`
(`Integer.class`, `LocalDate.class` and so on). `new DTable(rows, names)`
works this out from the data for you.

**Why did my enum column sort alphabetically?**
It sorts by `toString()` if the column class is `Object`. Return the enum's
class from `getColumnClass` and it sorts in declaration order (for
example High, Medium, Low).

**The window icon doesn't change on my Linux desktop.**
GNOME doesn't show window icons in its title bars. Use
`lafDecorated(true)` with a FlatLaf theme to get a themed title bar that
does.

**How do I translate DFramework's messages?**
Add `com/dframework/swing/messages_<lang>.properties` to your classpath
with the same keys as `messages.properties`.

**Is it accessible?**
Components keep Swing's accessibility support. Toasts, the palette search
and the breadcrumb have accessible names, and every component works from
the keyboard. Animations can be turned off for reduced motion
(`DToast.setAnimated(false)`, `toggle.setAnimated(false)`).

**Where's the original 2001-2012 code?**
In the repository's history (commit *"Import original 2012 DFramework
sources"*) and in `dframework.zip`.

---

*DFramework: Java Swing, as it should have been out of the box. Since 2001.*
