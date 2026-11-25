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
 * Alta y edición del menú: SKU / código de barras, tipo, precio, costo y ganancia.
 */
public class ProductosDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtNombre = new JTextField(18);
    private final JTextField txtSku = new JTextField(18);
    private final JComboBox<String> comboTipo = new JComboBox<>(Catalogos.TIPOS_PRODUCTO);
    private final JTextField txtPrecio = new JTextField(8);
    private final JTextField txtCosto = new JTextField(8);
    private final JLabel lblGanancia = new JLabel("Ganancia: —");
    private final Runnable onChanged;

    public ProductosDialog(JFrame owner, Runnable onChanged) {
        super(owner, "Productos y precios", true);
        this.onChanged = onChanged;

        setSize(860, 540);
        setMinimumSize(new Dimension(860, 540));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        model = new DefaultTableModel(
                new Object[]{"ID", "Producto", "SKU", "Tipo", "Precio", "Costo", "Ganancia"}, 0) {
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
        form.add(label("SKU / código de barras"), c);
        c.gridx = 1;
        styleField(txtSku);
        form.add(txtSku, c);

        c.gridx = 0; c.gridy = 2;
        form.add(label("Tipo"), c);
        c.gridx = 1;
        UiTheme.styleCombo(comboTipo);
        comboTipo.setPreferredSize(new Dimension(180, 34));
        comboTipo.setLightWeightPopupEnabled(false);
        form.add(comboTipo, c);

        c.gridx = 0; c.gridy = 3;
        form.add(label("Precio venta"), c);
        c.gridx = 1;
        styleField(txtPrecio);
        form.add(txtPrecio, c);

        c.gridx = 0; c.gridy = 4;
        form.add(label("Costo producción"), c);
        c.gridx = 1;
        styleField(txtCosto);
        txtCosto.setText("0");
        form.add(txtCosto, c);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
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

        c.gridx = 0; c.gridy = 6; c.gridwidth = 2;
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
                txtSku.setText(String.valueOf(model.getValueAt(row, 2)));
                selectTipo(String.valueOf(model.getValueAt(row, 3)));
                txtPrecio.setText(String.valueOf(model.getValueAt(row, 4)).replace("$", ""));
                txtCosto.setText(String.valueOf(model.getValueAt(row, 5)).replace("$", ""));
                actualizarLblGanancia();
            }
        });

        btnGuardar.addActionListener(e -> {
            comboTipo.hidePopup();
            guardarNuevo();
        });
        btnActualizar.addActionListener(e -> {
            comboTipo.hidePopup();
            actualizarProducto();
        });
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
        String sql = "SELECT id, nombre, COALESCE(sku, '') AS sku, COALESCE(tipo, 'Otro') AS tipo, "
                + "precio, COALESCE(costo, 0) AS costo "
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
                    rs.getString("sku"),
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
        String sku = txtSku.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el SKU o escaneá el código de barras.");
            txtSku.requestFocusInWindow();
            return;
        }
        if (nombreOcupado(nombre, null)) {
            JOptionPane.showMessageDialog(this, "Ya existe un producto con ese nombre.");
            return;
        }
        if (skuOcupado(sku, null)) {
            JOptionPane.showMessageDialog(this, "Ya existe un producto con ese código de barras.");
            return;
        }

        String sql = "INSERT INTO menuPrecios (nombre, precio, costo, tipo, sku) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.setInt(3, costo);
            ps.setString(4, tipo);
            ps.setString(5, sku);
            ps.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String mensajeStock = asegurarStockHomónimo(nombre, costo, tipo);
        txtNombre.setText("");
        txtSku.setText("");
        txtPrecio.setText("");
        txtCosto.setText("0");
        comboTipo.setSelectedItem("Bebidas");
        actualizarLblGanancia();
        cargarTabla();
        if (onChanged != null) {
            onChanged.run();
        }
        JOptionPane.showMessageDialog(this, "Producto guardado.\n" + mensajeStock);
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
        String sku = txtSku.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el SKU o escaneá el código de barras.");
            txtSku.requestFocusInWindow();
            return;
        }
        if (nombreOcupado(nombre, id)) {
            JOptionPane.showMessageDialog(this, "Ya existe un producto con ese nombre.");
            return;
        }
        if (skuOcupado(sku, id)) {
            JOptionPane.showMessageDialog(this, "Ya existe un producto con ese código de barras.");
            return;
        }

        String sql = "UPDATE menuPrecios SET nombre = ?, precio = ?, costo = ?, tipo = ?, sku = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setInt(2, precio);
            ps.setInt(3, costo);
            ps.setString(4, tipo);
            ps.setString(5, sku);
            ps.setInt(6, id);
            ps.executeUpdate();
            String mensajeStock = asegurarStockHomónimo(nombre, costo, tipo);
            cargarTabla();
            if (onChanged != null) {
                onChanged.run();
            }
            JOptionPane.showMessageDialog(this, "Producto actualizado.\n" + mensajeStock);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Si no hay stock con el mismo nombre, lo crea con cantidad 0.
     * No pisa costo ni cantidad de un stock ya existente.
     */
    private String asegurarStockHomónimo(String nombre, int costo, String tipo) {
        String findSql = "SELECT id, cantidad FROM stock WHERE lower(nombre) = lower(?) ORDER BY id LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement find = connection.prepareStatement(findSql)) {
            find.setString(1, nombre);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) {
                    return "Quedó vinculado al stock «" + nombre + "» (cantidad actual: "
                            + formatCant(rs.getDouble("cantidad")) + ").";
                }
            }
            String insertSql = "INSERT INTO stock (nombre, cantidad, unidad, costo_unitario, minimo, categoria) "
                    + "VALUES (?, 0, 'u', ?, 0, ?)";
            try (PreparedStatement ins = connection.prepareStatement(insertSql)) {
                ins.setString(1, nombre);
                ins.setInt(2, costo);
                ins.setString(3, Catalogos.categoriaDesdeTipo(tipo));
                ins.executeUpdate();
            }
            return "Quedó vinculado al stock «" + nombre + "» (cantidad actual: 0).";
        } catch (SQLException ex) {
            return "No se pudo vincular al stock: " + ex.getMessage();
        }
    }

    private static String formatCant(double n) {
        if (Math.abs(n - Math.rint(n)) < 0.001) {
            return String.format("%.0f", n);
        }
        return String.format("%.2f", n);
    }

    private boolean skuOcupado(String sku, Integer exceptoId) {
        if (sku == null || sku.isBlank()) {
            return false;
        }
        String sql = "SELECT id FROM menuPrecios WHERE lower(trim(sku)) = lower(?)"
                + (exceptoId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sku.trim());
            if (exceptoId != null) {
                ps.setInt(2, exceptoId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean nombreOcupado(String nombre, Integer exceptoId) {
        String sql = "SELECT id FROM menuPrecios WHERE lower(nombre) = lower(?)"
                + (exceptoId == null ? "" : " AND id <> ?") + " LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            if (exceptoId != null) {
                ps.setInt(2, exceptoId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
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
        f.setBackground(UiTheme.SURFACE_ALT);
        f.setForeground(UiTheme.INK);
        f.setCaretColor(UiTheme.ACCENT);
        f.setPreferredSize(new Dimension(160, 34));
        f.setMinimumSize(new Dimension(120, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
