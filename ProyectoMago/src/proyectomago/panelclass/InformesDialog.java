package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.table.DefaultTableModel;

/**
 * Informe de ventas por rango de fechas (totales diarios).
 */
public class InformesDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblResumen = new JLabel(" ");
    private final JSpinner spinDesde;
    private final JSpinner spinHasta;
    private static final DateTimeFormatter SQL_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHOW_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public InformesDialog(JFrame owner) {
        super(owner, "Informes de ventas", true);
        setSize(640, 480);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        Calendar cal = Calendar.getInstance();
        Date hoy = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -6);
        Date hace7 = cal.getTime();

        spinDesde = new JSpinner(new SpinnerDateModel(hace7, null, null, Calendar.DAY_OF_MONTH));
        spinHasta = new JSpinner(new SpinnerDateModel(hoy, null, null, Calendar.DAY_OF_MONTH));
        spinDesde.setEditor(new JSpinner.DateEditor(spinDesde, "dd/MM/yyyy"));
        spinHasta.setEditor(new JSpinner.DateEditor(spinHasta, "dd/MM/yyyy"));

        model = new DefaultTableModel(new Object[]{"Fecha", "Total del día"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(UiTheme.BODY);
        table.setRowHeight(28);
        table.getTableHeader().setFont(UiTheme.BODY_BOLD);
        table.setBackground(UiTheme.SURFACE);

        add(buildFiltros(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        buscar();
    }

    private JPanel buildFiltros() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);

        JPanel fechas = new JPanel(new GridBagLayout());
        fechas.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        fechas.add(label("Desde"), c);
        c.gridx = 1;
        styleSpinner(spinDesde);
        fechas.add(spinDesde, c);
        c.gridx = 2;
        fechas.add(label("Hasta"), c);
        c.gridx = 3;
        styleSpinner(spinHasta);
        fechas.add(spinHasta, c);

        JButton btnBuscar = primaryButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        c.gridx = 4;
        fechas.add(btnBuscar, c);

        JPanel atajos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        atajos.setOpaque(false);
        atajos.add(chip("Hoy", this::rangoHoy));
        atajos.add(chip("Últimos 7 días", this::rango7Dias));
        atajos.add(chip("Este mes", this::rangoMes));
        atajos.add(chip("Todo", this::rangoTodo));

        panel.add(fechas, BorderLayout.NORTH);
        panel.add(atajos, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        lblResumen.setFont(UiTheme.HEADING);
        lblResumen.setForeground(UiTheme.ACCENT);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnVista = secondaryButton("Vista para imprimir");
        JButton btnCerrar = secondaryButton("Cerrar");
        btnVista.addActionListener(e -> mostrarVistaImpresion());
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnVista);
        actions.add(btnCerrar);

        footer.add(lblResumen, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void rangoHoy() {
        Date hoy = new Date();
        spinDesde.setValue(hoy);
        spinHasta.setValue(hoy);
        buscar();
    }

    private void rango7Dias() {
        Calendar cal = Calendar.getInstance();
        Date hasta = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -6);
        spinDesde.setValue(cal.getTime());
        spinHasta.setValue(hasta);
        buscar();
    }

    private void rangoMes() {
        Calendar cal = Calendar.getInstance();
        Date hasta = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        spinDesde.setValue(cal.getTime());
        spinHasta.setValue(hasta);
        buscar();
    }

    private void rangoTodo() {
        // Rango amplio: desde 2000 hasta hoy
        Calendar cal = Calendar.getInstance();
        Date hasta = cal.getTime();
        cal.set(2000, Calendar.JANUARY, 1);
        spinDesde.setValue(cal.getTime());
        spinHasta.setValue(hasta);
        buscar();
    }

    private void buscar() {
        model.setRowCount(0);
        Date desdeDate = (Date) spinDesde.getValue();
        Date hastaDate = (Date) spinHasta.getValue();
        if (desdeDate.after(hastaDate)) {
            JOptionPane.showMessageDialog(this, "La fecha «Desde» no puede ser posterior a «Hasta».");
            return;
        }

        LocalDate desde = toLocalDate(desdeDate);
        LocalDate hasta = toLocalDate(hastaDate);

        String sql = "SELECT DATE(fecha) AS dia, total, fecha "
                + "FROM informes "
                + "WHERE DATE(fecha) >= ? AND DATE(fecha) <= ? "
                + "ORDER BY DATE(fecha) DESC";

        double suma = 0;
        int dias = 0;

        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, desde.format(SQL_DAY));
            ps.setString(2, hasta.format(SQL_DAY));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String diaSql = rs.getString("dia");
                    if (diaSql == null || diaSql.isBlank()) {
                        // Fallback si DATE() no devolvió valor legible
                        String fechaRaw = rs.getString("fecha");
                        diaSql = fechaRaw != null && fechaRaw.length() >= 10
                                ? fechaRaw.substring(0, 10) : fechaRaw;
                    }
                    LocalDate dia = LocalDate.parse(diaSql.substring(0, Math.min(10, diaSql.length())));
                    double total = rs.getDouble("total");
                    model.addRow(new Object[]{
                        dia.format(SHOW_DAY),
                        "$" + String.format("%.0f", total)
                    });
                    suma += total;
                    dias++;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al consultar informes: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            lblResumen.setText(" ");
            return;
        }

        if (dias == 0) {
            lblResumen.setText("Sin ventas en ese rango");
        } else {
            lblResumen.setText(dias + (dias == 1 ? " día" : " días")
                    + "  ·  Total período: $" + String.format("%.0f", suma));
        }
    }

    private void mostrarVistaImpresion() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para mostrar.");
            return;
        }

        String nombre = ComercioConfig.get().getNombreComercio();
        SimpleDateFormat show = new SimpleDateFormat("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append("\n");
        sb.append("INFORME DE VENTAS\n");
        sb.append("Desde: ").append(show.format((Date) spinDesde.getValue())).append("\n");
        sb.append("Hasta: ").append(show.format((Date) spinHasta.getValue())).append("\n");
        sb.append("--------------------------------\n");
        for (int i = 0; i < model.getRowCount(); i++) {
            sb.append(model.getValueAt(i, 0)).append("   ")
                    .append(model.getValueAt(i, 1)).append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append(lblResumen.getText()).append("\n");

        JTextArea area = new JTextArea(sb.toString(), 16, 34);
        area.setFont(UiTheme.MONO);
        area.setEditable(false);
        int opcion = JOptionPane.showOptionDialog(this, new JScrollPane(area), "Vista del informe",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Imprimir", "Cerrar"}, "Cerrar");
        if (opcion == 0) {
            TicketPrinter.PrintResult result = TicketPrinter.printCopies(sb.toString() + "\n\n", 1);
            if (!result.printed) {
                JOptionPane.showMessageDialog(this, result.message);
            } else {
                JOptionPane.showMessageDialog(this, result.message);
            }
        }
    }

    private static LocalDate toLocalDate(Date date) {
        return new java.sql.Date(date.getTime()).toLocalDate();
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.BODY_BOLD);
        l.setForeground(UiTheme.INK);
        return l;
    }

    private static void styleSpinner(JSpinner spinner) {
        spinner.setFont(UiTheme.BODY);
        spinner.setPreferredSize(new Dimension(120, 32));
    }

    private JButton chip(String text, Runnable action) {
        JButton b = secondaryButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
