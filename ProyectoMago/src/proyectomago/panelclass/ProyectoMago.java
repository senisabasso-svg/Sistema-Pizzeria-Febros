package proyectomago.panelclass;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class ProyectoMago {

    public static void main(String[] args) {
        // Cambiar el Look and Feel a Nimbus (moderno)
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Crear el frame
        JFrame frame = new JFrame("Proyecto Mago");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(false); // Mantener decoraciones del sistema (barra de título)
        frame.setResizable(false); // Evitar el cambio de tamaño

        // Crear una instancia del panel
        PrincipalPanel panel = new PrincipalPanel();

        // Añadir el panel al frame
        frame.add(panel);

        // Establecer un tamaño preferido (ajustado para dar más espacio visual)
        frame.setPreferredSize(new Dimension(900, 650));

        // Ajustar el tamaño y hacerlo visible
        frame.pack();
        frame.setLocationRelativeTo(null); // Centrar la ventana
        frame.setVisible(true);

        // Personalizar la apariencia del panel (estilo moderno y claro)
        panel.setBackground(new Color(123, 155, 254)); // Fondo más suave

        // Personalización de componentes adicionales
        customizeComponents();
    }

    private static void customizeComponents() {
        // Estilo para botones
        UIManager.put("Button.background", new Color(0, 123, 255)); // Fondo azul más vivo
        UIManager.put("Button.foreground", Color.WHITE); // Texto blanco en botones
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 16)); // Fuente más moderna
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2), 
            BorderFactory.createEmptyBorder(10, 20, 10, 20) // Relleno dentro del botón
        ));
        UIManager.put("Button.focus", new Color(0, 123, 255)); // Resaltar cuando está en foco

        // Estilo para campos de texto (si los hay)
        UIManager.put("TextField.background", new  Color(0, 123, 255)); // Fondo blanco
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14)); // Fuente moderna
        UIManager.put("TextField.border", BorderFactory.createLineBorder(new Color(200, 200, 200), 1)); // Borde gris claro

        // Estilo para etiquetas
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Label.foreground", new Color(51, 51, 51)); // Texto oscuro y legible
    }

    private static void createTableIfNotExists(Connection conn) {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS menuPrecios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "precio REAL NOT NULL"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla 'menuPrecios' verificada o creada.");
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla: " + e.getMessage());
        }
    }
}
