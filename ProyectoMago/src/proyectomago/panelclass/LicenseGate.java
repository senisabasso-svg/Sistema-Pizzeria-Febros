package proyectomago.panelclass;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Pantalla de bloqueo / reintento de licencia al iniciar.
 */
public final class LicenseGate {

    private LicenseGate() {
    }

    /**
     * @return true si el sistema puede abrirse.
     */
    public static boolean ensureCanRun(JFrame ownerHint) {
        LocalSystemLicense.Result result = LocalSystemLicense.ensureAllowed();
        if (result.allowed()) {
            System.out.println(result.message);
            return true;
        }

        while (true) {
            int choice = showBlocked(ownerHint, result);
            if (choice == 0) {
                // Configurar
                ConfiguracionDialog dlg = new ConfiguracionDialog(ownerHint, null);
                dlg.setVisible(true);
                result = LocalSystemLicense.ensureAllowed();
                if (result.allowed()) {
                    return true;
                }
            } else if (choice == 1) {
                // Reintentar validación online
                result = LocalSystemLicense.validateNow();
                if (result.allowed()) {
                    JOptionPane.showMessageDialog(ownerHint, result.message, "Licencia",
                            JOptionPane.INFORMATION_MESSAGE);
                    return true;
                }
                JOptionPane.showMessageDialog(ownerHint, result.message, "Licencia",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                // Salir
                return false;
            }
        }
    }

    private static int showBlocked(JFrame owner, LocalSystemLicense.Result result) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        panel.setPreferredSize(new Dimension(420, 120));

        String titulo;
        switch (result.outcome) {
            case BLOCKED_INACTIVE:
                titulo = "Sistema inactivo";
                break;
            case BLOCKED_NO_CONFIG:
                titulo = "Falta activación";
                break;
            case BLOCKED_NETWORK:
                titulo = "Sin conexión para validar";
                break;
            default:
                titulo = "No se pudo activar";
                break;
        }

        JLabel title = new JLabel("<html><b>" + titulo + "</b></html>");
        title.setFont(UiTheme.HEADING);
        title.setForeground(UiTheme.INK);

        JLabel body = new JLabel("<html>" + escapeHtml(result.message)
                + "<br><br>Tras una validación <b>activo</b>, el sistema puede usarse "
                + ComercioConfig.DIAS_CACHE_LICENCIA
                + " días sin internet.</html>");
        body.setFont(UiTheme.BODY);
        body.setForeground(UiTheme.MUTED);

        panel.add(title, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);

        Object[] options = {"Configurar", "Validar ahora", "Salir"};
        return JOptionPane.showOptionDialog(
                owner,
                panel,
                "Activación del sistema",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
