package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Alta y baja de vendedores que se eligen al abrir la caja.
 */
public class VendedoresDialog extends JDialog {

    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(model);
    private final JTextField txtNombre = new JTextField(18);

    public VendedoresDialog(JFrame owner) {
        super(owner, "Vendedores", true);
        setSize(420, 420);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFont(UiTheme.BODY);
        lista.setBackground(UiTheme.SURFACE_ALT);
        lista.setForeground(UiTheme.INK);
        lista.setFixedCellHeight(28);

        JLabel titulo = new JLabel("Estos nombres se eligen al abrir la caja.");
        titulo.setFont(UiTheme.SMALL);
        titulo.setForeground(UiTheme.MUTED);

        JScrollPane scroll = new JScrollPane(lista);
        UiTheme.styleScroll(scroll);

        JPanel norte = new JPanel(new BorderLayout(0, 8));
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(scroll, BorderLayout.CENTER);
        add(norte, BorderLayout.CENTER);

        txtNombre.setFont(UiTheme.BODY);
        txtNombre.setBorder(UiTheme.fieldBorder());
        txtNombre.setBackground(UiTheme.SURFACE_ALT);
        txtNombre.setForeground(UiTheme.INK);
        txtNombre.setCaretColor(UiTheme.ACCENT);
        txtNombre.setPreferredSize(new Dimension(10, 34));

        JButton btnAgregar = UiTheme.primaryButton("Agregar");
        JButton btnQuitar = UiTheme.secondaryButton("Quitar");
        JButton btnCerrar = UiTheme.secondaryButton("Cerrar");
        btnAgregar.addActionListener(e -> agregar());
        btnQuitar.addActionListener(e -> quitar());
        btnCerrar.addActionListener(e -> dispose());
        txtNombre.addActionListener(e -> agregar());

        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setOpaque(false);
        form.add(txtNombre, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnQuitar);
        actions.add(btnAgregar);
        actions.add(btnCerrar);

        JPanel sur = new JPanel(new BorderLayout(0, 10));
        sur.setOpaque(false);
        sur.add(form, BorderLayout.NORTH);
        sur.add(actions, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        cargar();
    }

    private void cargar() {
        model.clear();
        String sql = "SELECT nombre FROM vendedores ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addElement(rs.getString("nombre"));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudieron cargar los vendedores: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del vendedor.");
            return;
        }
        if (existe(nombre)) {
            JOptionPane.showMessageDialog(this, "Ya existe un vendedor con ese nombre.");
            return;
        }
        String sql = "INSERT INTO vendedores (nombre) VALUES (?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            txtNombre.setText("");
            cargar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitar() {
        String nombre = lista.getSelectedValue();
        if (nombre == null) {
            JOptionPane.showMessageDialog(this, "Seleccioná un vendedor de la lista.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Quitar a " + nombre + "?", "Vendedores", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = "DELETE FROM vendedores WHERE lower(nombre) = lower(?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            cargar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo quitar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existe(String nombre) {
        String sql = "SELECT id FROM vendedores WHERE lower(nombre) = lower(?) LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}
