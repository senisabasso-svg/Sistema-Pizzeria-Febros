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
 * Stock de panadería: ingredientes, productos terminados y packaging.
 */
public class StockDialog extends JDialog {

    private static final String[] CATEGORIAS = {
        "Ingrediente", "Producto terminado", "Packaging", "Otro"
    };

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtNombre = new JTextField(16);
    private final JTextField txtCantidad = new JTextField(8);
    private final JTextField txtUnidad = new JTextField(6);
    private final JTextField txtCosto = new JTextField(8);
    private final JTextField txtMinimo = new JTextField(8);
    private final JComboBox<String> comboCategoria = new JComboBox<>(CATEGORIAS);

    public StockDialog(JFrame owner) {
        super(owner, "Stock panadería", true);
        setSize(760, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        model = new DefaultTableModel(
                new Object[]{"ID", "Ítem", "Categoría", "Cant.", "Unidad", "Costo u.", "Mínimo", "Alerta"}, 0) {
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
                selectCategoria(String.valueOf(model.getValueAt(row, 2)));
                txtCantidad.setText(String.valueOf(model.getValueAt(row, 3)));
                txtUnidad.setText(String.valueOf(model.getValueAt(row, 4)));
                txtCosto.setText(String.valueOf(model.getValueAt(row, 5)).replace("$", ""));
                txtMinimo.setText(String.valueOf(model.getValueAt(row, 6)));
            }
        });

        cargarTabla();
    }

    private void selectCategoria(String cat) {
        if (cat == null || cat.isBlank()) {
            comboCategoria.setSelectedItem("Ingrediente");
            return;
        }
        for (int i = 0; i < comboCategoria.getItemCount(); i++) {
            if (cat.equalsIgnoreCase(comboCategoria.getItemAt(i))) {
                comboCategoria.setSelectedIndex(i);
                return;
            }
        }
        comboCategoria.addItem(cat);
        comboCategoria.setSelectedItem(cat);
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
        form.add(label("Categoría"), c);
        c.gridx = 3;
        comboCategoria.setFont(UiTheme.BODY);
        comboCategoria.setPreferredSize(new Dimension(160, 34));
        form.add(comboCategoria, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Cantidad"), c);
        c.gridx = 1;
        styleField(txtCantidad);
        form.add(txtCantidad, c);

        c.gridx = 2;
        form.add(label("Unidad"), c);
        c.gridx = 3;
        styleField(txtUnidad);
        txtUnidad.setText("kg");
        form.add(txtUnidad, c);

        c.gridx = 0; c.gridy = 2;
        form.add(label("Costo unit."), c);
        c.gridx = 1;
        styleField(txtCosto);
        form.add(txtCosto, c);

        c.gridx = 2;
        form.add(label("Mínimo"), c);
        c.gridx = 3;
        styleField(txtMinimo);
        txtMinimo.setText("0");
        form.add(txtMinimo, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnNuevo = primaryButton("Guardar nuevo");
        JButton btnActualizar = secondaryButton("Actualizar");
        JButton btnSumar = secondaryButton("+ Entrada");
        JButton btnRestar = secondaryButton("− Salida");
        JButton btnCerrar = secondaryButton("Cerrar");
        btnNuevo.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizar());
        btnSumar.addActionListener(e -> ajustarCantidad(true));
        btnRestar.addActionListener(e -> ajustarCantidad(false));
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnNuevo);
        actions.add(btnActualizar);
        actions.add(btnSumar);
        actions.add(btnRestar);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 4;
        form.add(actions, c);
        return form;
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, nombre, COALESCE(categoria,'Ingrediente') AS categoria, cantidad, unidad, "
                + "costo_unitario, minimo FROM stock ORDER BY categoria, nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double cant = rs.getDouble("cantidad");
                double min = rs.getDouble("minimo");
                String alerta = cant <= min ? "BAJO" : "";
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("categoria"),
                    formatNum(cant),
                    rs.getString("unidad"),
                    "$" + formatNum(rs.getDouble("costo_unitario")),
                    formatNum(min),
                    alerta
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar stock: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarNuevo() {
        String nombre = txtNombre.getText().trim();
        Double cantidad = parseNum(txtCantidad.getText().trim());
        String unidad = txtUnidad.getText().trim();
        Double costo = parseNum(txtCosto.getText().trim());
        Double minimo = parseNum(txtMinimo.getText().trim());
        String categoria = String.valueOf(comboCategoria.getSelectedItem());

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del ítem.");
            return;
        }
        if (cantidad == null || costo == null || minimo == null) {
            JOptionPane.showMessageDialog(this, "Cantidad, costo y mínimo deben ser números.");
            return;
        }
        if (unidad.isEmpty()) {
            unidad = "u";
        }

        String sql = "INSERT INTO stock (nombre, cantidad, unidad, costo_unitario, minimo, categoria) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, cantidad);
            ps.setString(3, unidad);
            ps.setDouble(4, costo);
            ps.setDouble(5, minimo);
            ps.setString(6, categoria);
            ps.executeUpdate();
            limpiarForm();
            cargarTabla();
            JOptionPane.showMessageDialog(this, "Ítem de stock guardado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un ítem de la lista.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String nombre = txtNombre.getText().trim();
        Double cantidad = parseNum(txtCantidad.getText().trim());
        String unidad = txtUnidad.getText().trim();
        Double costo = parseNum(txtCosto.getText().trim());
        Double minimo = parseNum(txtMinimo.getText().trim());
        String categoria = String.valueOf(comboCategoria.getSelectedItem());

        if (nombre.isEmpty() || cantidad == null || costo == null || minimo == null) {
            JOptionPane.showMessageDialog(this, "Completá nombre, cantidad, costo y mínimo.");
            return;
        }
        if (unidad.isEmpty()) {
            unidad = "u";
        }

        String sql = "UPDATE stock SET nombre = ?, cantidad = ?, unidad = ?, costo_unitario = ?, "
                + "minimo = ?, categoria = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, cantidad);
            ps.setString(3, unidad);
            ps.setDouble(4, costo);
            ps.setDouble(5, minimo);
            ps.setString(6, categoria);
            ps.setInt(7, id);
            ps.executeUpdate();
            cargarTabla();
            JOptionPane.showMessageDialog(this, "Stock actualizado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajustarCantidad(boolean entrada) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un ítem.");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String titulo = entrada ? "Entrada de stock" : "Salida de stock";
        String deltaTxt = JOptionPane.showInputDialog(this, "Cantidad a " + (entrada ? "sumar" : "restar") + ":", "1");
        if (deltaTxt == null) {
            return;
        }
        Double delta = parseNum(deltaTxt.trim());
        if (delta == null || delta <= 0) {
            JOptionPane.showMessageDialog(this, "Ingresá una cantidad válida.");
            return;
        }
        if (!entrada) {
            delta = -delta;
        }

        String sql = "UPDATE stock SET cantidad = cantidad + ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, delta);
            ps.setInt(2, id);
            ps.executeUpdate();
            cargarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error en " + titulo + ": " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarForm() {
        txtNombre.setText("");
        txtCantidad.setText("");
        txtUnidad.setText("kg");
        txtCosto.setText("");
        txtMinimo.setText("0");
        comboCategoria.setSelectedItem("Ingrediente");
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
        f.setPreferredSize(new Dimension(10, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
