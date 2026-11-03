package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
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
 * Alta y edición rápida del menú / precios.
 */
public class ProductosDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtPrecio = new JTextField(8);
    private final Runnable onChanged;

    public ProductosDialog(JFrame owner, Runnable onChanged) {
        super(owner, "Productos y precios", true);
        this.onChanged = onChanged;

        setSize(560, 420);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));

        model = new DefaultTableModel(new Object[]{"ID", "Producto", "Precio"}, 0) {
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

        c.gridx = 0; c.gridy = 1;
        form.add(label("Precio"), c);
        c.gridx = 1;
        styleField(txtPrecio);
        form.add(txtPrecio, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnGuardar = primaryButton("Guardar nuevo");
        JButton btnActualizar = secondaryButton("Actualizar precio");
        JButton btnCerrar = secondaryButton("Cerrar");
        actions.add(btnGuardar);
        actions.add(btnActualizar);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        form.add(actions, c);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                txtNombre.setText(String.valueOf(model.getValueAt(row, 1)));
                txtPrecio.setText(String.valueOf(model.getValueAt(row, 2)));
            }
        });

        btnGuardar.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizarPrecio());
        btnCerrar.addActionListener(e -> dispose());

        cargarTabla();
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, nombre, precio FROM menuPrecios ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    (int) Math.round(rs.getDouble("precio"))
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el menú: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarNuevo() {
        String nombre = txtNombre.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del producto.");
            return;
        }
        Integer precio = parsePrecio(precioTxt);
        if (precio == null) {
            JOptionPane.showMessageDialog(this, "Ingresá un precio válido (solo números).");
            return;
        }

        String sql = "INSERT INTO menuPrecios (nombre, precio) VALUES (?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        txtNombre.setText("");
        txtPrecio.setText("");
        cargarTabla();
        if (onChanged != null) {
            onChanged.run();
        }
        JOptionPane.showMessageDialog(this, "Producto guardado.");
    }

    private void actualizarPrecio() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un producto de la lista.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String nombre = txtNombre.getText().trim();
        Integer precio = parsePrecio(txtPrecio.getText().trim());
        if (nombre.isEmpty() || precio == null) {
            JOptionPane.showMessageDialog(this, "Nombre y precio válidos son obligatorios.");
            return;
        }

        String sql = "UPDATE menuPrecios SET nombre = ?, precio = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.setInt(3, id);
            ps.executeUpdate();
            cargarTabla();
            if (onChanged != null) {
                onChanged.run();
            }
            JOptionPane.showMessageDialog(this, "Producto actualizado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Integer parsePrecio(String texto) {
        if (texto == null || !texto.matches("\\d+")) {
            return null;
        }
        return Integer.parseInt(texto);
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
        f.setBackground(UiTheme.SURFACE);
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
