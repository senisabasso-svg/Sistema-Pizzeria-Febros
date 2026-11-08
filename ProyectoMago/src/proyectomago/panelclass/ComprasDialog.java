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
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
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
 * Compra a proveedor: suma stock y deja historial.
 */
public class ComprasDialog extends JDialog {

    private final DefaultTableModel modelHist;
    private final JTable tableHist;
    private final JComboBox<KeyValue> comboProveedor = new JComboBox<>();
    private final JComboBox<KeyValue> comboStock = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(8);
    private final JTextField txtCosto = new JTextField(8);
    private final JTextField txtNota = new JTextField(18);
    private final JLabel lblHint = new JLabel(" ");
    private boolean stockListenerReady;

    public ComprasDialog(JFrame owner) {
        super(owner, "Compras a proveedores", true);
        setSize(760, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        modelHist = new DefaultTableModel(
                new Object[]{"ID", "Fecha", "Proveedor", "Total", "Nota"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableHist = new JTable(modelHist);
        tableHist.setFont(UiTheme.BODY);
        tableHist.setRowHeight(26);
        tableHist.getTableHeader().setFont(UiTheme.BODY_BOLD);
        tableHist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableHist.getColumnModel().getColumn(0).setMinWidth(0);
        tableHist.getColumnModel().getColumn(0).setMaxWidth(0);
        tableHist.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(tableHist), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cargarProveedores();
        cargarStock();
        if (!stockListenerReady) {
            comboStock.addActionListener(e -> precargarCosto());
            stockListenerReady = true;
        }
        cargarHistorial();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(UiTheme.cardBorder());
        form.setBackground(UiTheme.SURFACE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(label("Proveedor"), c);
        c.gridx = 1;
        styleCombo(comboProveedor);
        form.add(comboProveedor, c);

        c.gridx = 2;
        form.add(label("Ítem de stock"), c);
        c.gridx = 3;
        styleCombo(comboStock);
        form.add(comboStock, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Cantidad"), c);
        c.gridx = 1;
        styleField(txtCantidad);
        form.add(txtCantidad, c);

        c.gridx = 2;
        form.add(label("Costo unit."), c);
        c.gridx = 3;
        styleField(txtCosto);
        form.add(txtCosto, c);

        c.gridx = 0; c.gridy = 2;
        form.add(label("Nota"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        styleField(txtNota);
        form.add(txtNota, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnRegistrar = primaryButton("Registrar compra e ingresar stock");
        JButton btnRefresh = secondaryButton("Actualizar listas");
        btnRegistrar.addActionListener(e -> registrarCompra());
        btnRefresh.addActionListener(e -> {
            cargarProveedores();
            cargarStock();
            cargarHistorial();
        });
        actions.add(btnRegistrar);
        actions.add(btnRefresh);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        form.add(actions, c);
        return form;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblHint.setFont(UiTheme.SMALL);
        lblHint.setForeground(UiTheme.MUTED);
        lblHint.setText("La compra suma cantidad al stock y actualiza el costo unitario del ítem.");
        JButton btnCerrar = secondaryButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        footer.add(lblHint, BorderLayout.WEST);
        footer.add(btnCerrar, BorderLayout.EAST);
        return footer;
    }

    private void cargarProveedores() {
        DefaultComboBoxModel<KeyValue> model = new DefaultComboBoxModel<>();
        String sql = "SELECT id, nombre FROM proveedores ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addElement(new KeyValue(String.valueOf(rs.getInt("id")), rs.getString("nombre")));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error proveedores: " + ex.getMessage());
        }
        comboProveedor.setModel(model);
    }

    private void cargarStock() {
        DefaultComboBoxModel<KeyValue> model = new DefaultComboBoxModel<>();
        String sql = "SELECT id, nombre, COALESCE(costo_unitario,0) AS costo FROM stock ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                KeyValue kv = new KeyValue(String.valueOf(rs.getInt("id")), rs.getString("nombre"));
                model.addElement(kv);
            }
            // preload first cost
            comboStock.setModel(model);
            if (model.getSize() > 0) {
                comboStock.setSelectedIndex(0);
                precargarCosto();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error stock: " + ex.getMessage());
        }
    }

    private void precargarCosto() {
        KeyValue sel = (KeyValue) comboStock.getSelectedItem();
        if (sel == null) {
            return;
        }
        String sql = "SELECT COALESCE(costo_unitario,0) FROM stock WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, sel.getKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtCosto.setText(String.format("%.0f", rs.getDouble(1)));
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void registrarCompra() {
        KeyValue prov = (KeyValue) comboProveedor.getSelectedItem();
        KeyValue stock = (KeyValue) comboStock.getSelectedItem();
        if (prov == null) {
            JOptionPane.showMessageDialog(this, "Cargá al menos un proveedor antes de comprar.");
            return;
        }
        if (stock == null) {
            JOptionPane.showMessageDialog(this, "Cargá ítems en Stock antes de registrar una compra.");
            return;
        }
        Double cant = parseNum(txtCantidad.getText().trim());
        Double costo = parseNum(txtCosto.getText().trim());
        if (cant == null || cant <= 0) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida.");
            return;
        }
        if (costo == null || costo < 0) {
            JOptionPane.showMessageDialog(this, "Costo unitario inválido.");
            return;
        }

        double subtotal = cant * costo;
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        int proveedorId = prov.getKey();
        int stockId = stock.getKey();
        String nota = txtNota.getText().trim();

        String sqlCompra = "INSERT INTO compras (proveedor_id, proveedor_nombre, fecha, total, nota) VALUES (?, ?, ?, ?, ?)";
        String sqlDet = "INSERT INTO compra_detalle (compra_id, stock_id, item_nombre, cantidad, costo_unit, subtotal, unidad) "
                + "VALUES (?, ?, ?, ?, ?, ?, (SELECT COALESCE(unidad,'u') FROM stock WHERE id = ?))";
        String sqlStock = "UPDATE stock SET cantidad = cantidad + ?, costo_unitario = ?, proveedor_id = ? WHERE id = ?";

        try (Connection connection = new conection().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int compraId;
                try (PreparedStatement ps = connection.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, proveedorId);
                    ps.setString(2, prov.getValue());
                    ps.setString(3, ahora);
                    ps.setDouble(4, subtotal);
                    ps.setString(5, nota);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se obtuvo id de compra");
                        }
                        compraId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = connection.prepareStatement(sqlDet)) {
                    ps.setInt(1, compraId);
                    ps.setInt(2, stockId);
                    ps.setString(3, stock.getValue());
                    ps.setDouble(4, cant);
                    ps.setDouble(5, costo);
                    ps.setDouble(6, subtotal);
                    ps.setInt(7, stockId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(sqlStock)) {
                    ps.setDouble(1, cant);
                    ps.setDouble(2, costo);
                    ps.setInt(3, proveedorId);
                    ps.setInt(4, stockId);
                    ps.executeUpdate();
                }
                connection.commit();
                txtCantidad.setText("");
                txtNota.setText("");
                cargarHistorial();
                JOptionPane.showMessageDialog(this,
                        "Compra registrada.\n+" + formatNum(cant) + " de " + stock.getValue()
                                + "\nTotal $" + String.format("%.0f", subtotal));
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar compra: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarHistorial() {
        modelHist.setRowCount(0);
        String sql = "SELECT id, fecha, proveedor_nombre, total, nota FROM compras ORDER BY id DESC LIMIT 80";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fecha = rs.getString("fecha");
                String show = fecha != null && fecha.length() >= 16 ? fecha.substring(0, 16) : fecha;
                modelHist.addRow(new Object[]{
                    rs.getInt("id"),
                    show,
                    rs.getString("proveedor_nombre"),
                    "$" + String.format("%.0f", rs.getDouble("total")),
                    rs.getString("nota") == null ? "" : rs.getString("nota")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error historial: " + ex.getMessage());
        }
    }

    private static Double parseNum(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(texto.replace(",", ".").replace("$", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatNum(double n) {
        if (Math.abs(n - Math.rint(n)) < 0.001) {
            return String.format("%.0f", n);
        }
        return String.format("%.2f", n);
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
        f.setPreferredSize(new Dimension(100, 34));
    }

    private static void styleCombo(JComboBox<?> combo) {
        UiTheme.styleCombo(combo);
        combo.setPreferredSize(new Dimension(200, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
