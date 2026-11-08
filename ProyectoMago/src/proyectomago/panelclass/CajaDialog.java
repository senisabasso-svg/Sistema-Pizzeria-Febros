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
 * Apertura / cierre de caja y movimientos informales (entró / salió).
 */
public class CajaDialog extends JDialog {

    private final DefaultTableModel modelMov;
    private final JTable tableMov;
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblResumen = new JLabel(" ");
    private final JTextField txtMonto = new JTextField(10);
    private final JTextField txtConcepto = new JTextField(18);
    private final JComboBox<String> comboTipo = new JComboBox<>(new String[]{"ENTRADA", "SALIDA"});

    private Integer cajaIdAbierta;
    private double montoApertura;
    private String fechaApertura;

    public CajaDialog(JFrame owner) {
        super(owner, "Caja del día", true);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        modelMov = new DefaultTableModel(new Object[]{"ID", "Hora", "Tipo", "Monto", "Concepto"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableMov = new JTable(modelMov);
        tableMov.setFont(UiTheme.BODY);
        tableMov.setRowHeight(26);
        tableMov.getTableHeader().setFont(UiTheme.BODY_BOLD);
        tableMov.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableMov.getColumnModel().getColumn(0).setMinWidth(0);
        tableMov.getColumnModel().getColumn(0).setMaxWidth(0);
        tableMov.getColumnModel().getColumn(0).setPreferredWidth(0);

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(tableMov), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        refrescar();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);

        lblEstado.setFont(UiTheme.HEADING);
        lblEstado.setForeground(UiTheme.INK);
        lblResumen.setFont(UiTheme.BODY);
        lblResumen.setForeground(UiTheme.MUTED);

        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        titles.add(lblEstado, BorderLayout.NORTH);
        titles.add(lblResumen, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnAbrir = primaryButton("Abrir caja");
        JButton btnCerrar = secondaryButton("Cerrar caja");
        btnAbrir.addActionListener(e -> abrirCaja());
        btnCerrar.addActionListener(e -> cerrarCaja());
        actions.add(btnAbrir);
        actions.add(btnCerrar);

        header.add(titles, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(UiTheme.cardBorder());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(label("Tipo"), c);
        c.gridx = 1;
        comboTipo.setFont(UiTheme.BODY);
        comboTipo.setPreferredSize(new Dimension(120, 32));
        form.add(comboTipo, c);

        c.gridx = 2;
        form.add(label("Monto"), c);
        c.gridx = 3;
        styleField(txtMonto);
        form.add(txtMonto, c);

        c.gridx = 0; c.gridy = 1;
        form.add(label("Concepto"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        styleField(txtConcepto);
        form.add(txtConcepto, c);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton btnMov = primaryButton("Registrar movimiento");
        JButton btnCerrar = secondaryButton("Cerrar ventana");
        btnMov.addActionListener(e -> registrarMovimiento());
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnMov);
        actions.add(btnCerrar);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 4; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        form.add(actions, c);

        footer.add(form, BorderLayout.CENTER);
        return footer;
    }

    private void refrescar() {
        cajaIdAbierta = null;
        montoApertura = 0;
        fechaApertura = null;
        modelMov.setRowCount(0);

        String sqlCaja = "SELECT id, fecha_apertura, monto_apertura FROM cajas "
                + "WHERE estado = 'ABIERTA' ORDER BY id DESC LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlCaja);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                cajaIdAbierta = rs.getInt("id");
                montoApertura = rs.getDouble("monto_apertura");
                fechaApertura = rs.getString("fecha_apertura");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer caja: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (cajaIdAbierta == null) {
            lblEstado.setText("Caja cerrada");
            lblEstado.setForeground(UiTheme.MUTED);
            lblResumen.setText("Abrí la caja para empezar el día y registrar movimientos.");
            return;
        }

        cargarMovimientos();
        actualizarResumen();
    }

    private void cargarMovimientos() {
        modelMov.setRowCount(0);
        String sql = "SELECT id, fecha, tipo, monto, concepto FROM movimientos_caja "
                + "WHERE caja_id = ? ORDER BY id DESC";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cajaIdAbierta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fecha = rs.getString("fecha");
                    String hora = fecha != null && fecha.length() >= 19 ? fecha.substring(11, 19) : fecha;
                    modelMov.addRow(new Object[]{
                        rs.getInt("id"),
                        hora,
                        rs.getString("tipo"),
                        "$" + String.format("%.0f", rs.getDouble("monto")),
                        rs.getString("concepto")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar movimientos: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarResumen() {
        double entradas = sumarMovimientos("ENTRADA");
        double salidas = sumarMovimientos("SALIDA");
        double ventas = ventasDelDia();
        double esperado = montoApertura + ventas + entradas - salidas;

        lblEstado.setText("Caja abierta · #" + cajaIdAbierta);
        lblEstado.setForeground(UiTheme.SUCCESS);
        lblResumen.setText(String.format(
                "Apertura $%.0f  ·  Ventas $%.0f  ·  Entradas $%.0f  ·  Salidas $%.0f  ·  Esperado en caja $%.0f",
                montoApertura, ventas, entradas, salidas, esperado));
    }

    private double sumarMovimientos(String tipo) {
        if (cajaIdAbierta == null) {
            return 0;
        }
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE caja_id = ? AND tipo = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cajaIdAbierta);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private double ventasDelDia() {
        String dia = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        if (fechaApertura != null && fechaApertura.length() >= 10) {
            dia = fechaApertura.substring(0, 10);
        }
        String sql = "SELECT COALESCE(SUM(total), 0) FROM informes WHERE DATE(fecha) = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dia);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private void abrirCaja() {
        if (cajaIdAbierta != null) {
            JOptionPane.showMessageDialog(this, "Ya hay una caja abierta. Cerrala antes de abrir otra.");
            return;
        }

        String montoTxt = JOptionPane.showInputDialog(this,
                "Monto inicial en caja (efectivo al abrir):", "0");
        if (montoTxt == null) {
            return;
        }
        Double monto = parseMonto(montoTxt.trim());
        if (monto == null) {
            JOptionPane.showMessageDialog(this, "Ingresá un monto válido.");
            return;
        }

        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sql = "INSERT INTO cajas (fecha_apertura, monto_apertura, estado) VALUES (?, ?, 'ABIERTA')";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ahora);
            ps.setDouble(2, monto);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Caja abierta con $" + String.format("%.0f", monto));
            refrescar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir caja: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cerrarCaja() {
        if (cajaIdAbierta == null) {
            JOptionPane.showMessageDialog(this, "No hay caja abierta para cerrar.");
            return;
        }

        double entradas = sumarMovimientos("ENTRADA");
        double salidas = sumarMovimientos("SALIDA");
        double ventas = ventasDelDia();
        double esperado = montoApertura + ventas + entradas - salidas;

        String contadoTxt = JOptionPane.showInputDialog(this,
                "Esperado en caja: $" + String.format("%.0f", esperado)
                        + "\n\n¿Cuánto contaste en efectivo?",
                String.format("%.0f", esperado));
        if (contadoTxt == null) {
            return;
        }
        Double contado = parseMonto(contadoTxt.trim());
        if (contado == null) {
            JOptionPane.showMessageDialog(this, "Ingresá un monto contado válido.");
            return;
        }

        double diferencia = contado - esperado;
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sql = "UPDATE cajas SET fecha_cierre = ?, monto_esperado = ?, monto_contado = ?, "
                + "diferencia = ?, estado = 'CERRADA' WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ahora);
            ps.setDouble(2, esperado);
            ps.setDouble(3, contado);
            ps.setDouble(4, diferencia);
            ps.setInt(5, cajaIdAbierta);
            ps.executeUpdate();

            String msg = "Caja cerrada.\n"
                    + "Esperado: $" + String.format("%.0f", esperado) + "\n"
                    + "Contado: $" + String.format("%.0f", contado) + "\n"
                    + "Diferencia: $" + String.format("%.0f", diferencia);
            JOptionPane.showMessageDialog(this, msg, "Cierre de caja", JOptionPane.INFORMATION_MESSAGE);
            refrescar();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cerrar caja: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrarMovimiento() {
        if (cajaIdAbierta == null) {
            JOptionPane.showMessageDialog(this, "Abrí la caja antes de registrar movimientos.");
            return;
        }

        Double monto = parseMonto(txtMonto.getText().trim());
        String concepto = txtConcepto.getText().trim();
        if (monto == null || monto <= 0) {
            JOptionPane.showMessageDialog(this, "Ingresá un monto mayor a 0.");
            return;
        }
        if (concepto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá un concepto (ej: pago delivery, cambio, gasto).");
            return;
        }

        String tipo = String.valueOf(comboTipo.getSelectedItem());
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sql = "INSERT INTO movimientos_caja (caja_id, fecha, tipo, monto, concepto) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cajaIdAbierta);
            ps.setString(2, ahora);
            ps.setString(3, tipo);
            ps.setDouble(4, monto);
            ps.setString(5, concepto);
            ps.executeUpdate();
            txtMonto.setText("");
            txtConcepto.setText("");
            cargarMovimientos();
            actualizarResumen();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Double parseMonto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(texto.replace(",", ".").replace("$", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
