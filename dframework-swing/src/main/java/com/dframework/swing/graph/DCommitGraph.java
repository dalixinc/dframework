package com.dframework.swing.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.Serial;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.dframework.swing.table.DTable;

/**
 * A commit history drawn as a graph: coloured lanes, with lines splitting at
 * branches and joining at merges, and badges for branches, tags and HEAD.
 * Each row also shows the commit's description, author, date and short id.
 *
 * <pre>{@code
 * DCommitGraph graph = new DCommitGraph();
 * graph.setCommits(commits);                         // children first, e.g. git log --topo-order
 * graph.addOpenListener(commit -> show(commit.id())); // double-click or Enter
 * add(new JScrollPane(graph));
 * }</pre>
 *
 * <p>The rows keep the order given, so sorting by column is turned off. The
 * layout is worked out by {@link CommitGraph}.
 */
public class DCommitGraph extends DTable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Lane colours, chosen to read on both light and dark themes. */
    public static final List<Color> DEFAULT_LANE_COLOURS = List.of(
            new Color(0x3B82F6), new Color(0x2DA44E), new Color(0xE5A50A), new Color(0xD9534F),
            new Color(0x9B59B6), new Color(0x17A2B8), new Color(0xE67E22), new Color(0xE84393));

    static final int LANE_WIDTH = 14;
    private static final int GRAPH_PADDING = 6;
    private static final int MAX_GRAPH_WIDTH = 320;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Model model;
    private final transient List<Consumer<GraphCommit>> openListeners = new CopyOnWriteArrayList<>();
    private transient List<Color> laneColours = DEFAULT_LANE_COLOURS;

    public DCommitGraph() {
        this(new Model());
    }

    private DCommitGraph(Model model) {
        super(model);
        this.model = model;
        setAutoCreateRowSorter(false);
        setRowSorter(null);
        getTableHeader().setReorderingAllowed(false);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setRowHeight(Math.max(getRowHeight(), 22));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        setEmptyText("No commits.");

        column(Model.GRAPH).setCellRenderer(new GraphRenderer());
        column(Model.DESCRIPTION).setCellRenderer(new DescriptionRenderer());
        column(Model.DESCRIPTION).setPreferredWidth(420);
        column(Model.AUTHOR).setPreferredWidth(140);
        column(Model.DATE).setPreferredWidth(120);
        column(Model.COMMIT).setPreferredWidth(70);
        sizeGraphColumn();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && row >= 0) {
                    fireOpen(row);
                }
            }
        });
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openCommit");
        getActionMap().put("openCommit", new AbstractAction() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getSelectedRow() >= 0) {
                    fireOpen(getSelectedRow());
                }
            }
        });
    }

    /** Shows {@code commits}, children first. Replaces what was shown. */
    public void setCommits(List<GraphCommit> commits) {
        model.set(List.copyOf(commits), CommitGraph.layout(commits));
        sizeGraphColumn();
    }

    public List<GraphCommit> getCommits() {
        return model.commits;
    }

    /** The commit in {@code row}. */
    public GraphCommit getCommit(int row) {
        return model.commits.get(row);
    }

    /** The selected commit, if any. */
    public Optional<GraphCommit> getSelectedCommit() {
        int row = getSelectedRow();
        return row < 0 ? Optional.empty() : Optional.of(model.commits.get(row));
    }

    /** Called with the commit the user double-clicks or presses Enter on. */
    public void addOpenListener(Consumer<GraphCommit> listener) {
        openListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeOpenListener(Consumer<GraphCommit> listener) {
        openListeners.remove(listener);
    }

    public List<Color> getLaneColours() {
        return laneColours;
    }

    /** The colours lanes cycle through. */
    public void setLaneColours(List<Color> colours) {
        if (colours.isEmpty()) {
            throw new IllegalArgumentException("At least one lane colour is needed");
        }
        this.laneColours = List.copyOf(colours);
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        if (row < 0) {
            return null;
        }
        GraphCommit c = model.commits.get(row);
        StringBuilder html = new StringBuilder("<html><b>").append(escape(c.subject())).append("</b><br>")
                .append(escape(c.author())).append(", ").append(DATE.format(c.time().atZone(ZoneId.systemDefault())))
                .append("<br><code>").append(c.id()).append("</code>");
        if (!c.refs().isEmpty()) {
            html.append("<br>").append(escape(String.join(", ", c.refs().stream().map(GraphCommit.Ref::name).toList())));
        }
        return html.append("<br><i>Double-click to inspect</i></html>").toString();
    }

    private void fireOpen(int row) {
        GraphCommit commit = model.commits.get(row);
        openListeners.forEach(listener -> listener.accept(commit));
    }

    private TableColumn column(int modelIndex) {
        return getColumnModel().getColumn(convertColumnIndexToView(modelIndex));
    }

    private void sizeGraphColumn() {
        int lanes = Math.max(1, CommitGraph.maxWidth(model.rows));
        int width = Math.min(MAX_GRAPH_WIDTH, lanes * LANE_WIDTH + 2 * GRAPH_PADDING);
        TableColumn graph = column(Model.GRAPH);
        graph.setMinWidth(Math.min(width, 3 * LANE_WIDTH));
        graph.setPreferredWidth(width);
        graph.setWidth(width);
    }

    Color laneColour(int index) {
        return laneColours.get(index % laneColours.size());
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** The colour of a badge for {@code kind}. */
    static Color badgeColour(GraphCommit.Kind kind) {
        return switch (kind) {
            case HEAD -> new Color(0x1F883D);
            case BRANCH -> new Color(0x2DA44E);
            case REMOTE -> new Color(0x8250DF);
            case TAG -> new Color(0xBF8700);
            case OTHER -> new Color(0x6E7781);
        };
    }

    // ---- model --------------------------------------------------------------------------------

    static final class Model extends AbstractTableModel {

        @Serial
        private static final long serialVersionUID = 1L;

        static final int GRAPH = 0;
        static final int DESCRIPTION = 1;
        static final int AUTHOR = 2;
        static final int DATE = 3;
        static final int COMMIT = 4;
        private static final String[] COLUMNS = {"Graph", "Description", "Author", "Date", "Commit"};

        private transient List<GraphCommit> commits = List.of();
        private transient List<CommitGraph.Row> rows = List.of();

        void set(List<GraphCommit> commits, List<CommitGraph.Row> rows) {
            this.commits = commits;
            this.rows = rows;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return commits.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            GraphCommit c = commits.get(row);
            return switch (column) {
                case GRAPH -> rows.get(row);
                case DESCRIPTION -> c.subject();
                case AUTHOR -> c.author();
                case DATE -> DCommitGraph.DATE.format(c.time().atZone(ZoneId.systemDefault()));
                default -> c.id().length() > 7 ? c.id().substring(0, 7) : c.id();
            };
        }
    }

    // ---- renderers ----------------------------------------------------------------------------

    /** Paints one row's lanes, lines and dot over the cell background. */
    private final class GraphRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        private transient CommitGraph.Row row;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int r, int column) {
            super.getTableCellRendererComponent(table, "", selected, false, r, column);
            row = (CommitGraph.Row) value;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (row == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                double mid = getHeight() / 2.0;
                double bottom = getHeight();
                for (CommitGraph.Line line : row.lines()) {
                    g2.setColor(laneColour(line.colour()));
                    double x1 = x(line.from());
                    double x2 = x(line.to());
                    // Lines run a pixel past the cell edge so anti-aliasing leaves no seam between rows.
                    double y1 = line.upper() ? -1 : mid;
                    double y2 = line.upper() ? mid : bottom + 1;
                    Path2D path = new Path2D.Double();
                    path.moveTo(x1, y1);
                    if (x1 == x2) {
                        path.lineTo(x2, y2);
                    } else {   // a smooth bend between lanes
                        double half = (y2 - y1) / 2;
                        path.curveTo(x1, y1 + half, x2, y2 - half, x2, y2);
                    }
                    g2.draw(path);
                }
                double r = 4.5;
                Ellipse2D dot = new Ellipse2D.Double(x(row.lane()) - r, mid - r, 2 * r, 2 * r);
                g2.setColor(laneColour(row.colour()));
                g2.fill(dot);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(getBackground());
                g2.draw(dot);
            } finally {
                g2.dispose();
            }
        }

        private double x(int lane) {
            return GRAPH_PADDING + lane * LANE_WIDTH + LANE_WIDTH / 2.0;
        }
    }

    /** The subject, after a badge for each ref. */
    private static final class DescriptionRenderer extends DefaultTableCellRenderer {

        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            GraphCommit commit = ((DCommitGraph) table).getCommit(table.convertRowIndexToModel(row));
            setIcon(commit.refs().isEmpty() ? null : new Badges(commit.refs(), getFont()));
            setIconTextGap(6);
            return this;
        }
    }

    /** Ref badges, drawn as one icon so the label lays them out before the text. */
    private static final class Badges implements Icon {

        private static final int GAP = 4;
        private static final int PAD = 5;
        private static final FontRenderContext METRICS_CONTEXT = new FontRenderContext(null, true, true);
        private final List<GraphCommit.Ref> refs;
        private final Font font;
        private final Font bold;

        Badges(List<GraphCommit.Ref> refs, Font font) {
            this.refs = refs;
            this.font = font.deriveFont(font.getSize2D() - 1f);
            this.bold = this.font.deriveFont(Font.BOLD);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int at = x;
                for (GraphCommit.Ref ref : refs) {
                    Font f = fontFor(ref);
                    FontMetrics fm = c.getFontMetrics(f);
                    int w = measure(f, ref.name()) + 2 * PAD;
                    g2.setColor(badgeColour(ref.kind()));
                    g2.fill(new RoundRectangle2D.Double(at, y, w, getIconHeight(), 8, 8));
                    g2.setColor(Color.WHITE);
                    g2.setFont(f);
                    g2.drawString(ref.name(), at + PAD, y + (getIconHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    at += w + GAP;
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            int width = 0;
            for (GraphCommit.Ref ref : refs) {
                width += measure(fontFor(ref), ref.name()) + 2 * PAD + GAP;
            }
            return Math.max(0, width - GAP);
        }

        @Override
        public int getIconHeight() {
            return measureHeight(font) + 2;
        }

        private Font fontFor(GraphCommit.Ref ref) {
            return ref.kind() == GraphCommit.Kind.HEAD ? bold : font;
        }

        private static int measure(Font f, String text) {
            return (int) Math.ceil(f.getStringBounds(text, METRICS_CONTEXT).getWidth());
        }

        private static int measureHeight(Font f) {
            return (int) Math.ceil(f.getLineMetrics("Ag", METRICS_CONTEXT).getHeight());
        }
    }
}
