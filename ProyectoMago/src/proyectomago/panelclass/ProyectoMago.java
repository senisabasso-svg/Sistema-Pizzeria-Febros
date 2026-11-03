package proyectomago.panelclass;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Dimension;

public class ProyectoMago {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            String nombre = ComercioConfig.get().getNombreComercio();
            JFrame frame = new JFrame(nombre + " · Sistema de caja");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(980, 640));
            frame.setContentPane(new PrincipalPanel());
            frame.pack();
            frame.setSize(1040, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
