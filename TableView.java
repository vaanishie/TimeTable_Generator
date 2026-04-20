import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/* TableView.java  —  Timetable Output Window :

Opens a new JFrame showing the generated timetable in a fully styled JTable, along with:
• Conflict cells highlighted in red/orange
• A conflict summary panel (if any conflicts found)
• Dark mode toggle button
• Export to PDF button
• A subject→teacher legend strip 
 */
public class TableView {

    // ── Column / Row labels ───────────────────────────────────
    private static final String[] PERIOD_HEADERS =
        {"Day", "P1", "P2", "P3", "P4", "P5", "P6"};
    private static final String[] DAY_NAMES =
        {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    // ── Light palette ─────────────────────────────────────────
    private static final Color L_ROW_A     = new Color(0xEAF4FB);
    private static final Color L_ROW_B     = new Color(0xFFFDE7);
    private static final Color L_HEADER_BG = new Color(0x1A237E);
    private static final Color L_HEADER_FG = Color.WHITE;
    private static final Color L_BG        = new Color(0xF5F7FF);
    private static final Color L_CARD      = Color.WHITE;
    private static final Color L_BORDER    = new Color(0xC5CAE9);
    private static final Color L_TEXT      = new Color(0x212121);
    private static final Color L_SUBTEXT   = new Color(0x546E7A);

    // ── Dark palette ──────────────────────────────────────────
    private static final Color D_ROW_A     = new Color(0x1E2A3A);
    private static final Color D_ROW_B     = new Color(0x1A2230);
    private static final Color D_HEADER_BG = new Color(0x0D47A1);
    private static final Color D_HEADER_FG = new Color(0xE3F2FD);
    private static final Color D_BG        = new Color(0x121212);
    private static final Color D_CARD      = new Color(0x1E1E1E);
    private static final Color D_BORDER    = new Color(0x37474F);
    private static final Color D_TEXT      = new Color(0xECEFF1);
    private static final Color D_SUBTEXT   = new Color(0x90A4AE);

    // ── Conflict colours (shared) ─────────────────────────────
    private static final Color CONFLICT_BG   = new Color(0xFFCDD2);
    private static final Color CONFLICT_DARK = new Color(0x7F0000);
    private static final Color CONFLICT_FG   = new Color(0xB71C1C);

    // ── State ─────────────────────────────────────────────────
    private boolean darkMode = false;
    private JFrame  frame;
    private JTable  table;
    private JPanel  conflictPanel;
    private JPanel  legendPanel;
    private JPanel  mainPanel;
    private TimetableSlot[][]              timetable;
    private Generator                      generator;
    private List<ConflictDetector.ConflictReport> reports;

    // ══════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════

    public void display(TimetableSlot[][] timetable,
                        Generator generator,
                        List<ConflictDetector.ConflictReport> reports) {
        this.timetable = timetable;
        this.generator = generator;
        this.reports   = reports;

        frame = new JFrame("📅  Timetable — " + generator.getPairs().size() + " subjects");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(buildTopBar(),    BorderLayout.NORTH);

        mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        mainPanel.add(buildTablePanel(),   BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        conflictPanel = buildConflictPanel();
        legendPanel   = buildLegendPanel();
        south.add(conflictPanel, BorderLayout.NORTH);
        south.add(legendPanel,   BorderLayout.SOUTH);
        mainPanel.add(south, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);

        applyTheme();

        frame.setSize(1050, 520);
        frame.setMinimumSize(new Dimension(800, 420));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── Top bar (title + buttons) ─────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Gradient header
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,new Color(0x1A237E),
                                              getWidth(),0,new Color(0x283593)));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("📅  Weekly Timetable", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        // Buttons panel
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);

        JButton darkBtn = makeHeaderBtn("🌙  Dark Mode");
        darkBtn.addActionListener(e -> toggleDarkMode());
        btns.add(darkBtn);

        JButton exportBtn = makeHeaderBtn("📄  Export PDF");
        exportBtn.addActionListener(e -> {
            PDFExporter exporter = new PDFExporter(timetable,
                generator.getPairs().size() + " subjects");
            boolean ok = exporter.export(frame);
            if (ok) showToast("Sent to printer / PDF dialog!");
        });
        btns.add(exportBtn);

        header.add(btns, BorderLayout.EAST);
        return header;
    }

    // ── Table panel ───────────────────────────────────────────

    private JScrollPane buildTablePanel() {
        // Build model
        Object[][] rowData = new Object[Generator.DAYS][Generator.PERIODS + 1];
        for (int d = 0; d < Generator.DAYS; d++) {
            rowData[d][0] = DAY_NAMES[d];
            for (int p = 0; p < Generator.PERIODS; p++) {
                rowData[d][p + 1] = timetable[d][p];   // store actual slot object
            }
        }

        DefaultTableModel model = new DefaultTableModel(rowData, PERIOD_HEADERS) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? String.class : TimetableSlot.class;
            }
        };

        table = new JTable(model);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(L_BORDER, 1));
        return scroll;
    }

    private void styleTable() {
        table.setRowHeight(56);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(false);

        // Custom cell renderer
        TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean focus, int row, int col) {

                // For the day-name column
                if (col == 0) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                            t, val, sel, focus, row, col);
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setBackground(darkMode ? D_HEADER_BG : L_HEADER_BG);
                    lbl.setForeground(darkMode ? D_HEADER_FG : L_HEADER_FG);
                    lbl.setOpaque(true);
                    return lbl;
                }

                // For slot cells — use a two-line panel
                TimetableSlot slot = (val instanceof TimetableSlot) ? (TimetableSlot) val : null;
                JPanel cell = new JPanel();
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

                Color bg;
                if (slot != null && slot.hasConflict()) {
                    bg = darkMode ? new Color(0x4E0000) : CONFLICT_BG;
                } else {
                    bg = (row % 2 == 0)
                        ? (darkMode ? D_ROW_A : L_ROW_A)
                        : (darkMode ? D_ROW_B : L_ROW_B);
                }
                cell.setBackground(bg);

                if (slot != null) {
                    JLabel subj = new JLabel(slot.getSubject());
                    subj.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    subj.setForeground(slot.hasConflict() ? CONFLICT_FG
                                       : (darkMode ? D_TEXT : L_TEXT));
                    subj.setAlignmentX(Component.CENTER_ALIGNMENT);
                    cell.add(subj);

                    JLabel teach = new JLabel(slot.getTeacher());
                    teach.setFont(new Font("Segoe UI", Font.ITALIC, 10));
                    teach.setForeground(slot.hasConflict() ? CONFLICT_FG.darker()
                                        : (darkMode ? D_SUBTEXT : L_SUBTEXT));
                    teach.setAlignmentX(Component.CENTER_ALIGNMENT);
                    cell.add(teach);

                    if (slot.hasConflict()) {
                        JLabel warn = new JLabel("⚠ conflict");
                        warn.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                        warn.setForeground(CONFLICT_FG);
                        warn.setAlignmentX(Component.CENTER_ALIGNMENT);
                        cell.add(warn);
                    }
                }

                return cell;
            }
        };

        for (int c = 0; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setCellRenderer(cellRenderer);
        }

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        for (int c = 1; c < table.getColumnCount(); c++) {
            table.getColumnModel().getColumn(c).setPreferredWidth(148);
        }

        // Header
        styleHeader();
    }

    private void styleHeader() {
        JTableHeader hdr = table.getTableHeader();
        hdr.setFont(new Font("Segoe UI", Font.BOLD, 13));
        hdr.setPreferredSize(new Dimension(hdr.getWidth(), 36));
        hdr.setReorderingAllowed(false);

        hdr.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,s,f,r,c);
                lbl.setBackground(darkMode ? D_HEADER_BG : L_HEADER_BG);
                lbl.setForeground(darkMode ? D_HEADER_FG : L_HEADER_FG);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBorder(BorderFactory.createMatteBorder(0,0,0,1,
                    new Color(darkMode ? 0x1565C0 : 0x3949AB)));
                return lbl;
            }
        });
    }

    // ── Conflict panel ────────────────────────────────────────

    private JPanel buildConflictPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        if (reports.isEmpty()) {
            JLabel ok = new JLabel("✅  No conflicts detected — schedule is clean!");
            ok.setFont(new Font("Segoe UI", Font.BOLD, 12));
            ok.setForeground(new Color(0x2E7D32));
            panel.add(ok);
        } else {
            JLabel warn = new JLabel("⚠  " + reports.size() + " conflict(s): ");
            warn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            warn.setForeground(CONFLICT_FG);
            panel.add(warn);

            for (ConflictDetector.ConflictReport r : reports) {
                JLabel lbl = new JLabel(r.toString());
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setForeground(CONFLICT_FG.darker());
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CONFLICT_FG, 1, true),
                    BorderFactory.createEmptyBorder(1, 6, 1, 6)));
                panel.add(lbl);
            }
        }

        panel.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        return panel;
    }

    // ── Legend panel ──────────────────────────────────────────

    private JPanel buildLegendPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));

        JLabel heading = new JLabel("Legend: ");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        panel.add(heading);

        Color[] chips = {
            new Color(0xFF8A65), new Color(0x4DB6AC), new Color(0x9575CD),
            new Color(0xE57373), new Color(0x4FC3F7), new Color(0xAED581),
            new Color(0xFFD54F), new Color(0xF06292), new Color(0x80DEEA),
            new Color(0xBCAAA4), new Color(0xA5D6A7), new Color(0xCE93D8)
        };

        List<SubjectTeacherPair> pairs = generator.getPairs();
        for (int i = 0; i < pairs.size(); i++) {
            SubjectTeacherPair p = pairs.get(i);
            Color chip = chips[i % chips.length];
            JLabel lbl = new JLabel("⬤ " + p.getSubject() + " (" + p.getTeacher() + ")");
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(chip.darker());
            panel.add(lbl);
        }

        return panel;
    }

    // ── Dark mode toggle ──────────────────────────────────────

    private void toggleDarkMode() {
        darkMode = !darkMode;
        applyTheme();
        table.repaint();
        table.getTableHeader().repaint();
    }

    private void applyTheme() {
        Color bg   = darkMode ? D_BG   : L_BG;
        Color card = darkMode ? D_CARD : L_CARD;
        Color text = darkMode ? D_TEXT : L_TEXT;

        frame.getContentPane().setBackground(bg);
        mainPanel.setBackground(bg);

        if (conflictPanel != null) {
            conflictPanel.setBackground(card);
            for (Component c : conflictPanel.getComponents()) {
                if (c instanceof JLabel) ((JLabel)c).setForeground(
                    reports.isEmpty() ? new Color(darkMode ? 0x81C784:0x2E7D32)
                                      : CONFLICT_FG);
            }
        }

        if (legendPanel != null) {
            legendPanel.setBackground(card);
            for (Component c : legendPanel.getComponents()) {
                if (c instanceof JLabel lbl && lbl.getText().startsWith("Legend")) {
                    lbl.setForeground(darkMode ? D_TEXT : L_TEXT);
                }
            }
        }

        if (table != null) {
            table.setBackground(darkMode ? D_CARD : L_CARD);
            table.setGridColor(darkMode ? D_BORDER : L_BORDER);
            table.getParent().setBackground(darkMode ? D_CARD : L_CARD); // viewport
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private JButton makeHeaderBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0x3949AB));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x7986CB), 1, true),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(0xFF6F00)); }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(0x3949AB)); }
        });
        return btn;
    }

    private void showToast(String message) {
        JOptionPane.showMessageDialog(frame, message,
            "Export", JOptionPane.INFORMATION_MESSAGE);
    }
}
