package proyectomago.panelclass;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * PIN de administrador para vendedores e informes.
 * El valor efectivo por defecto sigue siendo 8521; se valida contra un hash.
 */
public final class AdminPin {

    private AdminPin() {
    }

    /**
     * @return true solo si el PIN ingresado es el de administrador.
     */
    public static boolean pedir(Component parent) {
        JPasswordField field = new JPasswordField(8);
        field.setFont(UiTheme.BODY);
        field.setPreferredSize(new Dimension(180, 34));

        JLabel msg = new JLabel("Ingresá el PIN de administrador");
        msg.setFont(UiTheme.BODY);
        msg.setForeground(UiTheme.INK);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(msg, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        int opcion = JOptionPane.showConfirmDialog(
                parent,
                panel,
                "PIN de administrador",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) {
            return false;
        }
        String ingresado = new String(field.getPassword());
        if (ComercioConfig.get().matchesAdminPin(ingresado)) {
            return true;
        }
        JOptionPane.showMessageDialog(parent, "PIN incorrecto.",
                "PIN de administrador", JOptionPane.WARNING_MESSAGE);
        return false;
    }
}
