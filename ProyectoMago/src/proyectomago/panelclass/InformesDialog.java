package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Informes visuales: filtros por tipo/ganancia, KPIs y donuts.
 */
public class InformesDialog extends JDialog {

    private final DefaultTableModel model;
    private final JTable table;
    private final JSpinner spinDesde;
    private final JSpinner spinHasta;
    private final JComboBox<String> comboTipo = new JComboBox<>();
    private final JTextField txtGananciaMin = new JTextField(8);
    private final DonutChartPanel chartTipo = new DonutChartPanel();
    private final DonutChartPanel chartResultado = new DonutChartPanel();

    private final JLabel kpiVentas = new JLabel("$0");
    private final JLabel kpiCosto = new JLabel("$0");
    private final JLabel kpiGanancia = new JLabel("$0");
    private final JLabel kpiMargen = new JLabel("0%");
    private final JLabel lblHint = new JLabel(" ");

    private static final DateTimeFormatter SQL_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHOW_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TIPOS_BASE = {
        "Todos", "Pan", "Facturas", "Tortas", "Masas", "Sandwiches", "Bebidas", "Fiambres", "Otro"
    };

    private double lastVentas;
    private double lastCosto;
    private double lastGanancia;

    public InformesDialog(JFrame owner) {
        super(owner, "Informes panadería · ventas y ganancia", true);
        setSize(1100, 860);
        setMinimumSize(new Dimension(960, 740));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        Calendar cal = Calendar.getInstance();
        Date hoy = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -29);
        Date hace30 = cal.getTime();

        spinDesde = new JSpinner(new SpinnerDateModel(hace30, null, null, Calendar.DAY_OF_MONTH));
        spinHasta = new JSpinner(new SpinnerDateModel(hoy, null, null, Calendar.DAY_OF_MONTH));
        spinDesde.setEditor(new JSpinner.DateEditor(spinDesde, "dd/MM/yyyy"));
        spinHasta.setEditor(new JSpinner.DateEditor(spinHasta, "dd/MM/yyyy"));

        model = new DefaultTableModel(
                new Object[]{"Fecha", "Ventas", "Costo", "Ganancia", "Margen"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        styleTable(table);

        chartTipo.setTitle("Ventas por tipo");
        chartResultado.setTitle("Resultado del período");
        chartTipo.setPreferredSize(new Dimension(10, 200));
        chartResultado.setPreferredSize(new Dimension(10, 200));

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(buildTopBar(), BorderLayout.NORTH);
        north.add(buildKpis(), BorderLayout.SOUTH);

        JPanel charts = new JPanel(new GridLayout(1, 2, 12, 0));
        charts.setOpaque(false);
        charts.add(chartTipo);
        charts.add(chartResultado);
        charts.setPreferredSize(new Dimension(10, 200));
        charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        JPanel detail = new UiTheme.RoundedPanel(16, UiTheme.SURFACE, UiTheme.BORDER);
        detail.setLayout(new BorderLayout(0, 8));
        detail.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel detailTitle = new JLabel("Detalle diario");
        detailTitle.setFont(UiTheme.HEADING);
        detailTitle.setForeground(UiTheme.INK);
        JScrollPane scroll = new JScrollPane(table);
        UiTheme.styleScroll(scroll);
        detail.add(detailTitle, BorderLayout.NORTH);
        detail.add(scroll, BorderLayout.CENTER);

        // Tabla en CENTER para que siempre crezca; gráficos arriba con alto fijo.
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(charts, BorderLayout.NORTH);
        center.add(detail, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cargarTiposCombo();
        buscar();
    }

    private JPanel buildTopBar() {
        JPanel shell = new UiTheme.RoundedPanel(16, UiTheme.SURFACE, UiTheme.BORDER);
        shell.setLayout(new BorderLayout(0, 8));
        shell.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Informes");
        title.setFont(UiTheme.HEADING);
        title.setForeground(UiTheme.INK);
        JLabel sub = new JLabel("Ventas, costos y margen del período");
        sub.setFont(UiTheme.SMALL);
        sub.setForeground(UiTheme.MUTED);
        JPanel texts = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        texts.setOpaque(false);
        texts.add(title);
        texts.add(sub);
        titleRow.add(texts, BorderLayout.WEST);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        row.add(label("Desde"), c);
        c.gridx = 1;
        styleSpinner(spinDesde);
        row.add(spinDesde, c);

        c.gridx = 2;
        row.add(label("Hasta"), c);
        c.gridx = 3;
        styleSpinner(spinHasta);
        row.add(spinHasta, c);

        c.gridx = 4;
        row.add(label("Tipo"), c);
        c.gridx = 5;
        UiTheme.styleCombo(comboTipo);
        comboTipo.setPreferredSize(new Dimension(120, 36));
        row.add(comboTipo, c);

        c.gridx = 6;
        row.add(label("Gan. mín."), c);
        c.gridx = 7;
        styleField(txtGananciaMin);
        txtGananciaMin.setToolTipText("Solo días con ganancia ≥ este monto (vacío = sin filtro)");
        row.add(txtGananciaMin, c);

        JButton btnBuscar = primaryButton("Aplicar");
        btnBuscar.addActionListener(e -> buscar());
        c.gridx = 8;
        row.add(btnBuscar, c);

        c.gridx = 9;
        JPanel atajos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        atajos.setOpaque(false);
        atajos.add(chip("Hoy", this::rangoHoy));
        atajos.add(chip("7 días", this::rango7Dias));
        atajos.add(chip("30 días", this::rango30Dias));
        atajos.add(chip("Mes", this::rangoMes));
        atajos.add(chip("Todo", this::rangoTodo));
        row.add(atajos, c);

        shell.add(titleRow, BorderLayout.NORTH);
        shell.add(row, BorderLayout.CENTER);
        return shell;
    }

    private JPanel buildKpis() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.add(compactKpi("Ventas", kpiVentas, UiTheme.INK));
        row.add(compactKpi("Costo", kpiCosto, UiTheme.MUTED));
        row.add(compactKpi("Ganancia", kpiGanancia, UiTheme.SUCCESS));
        row.add(compactKpi("Margen", kpiMargen, UiTheme.ACCENT));
        return row;
    }

    private static JPanel compactKpi(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new UiTheme.RoundedPanel(12, UiTheme.SURFACE, UiTheme.BORDER);
        card.setLayout(new BorderLayout(0, 2));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(UiTheme.SMALL_BOLD);
        t.setForeground(UiTheme.MUTED);
        valueLabel.setFont(UiTheme.KPI);
        valueLabel.setForeground(valueColor);
        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        lblHint.setFont(UiTheme.SMALL);
        lblHint.setForeground(UiTheme.MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnExcel = secondaryButton("Exportar Excel");
        JButton btnVista = secondaryButton("Vista para imprimir");
        JButton btnCerrar = primaryButton("Cerrar");
        btnExcel.addActionListener(e -> exportarExcel());
        btnVista.addActionListener(e -> mostrarVistaImpresion());
        btnCerrar.addActionListener(e -> dispose());
        actions.add(btnExcel);
        actions.add(btnVista);
        actions.add(btnCerrar);

        footer.add(lblHint, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private void cargarTiposCombo() {
        comboTipo.removeAllItems();
        for (String t : TIPOS_BASE) {
            comboTipo.addItem(t);
        }
        String sql = "SELECT DISTINCT tipo FROM menuPrecios WHERE tipo IS NOT NULL AND TRIM(tipo) <> '' "
                + "UNION SELECT DISTINCT tipo FROM venta_detalle WHERE tipo IS NOT NULL AND TRIM(tipo) <> '' "
                + "ORDER BY 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String tipo = rs.getString(1);
                if (tipo == null) {
                    continue;
                }
                boolean exists = false;
                for (int i = 0; i < comboTipo.getItemCount(); i++) {
                    if (tipo.equalsIgnoreCase(comboTipo.getItemAt(i))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    comboTipo.addItem(tipo);
                }
            }
        } catch (SQLException ignored) {
            // Keep base list.
        }
        comboTipo.setSelectedIndex(0);
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

    private void rango30Dias() {
        Calendar cal = Calendar.getInstance();
        Date hasta = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -29);
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
        String tipoFiltro = String.valueOf(comboTipo.getSelectedItem());
        boolean filtrarTipo = tipoFiltro != null && !"Todos".equalsIgnoreCase(tipoFiltro);
        Double gananciaMin = parseMonto(txtGananciaMin.getText().trim());

        Map<String, double[]> porDia = new LinkedHashMap<>();
        Map<String, Double> porTipo = new LinkedHashMap<>();
        boolean hayDetalle = false;

        String sqlDetalle = "SELECT DATE(fecha) AS dia, tipo, "
                + "SUM(subtotal) AS ventas, SUM(costo_total) AS costo "
                + "FROM venta_detalle "
                + "WHERE DATE(fecha) >= ? AND DATE(fecha) <= ? "
                + (filtrarTipo ? "AND tipo = ? " : "")
                + "GROUP BY DATE(fecha), tipo "
                + "ORDER BY DATE(fecha) DESC";

        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlDetalle)) {
            ps.setString(1, desde.format(SQL_DAY));
            ps.setString(2, hasta.format(SQL_DAY));
            if (filtrarTipo) {
                ps.setString(3, tipoFiltro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hayDetalle = true;
                    String dia = normalizeDay(rs.getString("dia"));
                    String tipo = rs.getString("tipo");
                    if (tipo == null || tipo.isBlank()) {
                        tipo = "Otro";
                    }
                    double ventas = rs.getDouble("ventas");
                    double costo = rs.getDouble("costo");
                    double[] acc = porDia.computeIfAbsent(dia, k -> new double[2]);
                    acc[0] += ventas;
                    acc[1] += costo;
                    porTipo.merge(tipo, ventas, Double::sum);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al consultar detalle: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Fallback: ventas diarias históricas sin costo/tipo.
        if (!hayDetalle && !filtrarTipo) {
            String sqlInf = "SELECT DATE(fecha) AS dia, total, fecha FROM informes "
                    + "WHERE DATE(fecha) >= ? AND DATE(fecha) <= ? ORDER BY DATE(fecha) DESC";
            try (Connection connection = new conection().getConnection();
                 PreparedStatement ps = connection.prepareStatement(sqlInf)) {
                ps.setString(1, desde.format(SQL_DAY));
                ps.setString(2, hasta.format(SQL_DAY));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String dia = normalizeDay(rs.getString("dia"));
                        if (dia == null) {
                            String raw = rs.getString("fecha");
                            dia = raw != null && raw.length() >= 10 ? raw.substring(0, 10) : raw;
                        }
                        double total = rs.getDouble("total");
                        double[] acc = porDia.computeIfAbsent(dia, k -> new double[2]);
                        acc[0] += total;
                    }
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al consultar informes: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            lblHint.setText("Todavía no hay ventas con detalle. Procesá pedidos nuevos para ver tipo y ganancia.");
        } else if (!hayDetalle && filtrarTipo) {
            lblHint.setText("No hay ventas del tipo «" + tipoFiltro + "» en ese rango.");
        } else {
            lblHint.setText("Detalle por producto · filtro tipo/ganancia aplicados al período.");
        }

        double sumVentas = 0;
        double sumCosto = 0;
        int dias = 0;

        List<String> diasOrden = new ArrayList<>(porDia.keySet());
        // Ya vienen DESC por query; LinkedHashMap conserva orden de inserción.
        for (String diaSql : diasOrden) {
            double[] v = porDia.get(diaSql);
            double ventas = v[0];
            double costo = v[1];
            double ganancia = ventas - costo;
            if (gananciaMin != null && ganancia < gananciaMin) {
                continue;
            }
            double margen = ventas > 0 ? (ganancia * 100.0 / ventas) : 0;
            LocalDate dia = LocalDate.parse(diaSql.substring(0, Math.min(10, diaSql.length())));
            model.addRow(new Object[]{
                dia.format(SHOW_DAY),
                "$" + money(ventas),
                "$" + money(costo),
                "$" + money(ganancia),
                Math.round(margen) + "%"
            });
            sumVentas += ventas;
            sumCosto += costo;
            dias++;
        }

        lastVentas = sumVentas;
        lastCosto = sumCosto;
        lastGanancia = sumVentas - sumCosto;
        double margen = sumVentas > 0 ? (lastGanancia * 100.0 / sumVentas) : 0;

        kpiVentas.setText("$" + money(sumVentas));
        kpiCosto.setText("$" + money(sumCosto));
        kpiGanancia.setText("$" + money(lastGanancia));
        kpiGanancia.setForeground(lastGanancia >= 0 ? UiTheme.SUCCESS : UiTheme.ACCENT);
        kpiMargen.setText(Math.round(margen) + "%");

        // Donut por tipo
        List<DonutChartPanel.Slice> slicesTipo = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> e : porTipo.entrySet()) {
            slicesTipo.add(new DonutChartPanel.Slice(e.getKey(), e.getValue(), DonutChartPanel.colorAt(i++)));
        }
        if (slicesTipo.isEmpty() && sumVentas > 0 && !filtrarTipo) {
            slicesTipo.add(new DonutChartPanel.Slice("Ventas", sumVentas, DonutChartPanel.colorAt(0)));
        }
        chartTipo.setCenter("Total", "$" + money(sumVentas));
        chartTipo.setSlices(slicesTipo);

        // Donut resultado: ganancia vs costo
        List<DonutChartPanel.Slice> slicesRes = new ArrayList<>();
        if (sumCosto > 0 || lastGanancia > 0) {
            if (sumCosto > 0) {
                slicesRes.add(new DonutChartPanel.Slice("Costo", sumCosto, new Color(0x8B5E3C)));
            }
            if (lastGanancia > 0) {
                slicesRes.add(new DonutChartPanel.Slice("Ganancia", lastGanancia, UiTheme.SUCCESS));
            } else if (lastGanancia < 0) {
                slicesRes.add(new DonutChartPanel.Slice("Pérdida", Math.abs(lastGanancia), UiTheme.ACCENT));
            }
        }
        chartResultado.setCenter("Margen", Math.round(margen) + "%");
        chartResultado.setSlices(slicesRes);

        if (dias == 0 && (gananciaMin != null)) {
            lblHint.setText("Ningún día supera la ganancia mínima de $" + money(gananciaMin) + ".");
        } else if (dias == 0) {
            lblHint.setText("Sin movimientos en ese rango con los filtros actuales.");
        }
    }

    private void exportarExcel() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para exportar. Aplicá un rango con ventas.");
            return;
        }

        SimpleDateFormat show = new SimpleDateFormat("dd-MM-yyyy");
        String desde = show.format((Date) spinDesde.getValue());
        String hasta = show.format((Date) spinHasta.getValue());
        String sugerido = "informe_" + desde + "_" + hasta + ".xls";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar informe a Excel");
        chooser.setSelectedFile(new File(sugerido));
        chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xls)", "xls"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xls")) {
            file = new File(file.getAbsolutePath() + ".xls");
        }

        try {
            String nombre = ComercioConfig.get().getNombreComercio();
            String tipo = String.valueOf(comboTipo.getSelectedItem());
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"__STYLE__Title", nombre});
            rows.add(new String[]{"Informe de ventas y ganancia"});
            rows.add(new String[]{"Desde", desde.replace('-', '/')});
            rows.add(new String[]{"Hasta", hasta.replace('-', '/')});
            rows.add(new String[]{"Tipo", tipo});
            rows.add(new String[]{""});
            rows.add(new String[]{"__STYLE__Header", "Resumen", "Monto"});
            rows.add(new String[]{"Ventas", money(lastVentas)});
            rows.add(new String[]{"Costo", money(lastCosto)});
            rows.add(new String[]{"Ganancia", money(lastGanancia)});
            double margen = lastVentas > 0 ? (lastGanancia * 100.0 / lastVentas) : 0;
            rows.add(new String[]{"Margen %", String.valueOf(Math.round(margen))});
            rows.add(new String[]{""});
            rows.add(new String[]{"__STYLE__Header", "Fecha", "Ventas", "Costo", "Ganancia", "Margen"});

            for (int i = 0; i < model.getRowCount(); i++) {
                rows.add(new String[]{
                    String.valueOf(model.getValueAt(i, 0)),
                    stripMoney(model.getValueAt(i, 1)),
                    stripMoney(model.getValueAt(i, 2)),
                    stripMoney(model.getValueAt(i, 3)),
                    stripPercent(model.getValueAt(i, 4))
                });
            }

            ExcelExport.writeSheet(file, "Informe", rows);

            int open = JOptionPane.showConfirmDialog(
                    this,
                    "Informe exportado:\n" + file.getAbsolutePath() + "\n\n¿Abrirlo ahora?",
                    "Excel",
                    JOptionPane.YES_NO_OPTION
            );
            if (open == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo exportar el Excel.\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String stripMoney(Object value) {
        if (value == null) {
            return "0";
        }
        return String.valueOf(value).replace("$", "").replace(".", "").replace(",", ".").trim();
    }

    private static String stripPercent(Object value) {
        if (value == null) {
            return "0";
        }
        return String.valueOf(value).replace("%", "").trim();
    }

    private void mostrarVistaImpresion() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay datos para mostrar.");
            return;
        }

        String nombre = ComercioConfig.get().getNombreComercio();
        SimpleDateFormat show = new SimpleDateFormat("dd/MM/yyyy");
        String tipo = String.valueOf(comboTipo.getSelectedItem());
        StringBuilder sb = new StringBuilder();
        sb.append(nombre).append("\n");
        sb.append("INFORME DE VENTAS Y GANANCIA\n");
        sb.append("Desde: ").append(show.format((Date) spinDesde.getValue())).append("\n");
        sb.append("Hasta: ").append(show.format((Date) spinHasta.getValue())).append("\n");
        sb.append("Tipo: ").append(tipo).append("\n");
        sb.append("--------------------------------\n");
        sb.append(String.format("Ventas:    $%s\n", money(lastVentas)));
        sb.append(String.format("Costo:     $%s\n", money(lastCosto)));
        sb.append(String.format("Ganancia:  $%s\n", money(lastGanancia)));
        sb.append("--------------------------------\n");
        for (int i = 0; i < model.getRowCount(); i++) {
            sb.append(model.getValueAt(i, 0)).append("  ")
                    .append(model.getValueAt(i, 1)).append("  gan ")
                    .append(model.getValueAt(i, 3)).append("  ")
                    .append(model.getValueAt(i, 4)).append("\n");
        }

        JTextArea area = new JTextArea(sb.toString(), 18, 40);
        area.setFont(UiTheme.MONO);
        area.setEditable(false);
        int opcion = JOptionPane.showOptionDialog(this, new JScrollPane(area), "Vista del informe",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Imprimir", "Cerrar"}, "Cerrar");
        if (opcion == 0) {
            TicketPrinter.PrintResult result = TicketPrinter.printCopies(sb.toString() + "\n\n", 1);
            JOptionPane.showMessageDialog(this, result.message);
        }
    }

    private static String normalizeDay(String diaSql) {
        if (diaSql == null || diaSql.isBlank()) {
            return null;
        }
        return diaSql.substring(0, Math.min(10, diaSql.length()));
    }

    private static String money(double n) {
        return String.format("%.0f", n);
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

    private static LocalDate toLocalDate(Date date) {
        return new java.sql.Date(date.getTime()).toLocalDate();
    }

    private static void styleTable(JTable table) {
        table.setFont(UiTheme.BODY);
        table.setRowHeight(30);
        table.setBackground(UiTheme.SURFACE);
        table.setForeground(UiTheme.INK);
        table.setGridColor(UiTheme.BORDER);
        table.setSelectionBackground(UiTheme.ACCENT);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setUI(new BasicTableHeaderUI());
        header.setFont(UiTheme.BODY_BOLD);
        header.setOpaque(true);
        header.setBackground(UiTheme.SURFACE_ALT);
        header.setForeground(UiTheme.INK);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(10, 34));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                l.setBackground(UiTheme.SURFACE_ALT);
                l.setForeground(UiTheme.INK);
                l.setFont(UiTheme.BODY_BOLD);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)
                ));
                return l;
            }
        });

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(JLabel.RIGHT);
        right.setBackground(UiTheme.SURFACE);
        right.setForeground(UiTheme.INK);
        for (int col = 1; col <= 4; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(right);
        }
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.SMALL_BOLD);
        l.setForeground(UiTheme.MUTED);
        return l;
    }

    private static void styleSpinner(JSpinner spinner) {
        UiTheme.styleSpinner(spinner);
        spinner.setPreferredSize(new Dimension(118, 36));
    }

    private static void styleField(JTextField f) {
        UiTheme.styleField(f);
        f.setPreferredSize(new Dimension(90, 36));
    }

    private JButton chip(String text, Runnable action) {
        JButton b = UiTheme.navButton(text);
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
