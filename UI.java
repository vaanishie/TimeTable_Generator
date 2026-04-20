import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  UI.java  —  Main Input Window
 * ============================================================
 *  The main input form. Lets the user:
 *   1. Add/remove subject–teacher pairs via an editable JTable
 *   2. Load a set of default subjects with one click
 *   3. Choose the number of class sections
 *   4. Generate the timetable
 *
 *  The form has a dark/light mode toggle and smooth visual
 *  styling designed to look like a real B.Tech project.
 * ============================================================
 */
public class UI {

    // ── Palettes ──────────────────────────────────────────────
    private static final Color L_BG       = new Color(0xF5F7FF);
    private static final Color L_CARD     = Color.WHITE;
    private static final Color L_BORDER   = new Color(0xC5CAE9);
    private static final Color L_TEXT     = new Color(0x212121);
    private static final Color L_SUBTEXT  = new Color(0x5C6BC0);
    private static final Color ACCENT     = new Color(0x3F51B5);
    private static final Color ACCENT_DK  = new Color(0x1A237E);
    private static final Color HIGHLIGHT  = new Color(0xFF6F00);

    private static final Color D_BG      = new Color(0x121212);
    private static final Color D_CARD    = new Color(0x1E1E1E);
    private static final Color D_BORDER  = new Color(0x37474F);
    private static final Color D_TEXT    = new Color(0xECEFF1);
    private static final Color D_SUBTEXT = new Color(0x90A4AE);

    // ── Default subjects ──────────────────────────────────────
    private static final String[][] DEFAULTS = {
        {"Mathematics",  "Mr. Sharma"},
        {"Physics",      "Ms. Verma"},
        {"Chemistry",    "Mr. Gupta"},
        {"Biology",      "Ms. Nair"},
        {"English",      "Mr. Singh"},
        {"Computer Sc.", "Ms. Joshi"}
    };

    // ── Swing components ──────────────────────────────────────
    private JFrame             frame;
    private DefaultTableModel  inputModel;
    private JSpinner           spinnerClasses;
    private JLabel             statusLabel;
    private JPanel             mainCard;
    private boolean            darkMode = false;

    // ══════════════════════════════════════════════════════════
    //  PUBLIC API
    // ══════════════════════════════════════════════════════════

    public void show() {
        frame = new JFrame("Timetable Generator v2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(buildMain(),   BorderLayout.CENTER);
        frame.add(buildFooter(), BorderLayout.SOUTH);

        frame.setSize(580, 640);
        frame.setMinimumSize(new Dimension(520, 560));
        frame.setLocationRelativeTo(null);
        applyTheme();
        frame.setVisible(true);
    }

    // ── Header ────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,ACCENT_DK,getWidth(),0,ACCENT));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 14, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("🗓");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 34));
        left.add(icon);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel appName = new JLabel("Timetable Generator");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        appName.setForeground(Color.WHITE);
        titles.add(appName);

        JLabel sub = new JLabel("Greedy + Backtracking CSP Scheduler");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(0xC5CAE9));
        titles.add(sub);

        left.add(titles);
        header.add(left, BorderLayout.WEST);

        // Dark mode toggle
        JButton darkBtn = new JButton("🌙");
        darkBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        darkBtn.setForeground(Color.WHITE);
        darkBtn.setOpaque(false);
        darkBtn.setContentAreaFilled(false);
        darkBtn.setBorderPainted(false);
        darkBtn.setFocusPainted(false);
        darkBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        darkBtn.setToolTipText("Toggle dark mode");
        darkBtn.addActionListener(e -> {
            darkMode = !darkMode;
            darkBtn.setText(darkMode ? "☀️" : "🌙");
            applyTheme();
        });
        header.add(darkBtn, BorderLayout.EAST);

        return header;
    }

    // ── Main content ──────────────────────────────────────────

    private JPanel buildMain() {
        mainCard = new JPanel(new BorderLayout(0, 12));
        mainCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        mainCard.add(buildSubjectSection(), BorderLayout.CENTER);
        mainCard.add(buildBottomControls(), BorderLayout.SOUTH);

        return mainCard;
    }

    /** Editable JTable for subject/teacher input. */
    private JPanel buildSubjectSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        // Section title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel sectionTitle = new JLabel("📚  Subjects & Teachers");
        sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sectionTitle.setForeground(ACCENT);
        titleRow.add(sectionTitle, BorderLayout.WEST);

        // Buttons on the right
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton loadBtn = makeSmallBtn("Load Defaults", ACCENT);
        loadBtn.addActionListener(e -> loadDefaults());
        btnRow.add(loadBtn);

        JButton addBtn = makeSmallBtn("+ Add Row", new Color(0x388E3C));
        addBtn.addActionListener(e -> addRow());
        btnRow.add(addBtn);

        JButton delBtn = makeSmallBtn("− Remove Row", new Color(0xC62828));
        delBtn.addActionListener(e -> removeSelectedRow());
        btnRow.add(delBtn);

        titleRow.add(btnRow, BorderLayout.EAST);
        section.add(titleRow, BorderLayout.NORTH);

        // Input table
        inputModel = new DefaultTableModel(new Object[]{"Subject", "Teacher"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return true; }
        };
        loadDefaults(); // pre-fill with defaults

        JTable inputTable = new JTable(inputModel);
        inputTable.setRowHeight(30);
        inputTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inputTable.setSelectionBackground(new Color(0xC5CAE9));
        inputTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        inputTable.getTableHeader().setBackground(ACCENT);
        inputTable.getTableHeader().setForeground(Color.WHITE);
        inputTable.getTableHeader().setReorderingAllowed(false);
        inputTable.setGridColor(L_BORDER);
        inputTable.setShowGrid(true);

        JScrollPane scroll = new JScrollPane(inputTable);
        scroll.setBorder(BorderFactory.createLineBorder(L_BORDER, 1));
        scroll.setPreferredSize(new Dimension(0, 220));

        section.add(scroll, BorderLayout.CENTER);

        // Hint
        JLabel hint = new JLabel("  ✏️  Click any cell to edit. Up to 12 subjects supported.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(L_SUBTEXT);
        section.add(hint, BorderLayout.SOUTH);

        return section;
    }

    /** Bottom row: class count spinner + generate button. */
    private JPanel buildBottomControls() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Classes row
        JPanel classRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        classRow.setOpaque(false);

        JLabel classLbl = new JLabel("🏫  Number of Class Sections:");
        classLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        classRow.add(classLbl);

        spinnerClasses = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        spinnerClasses.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinnerClasses.setPreferredSize(new Dimension(70, 32));
        JTextField tf = ((JSpinner.DefaultEditor) spinnerClasses.getEditor()).getTextField();
        tf.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tf.setHorizontalAlignment(JTextField.CENTER);
        classRow.add(spinnerClasses);

        JLabel note = new JLabel("(informational — shown in output title)");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        note.setForeground(L_SUBTEXT);
        classRow.add(note);

        panel.add(classRow, BorderLayout.NORTH);

        // Generate button
        JButton genBtn = buildGenerateButton();
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnWrapper.setOpaque(false);
        btnWrapper.add(genBtn);
        panel.add(btnWrapper, BorderLayout.CENTER);

        return panel;
    }

    /** Footer status bar. */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createEmptyBorder(4, 18, 10, 18));
        footer.setBackground(L_BG);

        statusLabel = new JLabel("Ready — load defaults or enter your subjects, then click Generate.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setForeground(L_SUBTEXT);
        footer.add(statusLabel, BorderLayout.WEST);

        return footer;
    }

    // ── Generate button ───────────────────────────────────────

    private JButton buildGenerateButton() {
        JButton btn = new JButton("Generate Timetable  →") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()  ? ACCENT_DK :
                            getModel().isRollover() ? HIGHLIGHT : ACCENT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(280, 48));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> onGenerate());
        return btn;
    }

    // ── Event handlers ────────────────────────────────────────

    private void onGenerate() {
        // Collect subject-teacher pairs from the editable table
        List<SubjectTeacherPair> pairs = new ArrayList<>();

        for (int r = 0; r < inputModel.getRowCount(); r++) {
            Object subj = inputModel.getValueAt(r, 0);
            Object teach = inputModel.getValueAt(r, 1);
            if (subj == null || teach == null) continue;
            String s = subj.toString().trim();
            String t = teach.toString().trim();
            if (!s.isEmpty() && !t.isEmpty()) {
                pairs.add(new SubjectTeacherPair(s, t));
            }
        }

        // Validate
        if (pairs.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "Please add at least one subject and teacher.",
                "No Subjects", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (pairs.size() > 12) {
            JOptionPane.showMessageDialog(frame,
                "Maximum 12 subjects are supported.",
                "Too Many Subjects", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int classes = (Integer) spinnerClasses.getValue();

        statusLabel.setForeground(ACCENT);
        statusLabel.setText("⏳  Running Greedy + Backtracking scheduler…");
        frame.repaint();

        // Run generation on a background thread to keep UI responsive
        SwingWorker<TimetableSlot[][], Void> worker = new SwingWorker<>() {
            @Override
            protected TimetableSlot[][] doInBackground() {
                Generator gen = new Generator(pairs);
                return gen.generate();
            }

            @Override
            protected void done() {
                try {
                    TimetableSlot[][] timetable = get();
                    Generator gen = new Generator(pairs);
                    gen.generate(); // rebuild to get a fresh generator with same pairs

                    // Detect conflicts
                    ConflictDetector detector = new ConflictDetector();
                    List<ConflictDetector.ConflictReport> reports =
                        detector.detect(timetable);

                    // Display
                    Generator finalGen = new Generator(pairs);
                    finalGen.generate();
                    Generator displayGen = new Generator(pairs);
                    TimetableSlot[][] finalTable = displayGen.generate();
                    List<ConflictDetector.ConflictReport> finalReports =
                        new ConflictDetector().detect(finalTable);

                    new TableView().display(finalTable, displayGen, finalReports);

                    String msg = reports.isEmpty()
                        ? "✅  Timetable generated — no conflicts!"
                        : "⚠  Generated with " + reports.size() + " conflict(s) highlighted.";
                    statusLabel.setForeground(reports.isEmpty()
                        ? new Color(0x2E7D32) : new Color(0xE65100));
                    statusLabel.setText(msg);

                } catch (Exception ex) {
                    statusLabel.setText("❌  Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadDefaults() {
        inputModel.setRowCount(0);
        for (String[] row : DEFAULTS) {
            inputModel.addRow(row);
        }
        setStatus("Default subjects loaded. Click Generate when ready.", L_SUBTEXT);
    }

    private void addRow() {
        if (inputModel.getRowCount() >= 12) {
            JOptionPane.showMessageDialog(frame, "Maximum 12 subjects reached.",
                "Limit", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        inputModel.addRow(new Object[]{"New Subject", "Teacher Name"});
    }

    private void removeSelectedRow() {
        // Find row via focused cell — simple approach
        if (inputModel.getRowCount() == 0) return;
        int last = inputModel.getRowCount() - 1;
        inputModel.removeRow(last);
    }

    // ── Theme ─────────────────────────────────────────────────

    private void applyTheme() {
        Color bg   = darkMode ? D_BG   : L_BG;
        Color card = darkMode ? D_CARD : L_CARD;
        Color text = darkMode ? D_TEXT : L_TEXT;
        Color sub  = darkMode ? D_SUBTEXT : L_SUBTEXT;

        frame.getContentPane().setBackground(bg);
        if (mainCard != null) {
            mainCard.setBackground(bg);
            tintChildren(mainCard, bg, card, text, sub);
        }
        if (statusLabel != null) {
            statusLabel.getParent().setBackground(bg);
            statusLabel.setForeground(sub);
        }
        frame.repaint();
    }

    /** Recursively set background/foreground on child components. */
    private void tintChildren(Container c, Color bg, Color card,
                               Color text, Color sub) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JScrollPane sp) {
                sp.getViewport().setBackground(card);
                sp.setBackground(card);
                if (sp.getViewport().getView() instanceof JTable t) {
                    t.setBackground(card);
                    t.setForeground(text);
                    t.getTableHeader().setBackground(ACCENT);
                    t.getTableHeader().setForeground(Color.WHITE);
                }
            } else if (comp instanceof JPanel p) {
                if (!(comp instanceof JTable)) p.setBackground(bg);
                tintChildren(p, bg, card, text, sub);
            } else if (comp instanceof JLabel lbl) {
                if (!lbl.getText().isEmpty() &&
                    !lbl.getForeground().equals(ACCENT) &&
                    !lbl.getForeground().equals(new Color(0x388E3C)) &&
                    !lbl.getForeground().equals(new Color(0xC62828))) {
                    lbl.setForeground(text);
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private JButton makeSmallBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setStatus(String msg, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setForeground(color);
        }
    }
}
