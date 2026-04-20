import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*Yeh class timetable ko PDF mein export/print karti hai.
Java ka built-in PrinterJob use hota hai (koi external library nahi).
System ke PDF printer (jaise Microsoft Print to PDF) ke through file banti hai.
Class Printable interface implement karti hai.
Graphics2D se table manually draw hota hai (lines, text, boxes).
Isse layout aur design pe full control milta hai.*/


public class PDFExporter implements Printable {

    // ── Layout constants (points — 72 pt = 1 inch) 
    private static final int MARGIN_X    = 40;
    private static final int MARGIN_TOP  = 60;
    private static final int COL_WIDTH   = 92;
    private static final int ROW_HEIGHT  = 50;
    private static final int HEADER_H    = 36;

    // ── Colour palette 
    private static final Color HEADER_BG   = new Color(0x1A237E);
    private static final Color ROW_A       = new Color(0xEAF4FB);
    private static final Color ROW_B       = new Color(0xFFFDE7);
    private static final Color CONFLICT_BG = new Color(0xFFCDD2);
    private static final Color GRID_LINE   = new Color(0x90A4AE);
    private static final Color TEXT_DARK   = new Color(0x212121);
    private static final Color TEXT_LIGHT  = Color.WHITE;
    private static final Color TEXT_SUB    = new Color(0x546E7A);

    // ── Data 
    private final TimetableSlot[][] table;
    private final String            title;

    private static final String[] DAY_NAMES    = {"Monday","Tuesday","Wednesday","Thursday","Friday"};
    private static final String[] PERIOD_NAMES = {"P1","P2","P3","P4","P5","P6"};

    public PDFExporter(TimetableSlot[][] table, String title) {
        this.table = table;
        this.title = title;
    }


    //  PUBLIC API  

    /**
     * Opens a print dialog so the user can choose "Save as PDF"
     * (or any printer).  Returns true if the user confirmed.
     */
    public boolean export(JFrame parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Timetable – " + title);
        job.setPrintable(this, getLandscapePage());

        // Show system print dialog
        if (job.printDialog()) {
            try {
                job.print();
                return true;
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(parent,
                    "Export failed:\n" + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    
    //  Printable INTERFACE
    
    /**
     * Java calls this once per page.  We only have one page.
     * All drawing is done here with Graphics2D.
     */
    @Override
    public int print(Graphics graphics, PageFormat pf, int pageIndex)
            throws PrinterException {

        if (pageIndex > 0) return NO_SUCH_PAGE;   // only 1 page

        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Translate to printable area origin
        g.translate(pf.getImageableX(), pf.getImageableY());

        drawTitle(g);
        drawTable(g);
        drawFooter(g, (int) pf.getImageableHeight());

        return PAGE_EXISTS;
    }

    // ── Drawing helpers ───────────────────────────────────────

    private void drawTitle(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(HEADER_BG);
        g.drawString("Class Timetable  —  " + title, MARGIN_X, 36);

        // Underline
        g.setColor(new Color(0x3F51B5));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(MARGIN_X, 40, MARGIN_X + 520, 40);
    }

    private void drawTable(Graphics2D g) {
        int cols = Generator.PERIODS + 1;  // day-name column + 6 periods
        int rows = Generator.DAYS + 1;     // header row + 5 days

        int tableX = MARGIN_X;
        int tableY = MARGIN_TOP;

        // ── Draw column headers (P1–P6) ──────────────────────
        // Day/Period label cell
        g.setColor(HEADER_BG);
        g.fillRect(tableX, tableY, COL_WIDTH - 10, HEADER_H);

        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.setColor(TEXT_LIGHT);
        drawCentred(g, "Day \\ Period",
            tableX, tableY, COL_WIDTH - 10, HEADER_H);

        for (int p = 0; p < Generator.PERIODS; p++) {
            int x = tableX + (COL_WIDTH - 10) + p * COL_WIDTH;
            g.setColor(HEADER_BG);
            g.fillRect(x, tableY, COL_WIDTH, HEADER_H);
            g.setColor(TEXT_LIGHT);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            drawCentred(g, PERIOD_NAMES[p], x, tableY, COL_WIDTH, HEADER_H);
        }

        // ── Draw data rows ────────────────────────────────────
        for (int day = 0; day < Generator.DAYS; day++) {
            int rowY = tableY + HEADER_H + day * ROW_HEIGHT;

            // Day name cell
            Color rowBg = (day % 2 == 0) ? ROW_A : ROW_B;
            g.setColor(rowBg);
            g.fillRect(tableX, rowY, COL_WIDTH - 10, ROW_HEIGHT);

            g.setColor(HEADER_BG);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            drawCentred(g, DAY_NAMES[day],
                tableX, rowY, COL_WIDTH - 10, ROW_HEIGHT);

            // Period cells
            for (int period = 0; period < Generator.PERIODS; period++) {
                int cellX = tableX + (COL_WIDTH - 10) + period * COL_WIDTH;
                TimetableSlot slot = table[day][period];

                // Background
                Color bg = slot != null && slot.hasConflict()
                         ? CONFLICT_BG : rowBg;
                g.setColor(bg);
                g.fillRect(cellX, rowY, COL_WIDTH, ROW_HEIGHT);

                if (slot != null) {
                    // Subject name
                    g.setColor(TEXT_DARK);
                    g.setFont(new Font("SansSerif", Font.BOLD, 10));
                    drawCentred(g, slot.getSubject(),
                        cellX, rowY, COL_WIDTH, ROW_HEIGHT / 2);

                    // Teacher name
                    g.setColor(TEXT_SUB);
                    g.setFont(new Font("SansSerif", Font.ITALIC, 9));
                    drawCentred(g, slot.getTeacher(),
                        cellX, rowY + ROW_HEIGHT / 2, COL_WIDTH, ROW_HEIGHT / 2);
                }
            }
        }

        // ── Draw grid lines ───────────────────────────────────
        g.setColor(GRID_LINE);
        g.setStroke(new BasicStroke(0.5f));

        int totalW = (COL_WIDTH - 10) + Generator.PERIODS * COL_WIDTH;
        int totalH = HEADER_H + Generator.DAYS * ROW_HEIGHT;

        // Horizontal lines
        for (int r = 0; r <= rows; r++) {
            int lineY = tableY + (r == 0 ? 0 : HEADER_H + (r - 1) * ROW_HEIGHT);
            if (r == rows) lineY = tableY + totalH;
            g.drawLine(tableX, lineY, tableX + totalW, lineY);
        }

        // Vertical lines
        for (int c = 0; c <= cols; c++) {
            int lineX = tableX + (c == 0 ? 0
                : (COL_WIDTH - 10) + (c - 1) * COL_WIDTH);
            if (c == cols) lineX = tableX + totalW;
            g.drawLine(lineX, tableY, lineX, tableY + totalH);
        }
    }

    private void drawFooter(Graphics2D g, int pageHeight) {
        String ts = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.setColor(Color.GRAY);
        g.drawString("Generated by Timetable Generator v2  •  " + ts,
            MARGIN_X, pageHeight - 12);
    }

    /** Draws a string centred inside a rectangle. */
    private void drawCentred(Graphics2D g, String text,
                              int rx, int ry, int rw, int rh) {
        FontMetrics fm = g.getFontMetrics();
        // Truncate if too wide
        while (fm.stringWidth(text) > rw - 6 && text.length() > 4) {
            text = text.substring(0, text.length() - 2) + "…";
        }
        int tx = rx + (rw - fm.stringWidth(text)) / 2;
        int ty = ry + (rh + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
    }

    /** Creates a landscape PageFormat. */
    private PageFormat getLandscapePage() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf  = job.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);
        return pf;
    }
}
