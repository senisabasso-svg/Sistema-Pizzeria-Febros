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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Agenda de clientes: celular + documento (y datos opcionales).
 */
public class ClientesDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField txtBuscar = new JTextField(16);
    private final JTextField txtNombre = new JTextField(16);
    private final JTextField txtCelular = new JTextField(12);
    private final JTextField txtDocumento = new JTextField(12);
    private final JTextField txtDireccion = new JTextField(18);
    private final JTextField txtNota = new JTextField(18);
    private final Runnable onChanged;

    public ClientesDialog(JFrame owner) {
        this(owner, null);
    }

    public ClientesDialog(JFrame owner, Runnable onChanged) {
        super(owner, "Clientes", true);
        this.onChanged = onChanged;
        setSize(760, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        model = new DefaultTableModel(
                new Object[]{"ID", "Nombre", "Celular", "Documento", "Dirección", "Nota"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(UiTheme.BODY);
        table.setRowHeight(28);
        table.getTableHeader().setFont(UiTheme.BODY_BOLD);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setPreferredWidth(0);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JPanel north = new JPanel(new BorderLayout(8, 0));
        north.setOpaque(false);
        JLabel lbl = new JLabel("Buscar (nombre / celular / documento)");
        lbl.setFont(UiTheme.SMALL);
        lbl.setForeground(UiTheme.MUTED);
        styleField(txtBuscar);
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        north.add(lbl, BorderLayout.NORTH);
        north.add(txtBuscar, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.convertRowIndexToModel(table.getSelectedRow());
                txtNombre.setText(String.valueOf(model.getValueAt(row, 1)));
                txtCelular.setText(String.valueOf(model.getValueAt(row, 2)));
                txtDocumento.setText(String.valueOf(model.getValueAt(row, 3)));
                txtDireccion.setText(String.valueOf(model.getValueAt(row, 4)));
                txtNota.setText(String.valueOf(model.getValueAt(row, 5)));
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
        form.add(label("Celular *"), c);
        c.gridx = 3;
        styleField(txtCelular);
        form.add(txtCelular, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Documento *"), c);
        c.gridx = 1;
        styleField(txtDocumento);
        form.add(txtDocumento, c);

        c.gridx = 2;
        form.add(label("Dirección"), c);
        c.gridx = 3;
        styleField(txtDireccion);
        form.add(txtDireccion, c);

        c.gridx = 0; c.gridy = 2;
        form.add(label("Nota"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        styleField(txtNota);
        form.add(txtNota, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnNuevo = primaryButton("Guardar nuevo");
        JButton btnActualizar = secondaryButton("Actualizar");
        JButton btnEliminar = secondaryButton("Eliminar");
        JButton btnCerrar = secondaryButton("Cerrar");
        btnNuevo.addActionListener(e -> guardarNuevo());
        btnActualizar.addActionListener(e -> actualizar());
        btnEliminar.addActionListener(e -> eliminar());
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnNuevo);
        actions.add(btnActualizar);
        actions.add(btnEliminar);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        form.add(actions, c);
        return form;
    }

    private void filtrar() {
        String q = txtBuscar.getText().trim();
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
        }
    }

    private void cargarTabla() {
        model.setRowCount(0);
        String sql = "SELECT id, nombre, celular, documento, direccion, nota "
                + "FROM clientes ORDER BY nombre COLLATE NOCASE, id DESC";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    nullToEmpty(rs.getString("nombre")),
                    nullToEmpty(rs.getString("celular")),
                    nullToEmpty(rs.getString("documento")),
                    nullToEmpty(rs.getString("direccion")),
                    nullToEmpty(rs.getString("nota"))
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo cargar clientes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarNuevo() {
        String celular = txtCelular.getText().trim();
        String documento = txtDocumento.getText().trim();
        if (celular.isEmpty() || documento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Celular y documento son obligatorios.");
            return;
        }
        if (existeDocumentoOCelular(documento, celular, null)) {
            JOptionPane.showMessageDialog(this, "Ya existe un cliente con ese celular o documento.");
            return;
        }

        String sql = "INSERT INTO clientes (nombre, celular, documento, direccion, nota) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, celular);
            ps.setString(3, documento);
            ps.setString(4, txtDireccion.getText().trim());
            ps.setString(5, txtNota.getText().trim());
            ps.executeUpdate();
            limpiar();
            cargarTabla();
            notifyChanged();
            JOptionPane.showMessageDialog(this, "Cliente guardado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizar() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        int id = (int) model.getValueAt(row, 0);
        String celular = txtCelular.getText().trim();
        String documento = txtDocumento.getText().trim();
        if (celular.isEmpty() || documento.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Celular y documento son obligatorios.");
            return;
        }
        if (existeDocumentoOCelular(documento, celular, id)) {
            JOptionPane.showMessageDialog(this, "Otro cliente ya usa ese celular o documento.");
            return;
        }

        String sql = "UPDATE clientes SET nombre = ?, celular = ?, documento = ?, direccion = ?, nota = ? WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, txtNombre.getText().trim());
            ps.setString(2, celular);
            ps.setString(3, documento);
            ps.setString(4, txtDireccion.getText().trim());
            ps.setString(5, txtNota.getText().trim());
            ps.setInt(6, id);
            ps.executeUpdate();
            cargarTabla();
            notifyChanged();
            JOptionPane.showMessageDialog(this, "Cliente actualizado.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminar() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un cliente.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar este cliente?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        int id = (int) model.getValueAt(row, 0);
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            limpiar();
            cargarTabla();
            notifyChanged();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean existeDocumentoOCelular(String documento, String celular, Integer exceptId) {
        String sql = "SELECT id FROM clientes WHERE (documento = ? OR celular = ?)"
                + (exceptId != null ? " AND id <> ?" : "")
                + " LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, documento);
            ps.setString(2, celular);
            if (exceptId != null) {
                ps.setInt(3, exceptId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void limpiar() {
        txtNombre.setText("");
        txtCelular.setText("");
        txtDocumento.setText("");
        txtDireccion.setText("");
        txtNota.setText("");
        table.clearSelection();
    }

    private void notifyChanged() {
        if (onChanged != null) {
            onChanged.run();
        }
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
