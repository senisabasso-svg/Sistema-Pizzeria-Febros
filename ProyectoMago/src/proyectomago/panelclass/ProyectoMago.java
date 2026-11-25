package proyectomago.panelclass;

import java.awt.Dimension;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class ProyectoMago {

    public static void main(String[] args) {
        try {
            Locale.setDefault(new Locale("es", "UY"));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("ComboBox.background", UiTheme.SURFACE_ALT);
            UIManager.put("ComboBox.foreground", UiTheme.INK);
            UIManager.put("ComboBox.selectionBackground", UiTheme.ACCENT);
            UIManager.put("ComboBox.selectionForeground", java.awt.Color.WHITE);
            UIManager.put("ComboBox.buttonBackground", UiTheme.SURFACE_ALT);
            UIManager.put("TextField.background", UiTheme.SURFACE_ALT);
            UIManager.put("TextField.foreground", UiTheme.INK);
            UIManager.put("TextField.caretForeground", UiTheme.INK);
            UIManager.put("OptionPane.okButtonText", "Aceptar");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
            UIManager.put("OptionPane.yesButtonText", "Sí");
            UIManager.put("OptionPane.noButtonText", "No");
            UIManager.put("OptionPane.titleText", "Mensaje");
            UIManager.put("OptionPane.inputDialogTitle", "Entrada");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            String nombre = ComercioConfig.get().getNombreComercio();
            JFrame frame = new JFrame(nombre + " · Gestión almacenes");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(1080, 680));
            frame.getContentPane().setBackground(UiTheme.BG);
            frame.setSize(1180, 740);
            frame.setLocationRelativeTo(null);

            Runnable openMain = () -> {
                frame.setContentPane(new PrincipalPanel());
                frame.setTitle(ComercioConfig.get().getNombreComercio() + " · Gestión almacenes");
                frame.revalidate();
                frame.repaint();
            };

            frame.setContentPane(new LoginPanel(openMain));
            frame.setVisible(true);
        });
    }
}
