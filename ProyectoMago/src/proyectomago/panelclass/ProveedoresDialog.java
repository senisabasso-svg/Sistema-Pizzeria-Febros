package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Agenda de proveedores de panadería (harina, lácteos, packaging, etc.).
 */
public class ProveedoresDialog extends JDialog {

    private static final String[] RUBROS = {
        "Harinas y granos", "Lácteos", "Fiambres", "Huevos", "Packaging",
        "Bebidas", "Congelados", "Limpieza", "General"
    };

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtTelefono = new JTextField(12);
    private final JComboBox<String> comboRubro = new JComboBox<>(RUBROS);
    private final JTextField txtNota = new JTextField(22);

    public ProveedoresDialog(JFrame owner) {
        super(owner, "Proveedores", true);
        setSize(720, 480);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        model = new DefaultTableModel(new Object[]{"ID", "Proveedor", "Teléfono", "Rubro", "Nota"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(UiTheme.BODY);
        table.setRowHeight(26);
        table.getTableHeader().setFont(UiTheme.BODY_BOLD);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                txtNombre.setText(String.valueOf(model.getValueAt(row, 1)));
                txtTelefono.setText(String.valueOf(model.getValueAt(row, 2)));
                selectRubro(String.valueOf(model.getValueAt(row, 3)));
                txtNota.setText(String.valueOf(model.getValueAt(row, 4)));
            }
        });

        cargarTabla();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(UiTheme.cardBorder());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(label("Nombre"), c);
        c.gridx = 1;
        styleField(txtNombre);
        form.add(txtNombre, c);

        c.gridx = 2;
        form.add(label("Teléfono"), c);
        c.gridx = 3;
        styleField(txtTelefono);
        form.add(txtTelefono, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Rubro"), c);
        c.gridx = 1;
        comboRubro.setFont(UiTheme.BODY);
        comboRubro.setPreferredSize(new Dimension(180, 34));
        form.add(comboRubro, c);

        c.gridx = 2;
        form.add(label("Nota"), c);
        c.gridx = 3;
        styleField(txtNota);
        form.add(txtNota, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnNuevo = primaryButton("Guardar nuevo");
        JButton btnActualizar = secondaryButton("Actualizar");
        JButton btnBorrar = secondaryButton("Eliminar");
        JButton btnCerrar = secondaryButton("Cerrar");
        btnNuevo.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizar());
        btnBorrar.addActionListener(e -> eliminar());
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnNuevo);
        actions.add(btnActualizar);
        actions.add(btnBorrar);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 4;
        form.add(actions, c);
        return form;
    }

    private void selectRubro(String rubro) {
        if (rubro == null || rubro.isBlank()) {
            comboRubro.setSelectedItem("General");
            return;
        }
        for (int i = 0; i < comboRubro.getItemCount(); i++) {
            if (rubro.equalsIgnoreCase(comboRubro.getItemAt(i))) {
                comboRubro.setSelectedIndex(i);
                return;
            }
        }
        comboRubro.addItem(rubro);
        comboRubro.setSelectedItem(rubro);
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, nombre, telefono, rubro, nota FROM proveedores ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    nullToEmpty(rs.getString("telefono")),
                    nullToEmpty(rs.getString("rubro")),
                    nullToEmpty(rs.getString("nota"))
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar proveedores: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarNuevo() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del proveedor.");
            return;
        }
        String sql = "INSERT INTO proveedores (nombre, telefono, rubro, nota) VALUES (?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, txtTelefono.getText().trim());
            ps.setString(3, String.valueOf(comboRubro.getSelectedItem()));
            ps.setString(4, txtNota.getText().trim());
            ps.executeUpdate();
            limpiar();
            cargarTabla();
            JOptionPane.showMessageDialog(this, "Proveedor guardado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un proveedor.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.");
            return;
        }
        String sql = "UPDATE proveedores SET nombre = ?, telefono = ?, rubro = ?, nota = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, txtTelefono.getText().trim());
            ps.setString(3, String.valueOf(comboRubro.getSelectedItem()));
            ps.setString(4, txtNota.getText().trim());
            ps.setInt(5, id);
            ps.executeUpdate();
            cargarTabla();
            JOptionPane.showMessageDialog(this, "Proveedor actualizado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un proveedor.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar este proveedor?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String sql = "DELETE FROM proveedores WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            limpiar();
            cargarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtTelefono.setText("");
        txtNota.setText("");
        comboRubro.setSelectedItem("General");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.BODY_BOLD);
        l.setForeground(UiTheme.INK);
        return l;
    }

    private static void styleField(JTextField f) {
        f.setFont(UiTheme.BODY);
        f.setBorder(UiTheme.fieldBorder());
        f.setBackground(UiTheme.SURFACE_ALT);
        f.setForeground(UiTheme.INK);
        f.setCaretColor(UiTheme.ACCENT);
        f.setPreferredSize(new Dimension(10, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
