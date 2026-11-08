package proyectomago.panelclass;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 * Paleta FEBROS Punto de Venta: navy profundo + naranja vibrante + blanco.
 */
public final class UiTheme {

    private UiTheme() {
    }

    public static final Color BG = new Color(0x0A1628);
    public static final Color SURFACE = new Color(0x12233A);
    public static final Color SURFACE_ALT = new Color(0x1A2E48);
    public static final Color INK = new Color(0xF7FAFF);
    public static final Color MUTED = new Color(0x9AA8BC);
    public static final Color ACCENT = new Color(0xFF8A1F);
    public static final Color ACCENT_DARK = new Color(0xE07800);
    public static final Color ACCENT_SOFT = new Color(0x2A1C10);
    public static final Color ACCENT_BLUE = new Color(0x3D8BFF);
    public static final Color SUCCESS = new Color(0x3DDC97);
    public static final Color BORDER = new Color(0x24374F);
    public static final Color BORDER_STRONG = new Color(0x35506E);

    public static final Font TITLE = new Font("Segoe UI Semibold", Font.BOLD, 24);
    public static final Font HEADING = new Font("Segoe UI Semibold", Font.BOLD, 15);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font("Segoe UI Semibold", Font.BOLD, 14);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font TOTAL = new Font("Segoe UI Semibold", Font.BOLD, 34);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 14);
    public static final Font LOGIN_TITLE = new Font("Segoe UI Semibold", Font.BOLD, 28);

    public static final Color BTN_PRIMARY_BG = ACCENT;
    public static final Color BTN_PRIMARY_HOVER = ACCENT_DARK;
    public static final Color BTN_PRIMARY_FG = Color.WHITE;
    public static final Color BTN_SECONDARY_BG = SURFACE_ALT;
    public static final Color BTN_SECONDARY_HOVER = new Color(0x24374F);

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        );
    }

    public static Border softPanelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        );
    }

    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(10, 12, 10, 12)
        );
    }

    public static Border headerBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 16, 12, 16)
        );
    }

    public static JButton primaryButton(String text) {
        JButton b = baseButton(text, BODY_BOLD);
        b.setBackground(BTN_PRIMARY_BG);
        b.setForeground(BTN_PRIMARY_FG);
        b.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        attachPressFeedback(b, BTN_PRIMARY_BG, BTN_PRIMARY_HOVER, BTN_PRIMARY_FG);
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = baseButton(text, BODY);
        b.setBackground(BTN_SECONDARY_BG);
        b.setForeground(INK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        attachPressFeedback(b, BTN_SECONDARY_BG, BTN_SECONDARY_HOVER, INK);
        return b;
    }

    /**
     * Combo legible en dark mode.
     * En Windows el L&F pinta fondo blanco y, si el foreground es claro, el texto desaparece.
     * Se fuerza BasicComboBoxUI + editor opaco navy con texto claro.
     */
    public static void styleCombo(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new BasicArrowButton(
                        BasicArrowButton.SOUTH, SURFACE_ALT, BORDER_STRONG, INK, SURFACE_ALT);
                button.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                button.setOpaque(true);
                return button;
            }
        });
        combo.setFont(BODY);
        combo.setBackground(SURFACE_ALT);
        combo.setForeground(INK);
        combo.setOpaque(true);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_STRONG, 1));
        combo.setPreferredSize(new Dimension(10, 38));
        combo.setEditable(true);
        Component editor = combo.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField tf = (JTextField) editor;
            tf.setFont(BODY);
            tf.setBackground(SURFACE_ALT);
            tf.setForeground(INK);
            tf.setCaretColor(INK);
            tf.setDisabledTextColor(INK);
            tf.setSelectedTextColor(Color.WHITE);
            tf.setSelectionColor(ACCENT);
            tf.setBorder(new EmptyBorder(8, 10, 8, 10));
            tf.setOpaque(true);
            tf.setEditable(false);
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(BODY);
                setOpaque(true);
                setBorder(new EmptyBorder(8, 10, 8, 10));
                if (isSelected) {
                    setBackground(ACCENT);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(SURFACE_ALT);
                    setForeground(INK);
                }
                return this;
            }
        });
    }

    private static JButton baseButton(String text, Font font) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setFont(font);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void attachPressFeedback(JButton b, Color normal, Color hover, Color fg) {
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(normal);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(hover.darker());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!b.isEnabled()) {
                    return;
                }
                b.setBackground(b.getBounds().contains(e.getPoint()) ? hover : normal);
                b.setForeground(fg);
            }
        });
    }
}
