package proyectomago.panelclass;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Paleta y tipografía del POS local.
 * Rojo tomate + carbón + crema cálida (pizzería, no “UI genérica”).
 */
public final class UiTheme {

    private UiTheme() {
    }

    public static final Color BG = new Color(0xF7F1E8);
    public static final Color SURFACE = new Color(0xFFFCF8);
    public static final Color SURFACE_ALT = new Color(0xEFE6DA);
    public static final Color INK = new Color(0x1F1A17);
    public static final Color MUTED = new Color(0x6B5E54);
    public static final Color ACCENT = new Color(0xC23B22);
    public static final Color ACCENT_DARK = new Color(0x9A2E1B);
    public static final Color SUCCESS = new Color(0x2F6B4F);
    public static final Color BORDER = new Color(0xD9CBBA);

    public static final Font TITLE = new Font("Segoe UI Semibold", Font.BOLD, 22);
    public static final Font HEADING = new Font("Segoe UI Semibold", Font.BOLD, 15);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font("Segoe UI Semibold", Font.BOLD, 14);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TOTAL = new Font("Segoe UI Semibold", Font.BOLD, 28);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 13);

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        );
    }

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    /** Fondo suave para botones principales (cercano al cream del UI). */
    public static final Color BTN_PRIMARY_BG = new Color(0xE8DCCE);
    public static final Color BTN_PRIMARY_BORDER = new Color(0xC9B8A4);

    /**
     * Botón principal suave: fondo claro + texto oscuro (legible, poco agresivo).
     * BasicButtonUI evita que Windows pinte el botón y deje el texto ilegible.
     */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(BODY_BOLD);
        b.setBackground(BTN_PRIMARY_BG);
        b.setForeground(INK);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_PRIMARY_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(BODY);
        b.setBackground(SURFACE);
        b.setForeground(INK);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
