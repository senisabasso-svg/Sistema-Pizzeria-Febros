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
 * Merma / sobrante del día (clave en panadería).
 */
public class MermaDialog extends JDialog {

    private static final String[] MOTIVOS = {
        "Sobrante del día", "Quemado / fallado", "Vencido", "Rotura", "Degustación", "Otro"
    };

    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<KeyValue> comboItem = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(8);
    private final JComboBox<String> comboMotivo = new JComboBox<>(MOTIVOS);
    private final JLabel lblResumen = new JLabel(" ");

    public MermaDialog(JFrame owner) {
        super(owner, "Merma del día", true);
        setSize(720, 500);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        model = new DefaultTableModel(
                new Object[]{"ID", "Fecha", "Ítem", "Cant.", "Motivo", "Costo est."}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(UiTheme.BODY);
        table.setRowHeight(26);
        table.getTableHeader().setFont(UiTheme.BODY_BOLD);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(buildForm(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cargarItems();
        cargarTabla();
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
        form.add(label("Ítem (stock)"), c);
        c.gridx = 1;
        comboItem.setFont(UiTheme.BODY);
        comboItem.setPreferredSize(new Dimension(220, 34));
        form.add(comboItem, c);

        c.gridx = 2;
        form.add(label("Cantidad"), c);
        c.gridx = 3;
        styleField(txtCantidad);
        form.add(txtCantidad, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Motivo"), c);
        c.gridx = 1;
        comboMotivo.setFont(UiTheme.BODY);
        comboMotivo.setPreferredSize(new Dimension(220, 34));
        form.add(comboMotivo, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnReg = primaryButton("Registrar merma");
        btnReg.addActionListener(e -> registrar());
        actions.add(btnReg);

        c.gridx = 2; c.gridy = 1; c.gridwidth = 2;
        form.add(actions, c);
        return form;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblResumen.setFont(UiTheme.HEADING);
        lblResumen.setForeground(UiTheme.ACCENT);
        JButton btnCerrar = secondaryButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        footer.add(lblResumen, BorderLayout.WEST);
        footer.add(btnCerrar, BorderLayout.EAST);
        return footer;
    }

    private void cargarItems() {
        DefaultComboBoxModel<KeyValue> modelCombo = new DefaultComboBoxModel<>();
        String sql = "SELECT id, nombre, COALESCE(unidad,'u') AS unidad, COALESCE(costo_unitario,0) AS costo "
                + "FROM stock ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString("nombre") + " (" + rs.getString("unidad") + ")";
                modelCombo.addElement(new KeyValue(String.valueOf(rs.getInt("id")), label));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar stock: " + ex.getMessage());
        }
        comboItem.setModel(modelCombo);
    }

    private void registrar() {
        KeyValue item = (KeyValue) comboItem.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "No hay ítems de stock. Cargá stock primero.");
            return;
        }
        Double cant = parseNum(txtCantidad.getText().trim());
        if (cant == null || cant <= 0) {
            JOptionPane.showMessageDialog(this, "Ingresá una cantidad válida.");
            return;
        }

        int stockId = item.getKey();
        String motivo = String.valueOf(comboMotivo.getSelectedItem());
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String sqlMeta = "SELECT nombre, COALESCE(unidad,'u') AS unidad, COALESCE(costo_unitario,0) AS costo, cantidad "
                + "FROM stock WHERE id = ?";
        String sqlMerma = "INSERT INTO mermas (fecha, item_nombre, cantidad, unidad, motivo, costo_estimado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE stock SET cantidad = MAX(cantidad - ?, 0) WHERE id = ?";

        try (Connection connection = new conection().getConnection()) {
            String nombre;
            String unidad;
            double costoUnit;
            double stockActual;
            try (PreparedStatement ps = connection.prepareStatement(sqlMeta)) {
                ps.setInt(1, stockId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Ítem no encontrado.");
                        return;
                    }
                    nombre = rs.getString("nombre");
                    unidad = rs.getString("unidad");
                    costoUnit = rs.getDouble("costo");
                    stockActual = rs.getDouble("cantidad");
                }
            }
            if (cant > stockActual) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "La merma (" + formatNum(cant) + ") supera el stock (" + formatNum(stockActual)
                                + "). ¿Registrar igual (stock quedará en 0)?",
                        "Stock insuficiente", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            double costoEst = cant * costoUnit;
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(sqlMerma)) {
                    ps.setString(1, ahora);
                    ps.setString(2, nombre);
                    ps.setDouble(3, cant);
                    ps.setString(4, unidad);
                    ps.setString(5, motivo);
                    ps.setDouble(6, costoEst);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(sqlStock)) {
                    ps.setDouble(1, cant);
                    ps.setInt(2, stockId);
                    ps.executeUpdate();
                }
                connection.commit();
                txtCantidad.setText("");
                cargarTabla();
                JOptionPane.showMessageDialog(this,
                        "Merma registrada: " + formatNum(cant) + " " + unidad + " de " + nombre
                                + "\nCosto estimado $" + String.format("%.0f", costoEst));
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar merma: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, fecha, item_nombre, cantidad, unidad, motivo, costo_estimado "
                + "FROM mermas ORDER BY id DESC LIMIT 100";
        double sumaCosto = 0;
        double sumaCant = 0;
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String fecha = rs.getString("fecha");
                String show = fecha != null && fecha.length() >= 16 ? fecha.substring(0, 16) : fecha;
                double cant = rs.getDouble("cantidad");
                double costo = rs.getDouble("costo_estimado");
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    show,
                    rs.getString("item_nombre"),
                    formatNum(cant) + " " + rs.getString("unidad"),
                    rs.getString("motivo"),
                    "$" + String.format("%.0f", costo)
                });
                sumaCosto += costo;
                sumaCant += cant;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar mermas: " + ex.getMessage());
            return;
        }
        lblResumen.setText("Merma listada: " + formatNum(sumaCant) + " u. · Costo est. $"
                + String.format("%.0f", sumaCosto));
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
        f.setBackground(UiTheme.SURFACE);
        f.setPreferredSize(new Dimension(100, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
