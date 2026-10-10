package com.dframework.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Compiles every Java example in docs/USER_GUIDE.md against the current library,
 * so the guide cannot drift from the API. Fragments become methods of a class that
 * declares the variables the guide takes as given; whole classes compile as written.
 */
class UserGuideExamplesTest {

    private static final Pattern JAVA_BLOCK = Pattern.compile("```java\\n(.*?)```", Pattern.DOTALL);
    private static final Pattern CLASS_DECL = Pattern.compile("^\\s*(public\\s+)?(final\\s+)?class\\s+(\\w+)", Pattern.MULTILINE);

    private static final String IMPORTS = """
            import com.dframework.swing.*;
            import com.dframework.swing.ai.*;
            import com.dframework.swing.audit.*;
            import com.dframework.swing.busy.*;
            import com.dframework.swing.button.*;
            import com.dframework.swing.close.*;
            import com.dframework.swing.command.*;
            import com.dframework.swing.dialog.*;
            import com.dframework.swing.field.*;
            import com.dframework.swing.nav.*;
            import com.dframework.swing.nav.DBreadcrumbBar.Crumb;
            import com.dframework.swing.notify.*;
            import com.dframework.swing.panel.*;
            import com.dframework.swing.plugin.*;
            import com.dframework.swing.table.*;
            import com.dframework.swing.theme.*;
            import com.dframework.swing.util.*;
            import java.awt.*;
            import java.time.Duration;
            import java.util.List;
            import java.util.Map;
            import java.util.Optional;
            import java.util.prefs.Preferences;
            import javax.swing.*;
            import javax.swing.table.*;
            """;

    /** What the guide's fragments take as given, with the type a reader would assume. */
    private static final String CONTEXT = """
                TableModel callModel; DFrame frame; JMenuBar menuBar; JPanel mainPanel; JMenuItem exitMenuItem;
                Icon someSwingIcon; JDialog myDialog; JLabel status; DTheme theme; ButtonGroup group; JMenu themeMenu;
                ConfirmCloser closer; Session session; Icon myLogo; DTextField email; DTextField field; JButton ok;
                List<DTextField> fields; JComboBox<String> myCombo; DTable table; CustomerList customerList;
                Icon logIcon; Icon sheetIcon; Action customersAction; DOutlookBar.Item item; DOutlookBar bar; Color color;
                JPanel homeScreen; JPanel reportsScreen; JPanel customersScreen; Call call; DBreadcrumbBar crumbs;
                DOutlookBar navBar; Action undoAction; DCommandPalette palette; JComponent network; DToggleSwitch wifi;
                JComponent callsScreen; Repository repository; DBusyPanel busy; List<String> rows;
                JComponent commandPanel; DOutlookBar nav; CardLayout cards; JPanel screens;
                PluginContext context; String output;

                interface Session { void logout(); }
                interface CustomerList { void filter(String query); }
                record Call(int number) { }
                interface Repository { TableModel loadCalls() throws Exception; }
                static class MyClass { }

                void save() { }
                void show(Object screen) { }
                void showDetail(Call call) { }
                void showList() { }
                void refresh() { }
                void undoDelete() { }
                void importRow(String row) { }
                void updateCount() { }
                void updateTitle() { }
                List<Command> openDocumentCommands() { return List.of(); }
            """;

    /** The sample application refers to the reader's own model and screen. */
    private static final String TRACKER_STUBS = """
            class CallModel extends AbstractTableModel {
                static CallModel load() { return new CallModel(); }
                public int getRowCount() { return 0; }
                public int getColumnCount() { return 0; }
                public Object getValueAt(int r, int c) { return null; }
            }
            class SettingsScreen extends JPanel { }
            """;

    @Test
    void everyJavaExampleCompiles(@TempDir Path dir) throws IOException {
        String guide = Files.readString(guidePath());
        List<String> blocks = new ArrayList<>();
        Matcher m = JAVA_BLOCK.matcher(guide);
        while (m.find()) {
            blocks.add(m.group(1));
        }
        assertThat(blocks).as("Java examples in the guide").hasSizeGreaterThan(40);

        List<Path> sources = new ArrayList<>();
        StringBuilder snippets = new StringBuilder(IMPORTS).append("\nclass GuideSnippets {\n").append(CONTEXT);
        for (int i = 0; i < blocks.size(); i++) {
            String block = blocks.get(i);
            Matcher cls = CLASS_DECL.matcher(block);
            if (cls.find()) {
                String name = cls.group(3);
                String source = block.stripLeading().startsWith("import") ? block : IMPORTS + "\n" + block;
                if (name.equals("Tracker")) {
                    source += TRACKER_STUBS;
                }
                sources.add(Files.writeString(dir.resolve(name + ".java"), source));
            } else {
                snippets.append("\n    // guide block ").append(i).append("\n    void block").append(i)
                        .append("() throws Exception {\n").append(block.indent(8)).append("    }\n");
            }
        }
        sources.add(Files.writeString(dir.resolve("GuideSnippets.java"), snippets.append("}\n").toString()));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StringWriter output = new StringWriter();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            boolean ok = compiler.getTask(output, files, diagnostics,
                    List.of("-d", dir.resolve("classes").toString(), "-cp", System.getProperty("java.class.path"),
                            "-proc:none"),
                    null, files.getJavaFileObjectsFromPaths(sources)).call();
            assertThat(ok).as(() -> "User Guide examples failed to compile:\n" + describe(diagnostics) + output).isTrue();
        }
    }

    private static Path guidePath() {
        // Tests run with the module directory as working directory.
        for (Path candidate : List.of(Path.of("../docs/USER_GUIDE.md"), Path.of("docs/USER_GUIDE.md"))) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Cannot find docs/USER_GUIDE.md from " + Path.of("").toAbsolutePath());
    }

    private static String describe(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder text = new StringBuilder();
        diagnostics.getDiagnostics().forEach(d -> text.append(d.getKind()).append(' ')
                .append(d.getSource() == null ? "" : Path.of(d.getSource().toUri()).getFileName())
                .append(':').append(d.getLineNumber()).append(": ").append(d.getMessage(null)).append('\n'));
        return text.toString();
    }
}
