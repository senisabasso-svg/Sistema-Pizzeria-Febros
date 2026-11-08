package proyectomago.panelclass;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ProyectoMago {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("ComboBox.background", UiTheme.SURFACE_ALT);
            UIManager.put("ComboBox.foreground", UiTheme.INK);
            UIManager.put("ComboBox.selectionBackground", UiTheme.ACCENT);
            UIManager.put("ComboBox.selectionForeground", java.awt.Color.WHITE);
            UIManager.put("ComboBox.buttonBackground", UiTheme.SURFACE_ALT);
            UIManager.put("TextField.background", UiTheme.SURFACE_ALT);
            UIManager.put("TextField.foreground", UiTheme.INK);
            UIManager.put("TextField.caretForeground", UiTheme.INK);
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            if (!LicenseGate.ensureCanRun(null)) {
                System.exit(0);
                return;
            }

            String nombre = ComercioConfig.get().getNombreComercio();
            JFrame frame = new JFrame(nombre + " · Punto de Venta");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1040, 680));
            frame.getContentPane().setBackground(UiTheme.BG);
            frame.setSize(1120, 740);
            frame.setLocationRelativeTo(null);

            Runnable openMain = () -> {
                frame.setContentPane(new PrincipalPanel());
                frame.setTitle(ComercioConfig.get().getNombreComercio() + " · Punto de Venta");
                frame.revalidate();
                frame.repaint();
            };

            frame.setContentPane(new LoginPanel(openMain));
            frame.setVisible(true);
        });
    }
}
