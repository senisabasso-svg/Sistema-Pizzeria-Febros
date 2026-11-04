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
 * Alta y edición del menú: tipo, precio, costo de producción y ganancia.
 */
public class ProductosDialog extends JDialog {

    private static final String[] TIPOS = {"Pizza", "Empanada", "Bebida", "Extra", "Otro"};

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtNombre = new JTextField(18);
    private final JComboBox<String> comboTipo = new JComboBox<>(TIPOS);
    private final JTextField txtPrecio = new JTextField(8);
    private final JTextField txtCosto = new JTextField(8);
    private final JLabel lblGanancia = new JLabel("Ganancia: —");
    private final Runnable onChanged;

    public ProductosDialog(JFrame owner, Runnable onChanged) {
        super(owner, "Productos y precios", true);
        this.onChanged = onChanged;

        setSize(700, 500);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(
                new Object[]{"ID", "Producto", "Tipo", "Precio", "Costo", "Ganancia"}, 0) {
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
        form.add(label("Tipo"), c);
        c.gridx = 1;
        comboTipo.setFont(UiTheme.BODY);
        comboTipo.setPreferredSize(new Dimension(180, 34));
        form.add(comboTipo, c);

        c.gridx = 0; c.gridy = 2;
        form.add(label("Precio venta"), c);
        c.gridx = 1;
        styleField(txtPrecio);
        form.add(txtPrecio, c);

        c.gridx = 0; c.gridy = 3;
        form.add(label("Costo producción"), c);
        c.gridx = 1;
        styleField(txtCosto);
        txtCosto.setText("0");
        form.add(txtCosto, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        lblGanancia.setFont(UiTheme.BODY_BOLD);
        lblGanancia.setForeground(UiTheme.SUCCESS);
        form.add(lblGanancia, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnGuardar = primaryButton("Guardar nuevo");
        JButton btnActualizar = secondaryButton("Actualizar");
        JButton btnCerrar = secondaryButton("Cerrar");
        actions.add(btnGuardar);
        actions.add(btnActualizar);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        form.add(actions, c);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(form, BorderLayout.SOUTH);

        javax.swing.event.DocumentListener recalc = new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarLblGanancia(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarLblGanancia(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarLblGanancia(); }
        };
        txtPrecio.getDocument().addDocumentListener(recalc);
        txtCosto.getDocument().addDocumentListener(recalc);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                txtNombre.setText(String.valueOf(model.getValueAt(row, 1)));
                selectTipo(String.valueOf(model.getValueAt(row, 2)));
                txtPrecio.setText(String.valueOf(model.getValueAt(row, 3)).replace("$", ""));
                txtCosto.setText(String.valueOf(model.getValueAt(row, 4)).replace("$", ""));
                actualizarLblGanancia();
            }
        });

        btnGuardar.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizarProducto());
        btnCerrar.addActionListener(e -> dispose());

        cargarTabla();
    }

    private void selectTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            comboTipo.setSelectedItem("Otro");
            return;
        }
        for (int i = 0; i < comboTipo.getItemCount(); i++) {
            if (tipo.equalsIgnoreCase(comboTipo.getItemAt(i))) {
                comboTipo.setSelectedIndex(i);
                return;
            }
        }
        comboTipo.addItem(tipo);
        comboTipo.setSelectedItem(tipo);
    }

    private void actualizarLblGanancia() {
        Integer precio = parseMonto(txtPrecio.getText().trim());
        Integer costo = parseMonto(txtCosto.getText().trim());
        if (precio == null || costo == null) {
            lblGanancia.setText("Ganancia: —");
            return;
        }
        int ganancia = precio - costo;
        lblGanancia.setText("Ganancia aprox.: $" + ganancia
                + (precio > 0 ? "  (" + Math.round(ganancia * 100.0 / precio) + "%)" : ""));
        lblGanancia.setForeground(ganancia >= 0 ? UiTheme.SUCCESS : UiTheme.ACCENT);
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, nombre, COALESCE(tipo, 'Otro') AS tipo, precio, COALESCE(costo, 0) AS costo "
                + "FROM menuPrecios ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int precio = (int) Math.round(rs.getDouble("precio"));
                int costo = (int) Math.round(rs.getDouble("costo"));
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("tipo"),
                    "$" + precio,
                    "$" + costo,
                    "$" + (precio - costo)
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar el menú: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarNuevo() {
        String nombre = txtNombre.getText().trim();
        String tipo = String.valueOf(comboTipo.getSelectedItem());
        Integer precio = parseMonto(txtPrecio.getText().trim());
        Integer costo = parseMonto(txtCosto.getText().trim());
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del producto.");
            return;
        }
        if (precio == null) {
            JOptionPane.showMessageDialog(this, "Ingresá un precio de venta válido.");
            return;
        }
        if (costo == null) {
            costo = 0;
        }
        if (tipo == null || tipo.isBlank()) {
            tipo = "Otro";
        }

        String sql = "INSERT INTO menuPrecios (nombre, precio, costo, tipo) VALUES (?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.setInt(3, costo);
            ps.setString(4, tipo);
            ps.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        txtNombre.setText("");
        txtPrecio.setText("");
        txtCosto.setText("0");
        comboTipo.setSelectedItem("Pizza");
        actualizarLblGanancia();
        cargarTabla();
        if (onChanged != null) {
            onChanged.run();
        }
        JOptionPane.showMessageDialog(this, "Producto guardado.");
    }

    private void actualizarProducto() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un producto de la lista.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String nombre = txtNombre.getText().trim();
        String tipo = String.valueOf(comboTipo.getSelectedItem());
        Integer precio = parseMonto(txtPrecio.getText().trim());
        Integer costo = parseMonto(txtCosto.getText().trim());
        if (nombre.isEmpty() || precio == null) {
            JOptionPane.showMessageDialog(this, "Nombre y precio de venta son obligatorios.");
            return;
        }
        if (costo == null) {
            costo = 0;
        }
        if (tipo == null || tipo.isBlank()) {
            tipo = "Otro";
        }

        String sql = "UPDATE menuPrecios SET nombre = ?, precio = ?, costo = ?, tipo = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.setInt(3, costo);
            ps.setString(4, tipo);
            ps.setInt(5, id);
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

    private Integer parseMonto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(",", ".").trim();
        if (!limpio.matches("\\d+(\\.\\d+)?")) {
            return null;
        }
        return (int) Math.round(Double.parseDouble(limpio));
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
        f.setPreferredSize(new Dimension(10, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
