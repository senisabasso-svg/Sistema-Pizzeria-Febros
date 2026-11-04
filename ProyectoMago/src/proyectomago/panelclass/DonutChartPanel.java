package proyectomago.panelclass;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/**
 * Donut chart suave para informes (antialias + leyenda + centro con total).
 */
public class DonutChartPanel extends JPanel {

    public static final class Slice {
        public final String label;
        public final double value;
        public final Color color;

        public Slice(String label, double value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private static final Color[] PALETTE = {
        new Color(0xC23B22),
        new Color(0x2F6B4F),
        new Color(0xC9922A),
        new Color(0x8B5E3C),
        new Color(0x3F6F8C),
        new Color(0xA84B2F),
        new Color(0x5C6B73),
        new Color(0x6B8F71)
    };

    private final List<Slice> slices = new ArrayList<>();
    private String title = "";
    private String centerCaption = "";
    private String centerValue = "";

    public DonutChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(280, 260));
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public void setCenter(String caption, String value) {
        this.centerCaption = caption == null ? "" : caption;
        this.centerValue = value == null ? "" : value;
    }

    public void setSlices(List<Slice> data) {
        slices.clear();
        if (data != null) {
            slices.addAll(data);
        }
        repaint();
    }

    public static Color colorAt(int index) {
        return PALETTE[Math.floorMod(index, PALETTE.length)];
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fondo tarjeta
        g2.setColor(UiTheme.SURFACE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
        g2.setColor(UiTheme.BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);

        int pad = 16;
        g2.setFont(UiTheme.HEADING);
        g2.setColor(UiTheme.INK);
        g2.drawString(title, pad, pad + 14);

        double total = 0;
        for (Slice s : slices) {
            if (s.value > 0) {
                total += s.value;
            }
        }

        int chartTop = pad + 28;
        int legendH = Math.min(90, 18 + slices.size() * 18);
        int chartSize = Math.min(w - pad * 2, h - chartTop - legendH - 12);
        chartSize = Math.max(120, chartSize);
        int cx = w / 2;
        int cy = chartTop + chartSize / 2;
        int outer = chartSize;
        int inner = (int) (outer * 0.58);
        int x = cx - outer / 2;
        int y = cy - outer / 2;

        if (total <= 0 || slices.isEmpty()) {
            g2.setColor(new Color(0xEDE4D8));
            g2.fillOval(x, y, outer, outer);
            g2.setColor(UiTheme.SURFACE);
            g2.fillOval(cx - inner / 2, cy - inner / 2, inner, inner);
            g2.setFont(UiTheme.SMALL);
            g2.setColor(UiTheme.MUTED);
            String empty = "Sin datos";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(empty, cx - fm.stringWidth(empty) / 2, cy + 4);
        } else {
            // Sombra suave
            g2.setColor(new Color(0, 0, 0, 18));
            g2.fillOval(x + 3, y + 5, outer, outer);

            double angle = 90;
            for (Slice s : slices) {
                if (s.value <= 0) {
                    continue;
                }
                double extent = -(s.value / total) * 360.0;
                g2.setColor(s.color);
                g2.fill(new Arc2D.Double(x, y, outer, outer, angle, extent, Arc2D.PIE));
                angle += extent;
            }

            // Hueco central
            g2.setColor(UiTheme.SURFACE);
            g2.fill(new Ellipse2D.Double(cx - inner / 2.0, cy - inner / 2.0, inner, inner));

            // Anillo sutil
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(0xE8DCCE));
            g2.drawOval(cx - inner / 2, cy - inner / 2, inner, inner);

            // Texto centro
            g2.setFont(UiTheme.SMALL);
            g2.setColor(UiTheme.MUTED);
            FontMetrics fmCap = g2.getFontMetrics();
            g2.drawString(centerCaption, cx - fmCap.stringWidth(centerCaption) / 2, cy - 8);

            g2.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
            g2.setColor(UiTheme.INK);
            FontMetrics fmVal = g2.getFontMetrics();
            g2.drawString(centerValue, cx - fmVal.stringWidth(centerValue) / 2, cy + 14);
        }

        // Leyenda
        int ly = chartTop + chartSize + 10;
        int lx = pad;
        g2.setFont(UiTheme.SMALL);
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            if (s.value <= 0) {
                continue;
            }
            if (ly + 16 > h - 8) {
                break;
            }
            g2.setColor(s.color);
            g2.fillRoundRect(lx, ly, 10, 10, 3, 3);
            g2.setColor(UiTheme.INK);
            String pct = total > 0 ? Math.round(s.value * 100.0 / total) + "%" : "";
            String text = s.label + "  $" + String.format("%.0f", s.value) + "  " + pct;
            g2.drawString(text, lx + 16, ly + 10);
            ly += 18;
        }

        g2.dispose();
    }
}
