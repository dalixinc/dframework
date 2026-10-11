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
16. [Collapsible sections: `DCollapsiblePane`](#16-collapsible-sections-dcollapsiblepane)
17. [The audit log: `AuditLog`](#17-the-audit-log-auditlog)
18. [Plugins: `DPlugin` and `DPluginHost`](#18-plugins-dplugin-and-dpluginhost)
19. [The DNS Dig plugin](#19-the-dns-dig-plugin)
20. [The Git Browser plugin](#20-the-git-browser-plugin)
21. [The Time in Java plugin](#21-the-time-in-java-plugin)
22. [Building tool plugins: `CommandRunner`, `DCommandPane`, `DExplainPane`](#22-building-tool-plugins-commandrunner-dcommandpane-dexplainpane)
23. [Resources: `DResources`](#23-resources-dresources)
24. [Putting it together](#24-putting-it-together)
25. [Tips, rules and FAQ](#25-tips-rules-and-faq)

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
./gradlew :dframework-fx-demo:run # a first look at DFramework for JavaFX
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
| **Admin** | Users, Settings, Backup, About | Placeholders, and an About box with a spinning sign, the DFramework version, the Java version and the theme |
| **Plugins** | DNS Dig | A plugin found at runtime: run `dig` lookups, see the parsed answers, and see exactly which command runs |
| | Git Browser | Explore a git repository read-only: common commands from a dropdown, a drawn commit graph, and git's raw objects (commits, trees, blobs) |
| | Time in Java | A talk in a plugin: Java's date and time APIs from 1996 to today, then GPS and Einstein, every example compiled and run live |

Things to try:

- **Theme menu**: switch between 13 themes, live. Tick *Classic Outlook Bar*
  for the Windows XP look of Tracker 2.
- **Dialogs menu**: the classic animated road-sign icons on real
  `JOptionPane`s.
- **Window Icon menu**: aliens, skulls, a burger, a flying saucer…
- **Ctrl+Shift+P** or **F1**: the command palette. Type `fdark`,
  `go toast` or `forget`.
- **The breadcrumb** above the screens: drill into a call, then click your
  way back.
- **Move and resize the window, then quit and relaunch**: it comes back
  where you left it.
- **The status bar** shows the theme on the left and the DFramework version
  on the right. Hover over the version to see the Java version.
- **Plugins › Git Browser › Graph**: the history as a graph. Double-click a
  commit to open it in Objects.
- **Plugins › Time in Java**: press **Run both** on lesson 5 and watch a
  shared `SimpleDateFormat` fall apart, then Page Down through to Einstein.

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

Before the filter sees it, a paste is tidied:
- **Lines:** text with line breaks is cut to its first non-blank line.
  Copying from a terminal often adds a trailing newline.
- **Table rows:** text with tabs is cut to its first non-blank cell. A
  copied table row is tab-separated.
- **Ends:** what's left is trimmed.

Text without line breaks or tabs is left alone. To change this, use
`setPasteCleaner(...)`; `null` turns it off.

```java
DTextField id = new DTextField(20);
id.setPasteCleaner(text -> DTextField.firstValue(text).toLowerCase());   // tidy, then lower-case
```

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
table.addPropertyChangeListener(DTable.FILTER_TEXT_PROPERTY, e -> updateCount());
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

### `DCommitGraph`: history as a graph

A `DTable` that draws a commit history the way `git log --graph` does. It
shows coloured lanes, lines that split at branches and join at merges, and
badges for branches, tags and HEAD. Each row also shows the commit's
description, author, date and short id. It knows nothing about git: give it
`GraphCommit` records (id, parent ids, refs, subject, author, time), with
children before parents.

```java
GraphCommit first = new GraphCommit("a1", List.of(), List.of(new GraphCommit.Ref("v1.0", GraphCommit.Kind.TAG)),
        "First commit", "Dale", Instant.parse("2026-01-01T10:00:00Z"));
GraphCommit next = new GraphCommit("b2", List.of("a1"), List.of(new GraphCommit.Ref("main", GraphCommit.Kind.HEAD)),
        "Second commit", "Dale", Instant.parse("2026-01-02T10:00:00Z"));

DCommitGraph graph = new DCommitGraph();
graph.setCommits(List.of(next, first));                          // children first
graph.addOpenListener(commit -> System.out.println(commit.id())); // double-click or Enter
JScrollPane scroller = new JScrollPane(graph);
```

- **Fixed order:** the rows keep the order you give, so clicking a header
  doesn't sort.
- **Badge colours:** HEAD (the checked-out branch, in bold) is dark green,
  local branches green, remote branches purple, tags amber, and anything
  else grey.
- **Tooltips:** hovering over a row shows the full id, author, date and
  refs.
- **Copying:** <kbd>Ctrl</kbd>+<kbd>C</kbd> copies the selected commit's
  full id, rather than the tab-separated row an ordinary table copies.
- **Missing parents:** a parent that isn't in the list, because the history
  was cut off, gets a line running off the bottom.
- **Lane colours:** `setLaneColours` changes the colours the lanes cycle
  through.

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
- When a group has more items than fit, it scrolls, with up and down arrows
  on the scroll bar. The bar reserves room for the scroll bar, so items
  aren't squeezed, and tabbing to an item scrolls it into view.

```java
bar.setSelectedItem(item);     // as if clicked: opens its group and runs its action
bar.getSelectedItem();         // Optional<Item>
bar.addPropertyChangeListener(DOutlookBar.SELECTED_ITEM_PROPERTY, e -> updateTitle());
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

Press <kbd>Ctrl+Shift+P</kbd> or <kbd>F1</kbd>, type a few letters, press
<kbd>Enter</kbd>, just like in modern code editors.

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
| <kbd>Ctrl+Shift+P</kbd> or <kbd>F1</kbd> | open / close |
| <kbd>↑</kbd> <kbd>↓</kbd> <kbd>PgUp</kbd> <kbd>PgDn</kbd> | move |
| <kbd>Enter</kbd> or click | run |
| <kbd>Esc</kbd> or click outside | close |

```java
palette.setKeyStrokes(KeyStroke.getKeyStroke("ctrl K"));   // different shortcuts (one or more)
palette.setIncludeMenuBar(false);                          // only your own commands
palette.open();                                            // e.g. from a menu item
```

The palette never takes a key away from your menus. If an enabled menu item
already uses one of its shortcuts (F1 for *Help*, say), that shortcut is left
to the menu item. Once the palette is open, any of its shortcuts closes it.

> **Ctrl+Shift+P not working?** The window you launched the app from may
> still have the keyboard focus. On GNOME, windows started from a terminal
> often don't take it, and in VS Code's terminal, Ctrl+Shift+P opens VS Code's
> own palette. Click inside the application first, or use F1.

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

## 16. Collapsible sections: `DCollapsiblePane`

A titled section whose content folds away when the header is clicked, for
detail that some users want and others don't:

```java
DCollapsiblePane details = new DCollapsiblePane("Under the hood", commandPanel);
details.rememberState(Preferences.userRoot().node("myapp"), "underTheHood");   // reopen as left
details.setExpanded(false);
details.addPropertyChangeListener(DCollapsiblePane.EXPANDED_PROPERTY, e -> updateTitle());
```

The header shows ▾ when open and ▸ when closed. It works from the keyboard
(<kbd>Space</kbd>) and tells screen readers whether the section is open.

To put a collapsible section under the main content, use `splitBelow`. It
puts the two in a split whose divider the user can drag:

```java
JTextArea results = new JTextArea();
DCollapsiblePane underTheHood = new DCollapsiblePane("Under the hood", new JLabel("git status"));
JSplitPane layout = DCollapsiblePane.splitBelow(new JScrollPane(results), underTheHood);
```

The section gets the height it asks for, up to a third of the split.
Collapsing it gives its room back to the main content, so the details never
squeeze the results they describe. Resizing the window places the divider
again. DNS Dig and the Git Browser are laid out this way.

---

## 17. The audit log: `AuditLog`

An append-only record of what an application did: one JSON line per
action, with time, user, source and details.

```java
AuditLog.getDefault().record("tracker2", "export",
        Map.of("file", "calls.csv", "rows", 300));
```

```json
{"time":"2026-10-10T19:04:12+01:00","user":"dale","source":"tracker2","action":"export","file":"calls.csv","rows":"300"}
```

- The default file is `~/.dframework/audit.log`. Move it with
  `-Ddframework.audit.file=/path/to/audit.log` or `AuditLog.setDefault(...)`.
- New log files are readable by their owner only, because commands can
  contain sensitive details.
- If the file can't be written, entries are kept in memory and a warning is
  logged. Auditing never stops the application.
- `recent()` and `recent(source)` return this session's entries.
  `addListener(...)` is called for each new one, which is how DNS Dig's
  history table stays current.
- `AuditLog.inMemory()` gives a log with no file, useful in tests.

---

## 18. Plugins: `DPlugin` and `DPluginHost`

A plugin is a self-contained application that a DFramework program can
display, much as ActiveX controls once let applications embed each other.
The host knows only the `DPlugin` interface; the plugin brings its own
screen and behaviour.

### Writing a plugin

```java
public final class HelloPlugin implements DPlugin {

    @Override
    public String id() {
        return "com.example.hello";          // stable: names its settings and audit records
    }

    @Override
    public String name() {
        return "Hello";
    }

    @Override
    public String description() {
        return "A minimal plugin.";
    }

    @Override
    public JComponent createView(PluginContext context) {
        JButton wave = new JButton("Wave");
        wave.addActionListener(e -> {
            context.audit("wave", Map.of("times", 1));      // recorded with this plugin as the source
            DToast.success(context.component(), "Hello!");
        });
        context.addCommand(Command.of("Hello", "Wave", wave::doClick));   // into the command palette
        return wave;
    }
}
```

Then register it, so hosts can find it, in a file in the plugin's jar:

```
META-INF/services/com.dframework.swing.plugin.DPlugin
```

containing the class name, `com.example.hello.HelloPlugin`.

Optional extras: `icon()` (ideally 32×32), `category()` (the navigation
group; default "Plugins"), and `onShow()`, `onHide()` and `dispose()` for
the plugin's lifecycle.

### What a plugin gets: `PluginContext`

| Method | Gives |
|---|---|
| `component()` | the hosting pane, as the owner for dialogs and toasts |
| `audit(action, details)` | an audit entry with the plugin's id as the source |
| `preferences()` | the plugin's own saved settings, kept between runs |
| `addCommand(command)` | a command in the window's palette |
| `ai()` | the host's AI assistant, if one is installed (see below) |

### Hosting plugins

```java
DPluginHost host = new DPluginHost();
List<DPluginPane> panes = host.addToOutlookBar(nav, DPluginHost.discover(),
        pane -> cards.show(screens, pane.getPlugin().id()));
panes.forEach(pane -> screens.add(pane, pane.getPlugin().id()));
Runtime.getRuntime().addShutdownHook(new Thread(host::disposeAll));
```

- `DPluginHost.discover()` finds every plugin on the classpath. Adding a
  plugin jar is all it takes; the host's code never names it.
- `addToOutlookBar` puts each plugin in the bar group named by its category.
  Or call `host.mount(plugin)` yourself, which returns a `DPluginPane`.
- **Plugins start only when first opened**, so unused ones cost nothing.
- **A plugin that crashes on start-up shows its error** in its own pane. The
  rest of the application carries on, and a jar that can't be loaded at all
  is skipped and logged.
- `disposeAll()` shuts plugins down on exit, so they can stop background
  work such as a running process.

> Plugins run with the same permissions as the host application, as ActiveX
> controls did. Only install plugins you trust.

### AI assistance: `AiAssistant`

A host can offer plugins an AI model through `PluginContext.ai()`. The
library defines only a small `AiAssistant` interface. The `dframework-ai`
module provides one backed by Claude, and like plugins it's found
automatically when it's on the classpath.

```java
Optional<AiAssistant> ai = context.ai();
if (ai.isPresent() && ai.get().setupHint().isEmpty()) {
    AiAnswer answer = ai.get().ask(
            new AiPrompt("You explain ping results in plain English.", "Explain this:\n" + output),
            text -> System.out.print(text));    // called as the answer is written
    context.audit("explain", Map.of("model", answer.model(), "outputTokens", answer.outputTokens()));
}
```

- `ask` blocks until the answer is complete, so call it off the Swing
  thread (a `SwingWorker` or `DBusyPanel.run`). Interrupting the thread
  stops the request.
- `setupHint()` says what the user must do first, such as set an API key,
  so you can show it instead of failing.
- Failures arrive as `AiException`, with a message written for the user
  and a `kind()`: not set up, rejected key, rate limited, network, refused
  or cancelled.

---

## 19. The DNS Dig plugin

DNS Dig (the `dframework-dig` module) is both a real tool and a worked
example of a plugin. It runs the standard `dig` command and explains what
it's doing.

**The form**
- **Name**, **Type** (A, AAAA, MX, NS, TXT, CNAME, SOA, CAA, SRV, PTR, DS,
  DNSKEY, ANY) and **Server**, which is blank for your system's resolver.
  The ▾ button offers Cloudflare, Google, Quad9 and OpenDNS.
- **Switches:** Short (`+short`), Trace (`+trace`), DNSSEC (`+dnssec`),
  TCP (`+tcp`), Answer only (`+noall +answer`), No recursion (`+norecurse`)
  and Reverse (`-x`, which takes an IP address).
- **Timeout** in seconds (`+time=`).
- <kbd>Enter</kbd> or **Dig** runs the lookup, and **Stop** cancels it.

**The results**
- **Status line:** for example
  `NOERROR · 5 answers · 6 ms · asked 1.1.1.1 over UDP · 161 bytes`. Green
  means NOERROR, amber NXDOMAIN, and red a failure.
- **Answers:** a searchable table of the records returned. With Trace on,
  records are grouped by the server that returned them
  (`Hop 2 · a.root-servers.net (20 ms)`).
- **Raw output:** dig's own text, shown as it arrives.

**Explain with AI**

After a lookup, **Explain with AI** asks Claude to explain the result in
plain English: what was asked, what the records mean, anything worth
checking, and sometimes a follow-up command to try.
- **Streaming:** the explanation appears in the **AI explanation** tab as
  it's written. **Stop** cancels it.
- **Footer:** shows the model that answered, the tokens used and the time
  taken.
- **What was sent to Claude** (collapsible) shows the exact instructions
  and data sent.
- **Disclosure:** the tab says plainly that explaining sends the command and
  dig's output to Anthropic.
- **Audit:** every explanation is recorded in the audit log with the model,
  tokens and outcome.

To set it up, give the application an Anthropic API key (from the Claude
Console at [platform.claude.com](https://platform.claude.com)) and restart:

```sh
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew :dframework-demo:run
```

Until then, the button explains what to do instead, and nothing is sent.
Explanations use Claude Opus 5.5 at medium effort. A typical explanation is
around a thousand tokens in and a few hundred out, a cent or two. To
use another model, start the application with
`-Ddframework.ai.model=<model id>`.

**Under the hood** (collapsible, and it stays closed if you close it)
- **The exact command** that runs when you press Dig. It updates as you
  change the form, and **Copy** puts it on the clipboard to paste into a
  terminal.
- **Every command run this session**, with time, result and duration.
- **Where the audit log is.** Every lookup is recorded there, with the
  command, the server, the result and how long it took.

**Safety**
- dig runs **without a shell**, so nothing typed into the form can run a
  different command.
- A name or server can't start with `-`, `+` or `@`, which dig would read
  as its own options. For example, `-f /etc/passwd` would make dig read a
  local file. That text is blocked as you type.

If `dig` isn't installed, the plugin says how to install it
(`sudo apt install bind9-dnsutils` on Debian or Ubuntu) and still shows the
commands it would run.

To use DNS Dig in your own application, put `dframework-dig` on the runtime
classpath and host plugins as shown in [chapter 18](#18-plugins-dplugin-and-dpluginhost).

---

## 20. The Git Browser plugin

The Git Browser (the `dframework-git` module) explores a git repository
**read-only** and shows every git command it runs. It follows the same rules
as DNS Dig: no shell, validated input, an "Under the hood" pane, an audit
record for every command, and an optional AI explanation.

**Opening a repository.** Type or **Browse…** to any folder inside a
repository and press **Open**. The browser finds the repository's top folder
and shows the current branch. It remembers the last repository. The first
time, it opens the repository containing the folder the application was
started from. That works from any folder inside the repository, so starting
the demo with `gradlew run` (which runs in `dframework-demo`) opens
DFramework itself. If that folder isn't in a repository, the box is left
empty for you to choose one.

**Commands tab.** Choose from a dropdown of common read-only commands. Each
has a one-line description:

| Command | Runs |
|---|---|
| Status | `git status --short --branch` |
| Recent commits / All branches | `git log --oneline --graph --decorate [--all] -n <limit>` |
| Show a commit | `git show --stat --format=fuller <revision>` (default HEAD) |
| Unstaged / Staged changes | `git diff --stat`, `git diff --cached --stat` |
| Branches, Tags, Remotes | `git branch --all -vv`, `git tag --list -n1`, `git remote -v` |
| Contributors | `git shortlog -sne HEAD` |
| Reflog, Stashes | `git reflog -n <limit>`, `git stash list` |
| Tracked files | `git ls-files` |
| History of a file, Blame a file | `git log --follow -- <path>`, `git blame -- <path>` |
| Describe, Object counts | `git describe --tags --always --dirty`, `git count-objects -v -H` |
| Repository settings | `git config --local --list` |

Where a command needs one, a **Revision** (such as `HEAD~3`, `main` or
`v1.0`) or **Path** box is enabled, and **Limit** sets how many entries
commands like `log` show. Output appears as it arrives. Very large output
stops at 20,000 lines, and the status line says so. When a command prints
nothing, the output says what that means. For example, "Nothing is staged."
or "No stashes.", and a status with no changes says "Nothing to commit: the
working tree is clean."

**Objects tab: git's own storage.** Git stores everything as objects:
*commits* point to a *tree* (a folder listing) and to their *parents*;
trees list *blobs* (file contents) and other trees. The Objects tab lets you
walk them:

- Type or paste an object and press **Inspect** or Enter: `HEAD`, `HEAD~3`,
  `main^{tree}`, `HEAD:README.md` (a file as of a commit) or an object id.
- **The dropdown** lists, in this order:
  - objects you inspected recently;
  - `HEAD`, `HEAD^{tree}` and `HEAD~1`;
  - every branch, remote branch and tag, newest first;
  - the commits in the graph, each with its description.

  Arrow keys move through the list. Enter or a click inspects the chosen
  entry.
- You see its type, full id and size, then its content:
  - **Commits** and **tags** show their text, with buttons to follow their
    **tree** and **parent** (or tagged **object**) links.
  - **Trees** show a table of entries. Double-click one to go inside.
  - **Blobs** show the file's text. Binary files, and files over about
    2 MB, are summarised rather than shown.
- A breadcrumb trail (`HEAD › tree 569ee81 › README.md`) lets you step back.

**Graph tab: the commit graph.** The tab draws the history as a graph, with
branches splitting off and merging back, and badges for branches, tags,
remote branches and HEAD. It's drawn the first time you open the tab, and
again for each repository you open.
- **All branches:** on by default. Turn it off to see only the current
  branch's history.
- **Limit:** how many commits to draw, newest first. The default is 500.
  The status line says when the limit was reached.
- **Refresh:** runs the command again.
- **Opening a commit:** double-click a commit, or select it and press Enter,
  to open it in the Objects tab.
- **Copying an id:** <kbd>Ctrl</kbd>+<kbd>C</kbd> copies the selected
  commit's full id.

It runs one command, shown under the hood and audited as *graph*:
`git log --all --topo-order --decorate=full -n 500 --format=…`.
`--topo-order` puts every commit after all its children, which the drawing
needs.

Each inspection runs four commands, all shown and audited:
`rev-parse --verify` (the full id), `cat-file -t` (type), `cat-file -s`
(size) and `cat-file -p` (content).

**Read-only, deliberately.** The browser can't change a repository:
- **Fixed menu:** there's only a menu of read-only commands, with no way to
  type an arbitrary one.
- **No hidden writes:** git runs with `GIT_OPTIONAL_LOCKS=0`, so even
  `git status` doesn't quietly rewrite the index, and with
  `GIT_TERMINAL_PROMPT=0`, so it never stops to ask for a password.
- **No option injection:** a revision or path can't start with `-`, so it
  can't be read as an option (`--output=<file>` would make some commands
  write a file). Revisions also go after `--end-of-options` and paths after
  `--`, so git rejects such text even if it got through.

**Under the hood** shows the next command exactly as it runs, environment
variables included, ready to **Copy** into a terminal. It also lists every
command run this session, marked *open*, *run*, *graph* or *inspect*; hover over one
to see it in full.

**Explain with AI** (when an AI assistant is installed) explains the last
output: a log, a status, a blame, the graph, a commit or a tree. As with DNS Dig, the
tab says what is sent, shows the exact prompt, and audits the request.
Commit messages and file contents are passed to the model as data, never as
instructions.

---

## 21. The Time in Java plugin

**Time in Java** (the `dframework-time` module) is a plugin for giving a
talk. It shows how hard time is to handle, by walking through Java's own
attempts: `java.util.Date` (1996), `Calendar` (1997), Joda-Time (2005) and
`java.time` (2014). Then it goes beyond the calendar, to leap seconds, GPS
time and Einstein's correction for GPS clocks.

**Every example runs live.** When you press **Run**, the code on screen is
compiled by the JDK's own compiler and run, and what it prints appears
underneath. Nothing is canned:
- **Live editing:** you can change the code in front of an audience and run
  it again. **Reset** puts the original back.
- **The whole file:** **Whole file…** shows the complete source that's
  compiled, imports included.

### A lesson

Each lesson has:
- a title and a short story setting the scene;
- **two examples:** on the left, in red, the pitfall, usually the old API;
  on the right, in green, the fix, usually `java.time`;
- a **takeaway**: one line to remember.

The two examples sit side by side on a wide screen, and one above the other
on a narrow one. In a small window the lesson scrolls, rather than
squeezing the code.

The output shows what the code printed. It also shows:
- the compiler's warnings, in amber, such as "`Date(int,int,int)` has been
  deprecated";
- any exception, in red, with the line that threw it. In some lessons, the
  exception *is* the fix: `LocalDate.of(2026, 2, 30)` refuses to make a
  30 February;
- how long compiling and running took.

### The lessons

| Era | Lessons |
|---|---|
| 1996 `java.util.Date` | 1. Year 3926, month 11 · 2. A Date isn't a date · 3. The date that changed itself |
| 1997 `Calendar` | 4. February 30th is March 2nd · 5. SimpleDateFormat and threads · 6. YYYY isn't yyyy · 7. Same date, ten days apart |
| 2005 Joda-Time | 8. Joda-Time: the fix that came first · 9. The hour that doesn't exist |
| 2014 `java.time` | 10. A day isn't 24 hours · 11. 31 January plus a month · 12. How old are you? · 13. Nine o'clock where? · 14. Testing tomorrow, today |
| GPS and Einstein | 15. 23:59:60 · 16. GPS time: 18 seconds ahead · 17. Einstein's correction |

A few highlights:
- **Lesson 5** shares one `SimpleDateFormat` between eight threads.
  Hundreds of the 4,000 parses come back as wrong dates or exceptions, and
  the numbers change on every run. The `java.time` version gets none.
- **Lesson 7:** Shakespeare and Cervantes both died on 23 April 1616, ten
  days apart, because England and Spain used different calendars.
- **Lesson 17** works out the special and general relativity effects on a
  GPS satellite's clock (−7.2 and +45.7 µs a day). That's about 12 km a
  day of error if uncorrected. It then shows the fix: the satellite clocks
  are set to 10.22999999543 MHz before launch.

### Presenting

- **Moving between lessons:** Page Down and Page Up (what presentation
  clickers send), the arrow keys, or **Previous** and **Next**. The
  **timeline** at the top shows where you are; click an era to jump there.
- **Running:** **Run both** runs the old way, then the new way, as does
  <kbd>Ctrl</kbd>+<kbd>Shift</kbd>+<kbd>Enter</kbd>.
  <kbd>Ctrl</kbd>+<kbd>Enter</kbd> runs the example you're editing.
- **Presenter mode** enlarges all the text and buttons and hides the
  lesson list, for a projector.
- **Remembered:** the lesson you were on and presenter mode, so you can set
  up before the talk.
- **The command palette** has *Next Lesson*, *Previous Lesson*, *Run Both
  Examples* and *Presenter Mode On or Off*.

**Explain with AI** (when an AI assistant is installed) explains the last
example run: the code, its output, and the lesson it illustrates.

**Under the hood** starts collapsed here, because in a talk the examples
come first. It shows the equivalent `javac` command and lists every
example run this session; each run is recorded in the audit log as *run*,
noting whether the code had been edited.

**Requirements.** The examples need a JDK, not just a Java runtime, because
they're compiled as you watch. Without a compiler, the lessons still show,
but **Run** is disabled. The Joda-Time lessons use the Joda-Time library,
which the module brings with it.

**They can't break before the talk.** A test runs every example and checks
that it still prints what its lesson says it does. If a JDK or Joda-Time
update ever changes a result, the build fails rather than your demo.

---

## 22. Building tool plugins: `CommandRunner`, `DCommandPane`, `DExplainPane`

DNS Dig and the Git Browser are built from three library components, ready
for your own command-line tools.

```java
List<String> command = List.of("ping", "-c", "3", "example.com");
Map<String, String> environment = Map.of("LC_ALL", "C");

// Under the hood: the exact command, Copy, this session's history, the audit log location.
DCommandPane commands = new DCommandPane(context.auditLog(), context.plugin().id(), "run");
commands.setCommand(CommandLines.display(environment, command));

// Run it without a shell, streaming lines; cancel with runner.cancel().
CommandRunner runner = new CommandRunner();
CommandRunner.Outcome outcome = runner.run(command, environment, line -> System.out.println(line));
context.audit("run", Map.of("command", CommandLines.display(environment, command),
        "outcome", outcome.exitCode() == 0 ? "ok" : "failed",
        "exitCode", outcome.exitCode(), "durationMs", outcome.durationMs()));
```

- **`CommandRunner`** runs a program **without a shell**, so `;`, `$(...)`
  and `*` in arguments are passed on literally. It merges standard error
  into the output, can be cancelled, and stops a program after 20,000 lines
  by default (`outcome.truncated()` reports it). `CommandRunner.locate("git")`
  checks the program is installed.
- **`CommandLines.display`** writes a command out as you'd type it, quoting
  what a shell would interpret, so it can be pasted and run.
- **`DCommandPane`** is the "Under the hood" content. Put it in a
  `DCollapsiblePane`, and place that below your results with
  `DCollapsiblePane.splitBelow`. Its history lists the audit entries for your plugin
  and the actions you name.
- **`DExplainPane`** is the AI explanation panel: `new DExplainPane(ai,
  auditLog, source, preferences)`, then `explain(command, prompt)`. It
  shows the setup hint when there are no credentials, says where the data
  goes, shows exactly what was sent, and audits the request.

> Programs can still treat an argument starting with `-` as an option.
> Validate what users type, and use the program's end-of-options marker
> (often `--`) where it has one.

---

## 23. Resources: `DResources`

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

### The version: `DFramework.version()`

```java
String v = DFramework.version();               // "4.0.0-SNAPSHOT"
JLabel about = new JLabel(DFramework.nameAndVersion());   // "DFramework 4.0.0-SNAPSHOT"
```

The build stamps the version from `gradle.properties` into the jar, so it
always matches the library in use. It's also in the jar manifest as
`Implementation-Version`. The demo shows it at the right of its status bar
and in **Admin › About DFramework**. When running from sources that haven't
been through the build, it reads `development`.

---

## 24. Putting it together

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

## 25. Tips, rules and FAQ

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

**The command palette shortcut does nothing.**
Click inside your application first: on GNOME, a window started from a
terminal often doesn't get the keyboard focus. If Ctrl+Shift+P still doesn't
reach the application, press F1 instead, or choose your own keys with
`palette.setKeyStrokes(...)`.

**The window icon doesn't change on my Linux desktop.**
GNOME doesn't show window icons in its title bars. Use
`lafDecorated(true)` with a FlatLaf theme to get a themed title bar that
does.

**Where is the audit log?**
`~/.dframework/audit.log` unless you move it (see [chapter 17](#17-the-audit-log-auditlog)).
Each line is one JSON record, so it reads easily with `jq`, for example
`jq 'select(.source == "com.dframework.dig")' ~/.dframework/audit.log`.

**The "Explain with AI" button says Claude isn't set up.**
Set the `ANTHROPIC_API_KEY` environment variable (or run `ant auth login`)
and restart the application. Without credentials, nothing is ever sent.

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
