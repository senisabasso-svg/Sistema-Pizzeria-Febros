package proyectomago.panelclass;

import connectDatabase.conection;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla principal POS para almacén / local 24 horas: venta rápida, caja, stock y proveedores.
 */
public class PrincipalPanel extends JPanel {

    private final ArrayList<Pedido> listaPedidos = new ArrayList<>();
    private final DefaultListModel<Pedido> listModel = new DefaultListModel<>();

    private final JTextField txtCodigo = new JTextField();
    private final JComboBox<String> comboProducto = new JComboBox<>();
    private final JTextField txtNota = new JTextField();
    private final JSpinner spinCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final JTextField txtPrecioUnitario = new JTextField();
    private final JLabel lblTotal = new JLabel("$0");
    private final JList<Pedido> listaUI = new JList<>(listModel);
    private final JLabel lblLogo = new JLabel();
    private final JLabel lblTituloComercio = new JLabel();

    private String precioSeleccionado = "0";
    private String nombreSeleccionado = "";
    private final ActionListener productoListener = e -> cargarPrecioProducto();

    public PrincipalPanel() {
        setBackground(UiTheme.BG);
        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        aplicarBranding();
        comboProducto.addActionListener(productoListener);
        txtCodigo.addActionListener(e -> buscarPorCodigo());
        cargarMenu();
        SwingUtilities.invokeLater(() -> txtCodigo.requestFocusInWindow());
    }

    private JPanel buildHeader() {
        JPanel header = UiTheme.headerBar();

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brand.setOpaque(false);

        lblLogo.setPreferredSize(new Dimension(52, 52));
        lblTituloComercio.setFont(UiTheme.TITLE);
        lblTituloComercio.setForeground(UiTheme.INK);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        JLabel subtitle = new JLabel("Local 24 horas · venta · caja · stock");
        subtitle.setFont(UiTheme.SMALL);
        subtitle.setForeground(UiTheme.MUTED);
        titles.add(lblTituloComercio, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        brand.add(lblLogo);
        brand.add(titles);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton btnCaja = navButton("Caja");
        JButton btnStock = navButton("Stock");
        JButton btnProveedores = navButton("Proveedores");
        JButton btnCompras = navButton("Compras");
        JButton btnMerma = navButton("Merma");
        JButton btnProductos = navButton("Productos");
        JButton btnInforme = navButton("Informes");
        JButton btnConfig = navButton("Configuración");
        btnCaja.addActionListener(e -> abrirCaja());
        btnStock.addActionListener(e -> abrirStock());
        btnProveedores.addActionListener(e -> abrirProveedores());
        btnCompras.addActionListener(e -> abrirCompras());
        btnMerma.addActionListener(e -> abrirMerma());
        btnProductos.addActionListener(e -> abrirProductos());
        btnInforme.addActionListener(e -> abrirInformes());
        btnConfig.addActionListener(e -> abrirConfiguracion());
        actions.add(btnCaja);
        actions.add(btnStock);
        actions.add(btnProveedores);
        actions.add(btnCompras);
        actions.add(btnMerma);
        actions.add(btnProductos);
        actions.add(btnInforme);
        actions.add(btnConfig);

        header.add(brand, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private void aplicarBranding() {
        ComercioConfig cfg = ComercioConfig.get();
        lblTituloComercio.setText(cfg.getNombreComercio());

        ImageIcon icon = loadLogo();
        if (icon != null) {
            lblLogo.setIcon(icon);
            lblLogo.setText("");
        } else {
            lblLogo.setIcon(null);
            lblLogo.setText("●");
            lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 28));
            lblLogo.setForeground(UiTheme.ACCENT);
        }

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) {
            ((JFrame) w).setTitle(cfg.getNombreComercio() + " · Gestión almacenes");
        }
    }

    private void abrirConfiguracion() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        ConfiguracionDialog dialog = new ConfiguracionDialog(frame, this::aplicarBranding);
        dialog.setVisible(true);
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(buildLeftColumn(), BorderLayout.WEST);
        body.add(buildOrderColumn(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildLeftColumn() {
        JPanel left = new JPanel(new BorderLayout(0, 14));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(380, 0));

        left.add(buildAddProductCard(), BorderLayout.NORTH);
        return left;
    }

    private JPanel buildAddProductCard() {
        JPanel shell = UiTheme.sectionCard("Agregar al pedido");
        JPanel card = UiTheme.cardBody(shell);
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 2, 5, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        card.add(fieldLabel("CÓDIGO DE BARRAS"), c);
        c.gridy = 1;
        styleField(txtCodigo);
        card.add(txtCodigo, c);

        c.gridy = 2;
        card.add(fieldLabel("PRODUCTO"), c);
        c.gridy = 3;
        styleCombo(comboProducto);
        card.add(comboProducto, c);

        c.gridy = 4; c.gridwidth = 2; c.weightx = 1;
        card.add(fieldLabel("NOTA"), c);
        c.gridy = 5;
        styleField(txtNota);
        card.add(txtNota, c);

        c.gridx = 0; c.gridy = 6; c.gridwidth = 2; c.weightx = 1;
        card.add(fieldLabel("CANT."), c);
        c.gridy = 7;
        UiTheme.styleSpinner(spinCantidad);
        card.add(spinCantidad, c);

        c.gridx = 0; c.gridy = 8; c.gridwidth = 2; c.weightx = 1;
        card.add(fieldLabel("PRECIO UNITARIO"), c);
        c.gridy = 9;
        styleField(txtPrecioUnitario);
        card.add(txtPrecioUnitario, c);

        c.gridy = 10;
        c.insets = new Insets(12, 2, 2, 2);
        JButton btnAdd = primaryButton("+ Sumar al pedido");
        btnAdd.addActionListener(e -> agregarProducto());
        card.add(btnAdd, c);

        return shell;
    }

    private JPanel buildOrderColumn() {
        JPanel shell = UiTheme.sectionCard("Pedido actual");
        JPanel card = UiTheme.cardBody(shell);
        card.setLayout(new BorderLayout(0, 12));

        listaUI.setFont(UiTheme.BODY);
        listaUI.setBackground(UiTheme.SURFACE_ALT);
        listaUI.setForeground(UiTheme.INK);
        listaUI.setSelectionBackground(UiTheme.ACCENT);
        listaUI.setSelectionForeground(Color.WHITE);
        listaUI.setFixedCellHeight(36);
        listaUI.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(listaUI);
        UiTheme.styleScroll(scroll);
        card.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);

        JPanel totalRow = new UiTheme.RoundedPanel(14, UiTheme.SURFACE_ALT, UiTheme.BORDER);
        totalRow.setLayout(new BorderLayout());
        totalRow.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel totalCaption = new JLabel("TOTAL DEL PEDIDO");
        totalCaption.setFont(UiTheme.SMALL_BOLD);
        totalCaption.setForeground(UiTheme.MUTED);
        lblTotal.setFont(UiTheme.TOTAL);
        lblTotal.setForeground(UiTheme.ACCENT);
        totalRow.add(totalCaption, BorderLayout.WEST);
        totalRow.add(lblTotal, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton btnQuitar = secondaryButton("Quitar ítem");
        JButton btnVaciar = secondaryButton("Vaciar");
        JButton btnProcesar = primaryButton("Cobrar e imprimir");
        btnQuitar.addActionListener(e -> quitarSeleccionado());
        btnVaciar.addActionListener(e -> vaciarPedido());
        btnProcesar.addActionListener(e -> procesarOrden());
        buttons.add(btnQuitar);
        buttons.add(btnVaciar);
        buttons.add(btnProcesar);

        south.add(totalRow, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        return shell;
    }

    private void agregarProducto() {
        Object selected = comboProducto.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No hay productos cargados. Usá «Productos» para crear el menú.");
            return;
        }

        String precioTxt = txtPrecioUnitario.getText().trim();
        if (!precioTxt.matches("\\d+(\\.\\d+)?")) {
            JOptionPane.showMessageDialog(this, "El precio unitario debe ser un número válido.");
            return;
        }

        try {
            spinCantidad.commitEdit();
        } catch (Exception ignored) {
            // Si el texto no es válido, se mantiene el valor anterior del modelo.
        }
        int cantidad = (Integer) spinCantidad.getValue();
        String nota = txtNota.getText().trim();
        Pedido pedido = new Pedido(selected.toString(), precioTxt, nota, cantidad);
        listaPedidos.add(pedido);
        listModel.addElement(pedido);
        actualizarTotal();
        spinCantidad.setValue(1);
        txtNota.setText("");
        txtCodigo.setText("");
        txtCodigo.requestFocusInWindow();
    }

    private void buscarPorCodigo() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            return;
        }
        String sql = "SELECT nombre FROM menuPrecios "
                + "WHERE trim(COALESCE(sku, '')) <> '' AND lower(trim(sku)) = lower(?) "
                + "ORDER BY id LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this,
                            "No hay un producto con el código " + codigo + ".");
                    txtCodigo.requestFocusInWindow();
                    txtCodigo.selectAll();
                    return;
                }
                String nombre = rs.getString("nombre");
                comboProducto.setSelectedItem(nombre);
                if (!nombre.equals(String.valueOf(comboProducto.getSelectedItem()))) {
                    for (int i = 0; i < comboProducto.getItemCount(); i++) {
                        if (nombre.equalsIgnoreCase(comboProducto.getItemAt(i))) {
                            comboProducto.setSelectedIndex(i);
                            break;
                        }
                    }
                }
                txtCodigo.setText("");
                txtCodigo.requestFocusInWindow();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "No se pudo buscar el código.\n" + e.getMessage(),
                    "Base de datos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void quitarSeleccionado() {
        int index = listaUI.getSelectedIndex();
        if (index < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná un ítem del pedido.");
            return;
        }
        listaPedidos.remove(index);
        listModel.remove(index);
        actualizarTotal();
    }

    private void vaciarPedido() {
        if (listaPedidos.isEmpty()) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "¿Vaciar el pedido actual?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            listaPedidos.clear();
            listModel.clear();
            actualizarTotal();
        }
    }

    private void actualizarTotal() {
        double total = 0;
        for (Pedido p : listaPedidos) {
            total += p.getSubtotal();
        }
        lblTotal.setText("$" + String.format("%.0f", total));
    }

    private double getTotalPedido() {
        double total = 0;
        for (Pedido p : listaPedidos) {
            total += p.getSubtotal();
        }
        return total;
    }

    private void procesarOrden() {
        if (listaPedidos.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agregá al menos un producto al pedido.");
            return;
        }

        double total = getTotalPedido();
        String ticket = TicketPrinter.buildSaleTicket(listaPedidos);

        // Guardar venta SIEMPRE, aunque falle la impresora.
        ArrayList<String> sinStock = new ArrayList<>();
        boolean guardado = registrarVenta(total, sinStock);
        TicketPrinter.PrintResult result = TicketPrinter.printCopies(ticket, 1);

        if (!result.printed) {
            mostrarVistaPrevia("Vista previa del ticket", result.ticketText);
        }

        String msg = (guardado ? "Venta registrada. " : "No se pudo guardar el informe. ")
                + result.message;
        if (guardado && !sinStock.isEmpty()) {
            msg += "\n\nNo se descontó stock para: " + String.join(", ", sinStock)
                    + ".\nCreá o alineá el nombre en Stock si corresponde.";
        }
        JOptionPane.showMessageDialog(this, msg, "Pedido procesado",
                guardado ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (guardado) {
            listaPedidos.clear();
            listModel.clear();
            actualizarTotal();
            txtNota.setText("");
        }
    }

    private boolean registrarVenta(double total) {
        return registrarVenta(total, null);
    }

    private boolean registrarVenta(double total, ArrayList<String> sinStock) {
        Date ahora = new Date();
        String totalStr = String.valueOf(total);
        try (Connection connection = new conection().getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (existeInformeDiario(connection, ahora)) {
                    añadirInformeExistente(connection, ahora, totalStr);
                } else {
                    sumoInforme(connection, ahora, totalStr);
                }
                registrarDetalleVenta(connection, ahora);
                descontarStockVenta(connection, sinStock);
                connection.commit();
                return true;
            } catch (Exception e) {
                connection.rollback();
                System.out.println("Error al registrar venta: " + e.getMessage());
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar venta: " + e.getMessage());
            return false;
        }
    }

    /** Si el producto coincide con un ítem de stock, descuenta solo ese ítem y nunca deja cantidad negativa. */
    private void descontarStockVenta(Connection connection, ArrayList<String> sinStock) throws SQLException {
        String sql = "UPDATE stock SET cantidad = MAX(cantidad - ?, 0) "
                + "WHERE id = (SELECT id FROM stock WHERE lower(nombre) = lower(?) ORDER BY id LIMIT 1)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Pedido p : listaPedidos) {
                ps.setInt(1, p.getCantidad());
                ps.setString(2, p.getNombreProd());
                int updated = ps.executeUpdate();
                if (updated == 0 && sinStock != null) {
                    String nombre = p.getNombreProd();
                    if (nombre != null && !sinStock.contains(nombre)) {
                        sinStock.add(nombre);
                    }
                }
            }
        }
    }

    /** Guarda ítems con tipo/costo para informes de ganancia y gráficos. */
    private void registrarDetalleVenta(Connection connection, Date fecha) throws SQLException {
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fecha);
        String sqlMeta = "SELECT COALESCE(tipo, 'Otro') AS tipo, COALESCE(costo, 0) AS costo "
                + "FROM menuPrecios WHERE nombre = ? ORDER BY id LIMIT 1";
        String sqlIns = "INSERT INTO venta_detalle "
                + "(fecha, producto, tipo, cantidad, precio_unit, costo_unit, subtotal, costo_total) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        for (Pedido p : listaPedidos) {
            String tipo = "Otro";
            double costoUnit = 0;
            try (PreparedStatement meta = connection.prepareStatement(sqlMeta)) {
                meta.setString(1, p.getNombreProd());
                try (ResultSet rs = meta.executeQuery()) {
                    if (rs.next()) {
                        tipo = rs.getString("tipo");
                        if (tipo == null || tipo.isBlank()) {
                            tipo = "Otro";
                        }
                        costoUnit = rs.getDouble("costo");
                    }
                }
            }
            double precioUnit = p.getPrecioUnitario();
            double subtotal = p.getSubtotal();
            double costoTotal = costoUnit * p.getCantidad();
            try (PreparedStatement ins = connection.prepareStatement(sqlIns)) {
                ins.setString(1, ahora);
                ins.setString(2, p.getNombreProd());
                ins.setString(3, tipo);
                ins.setInt(4, p.getCantidad());
                ins.setDouble(5, precioUnit);
                ins.setDouble(6, costoUnit);
                ins.setDouble(7, subtotal);
                ins.setDouble(8, costoTotal);
                ins.executeUpdate();
            }
        }
    }

    private void abrirInformes() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        if (!AdminPin.pedir(frame)) {
            return;
        }
        InformesDialog dialog = new InformesDialog(frame);
        dialog.setVisible(true);
    }

    private void abrirCaja() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        CajaDialog dialog = new CajaDialog(frame);
        dialog.setVisible(true);
    }

    private void abrirStock() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        StockDialog dialog = new StockDialog(frame);
        dialog.setVisible(true);
    }

    private void abrirProveedores() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        new ProveedoresDialog(frame).setVisible(true);
    }

    private void abrirCompras() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        new ComprasDialog(frame).setVisible(true);
    }

    private void abrirMerma() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        new MermaDialog(frame).setVisible(true);
    }

    private void abrirProductos() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        ProductosDialog dialog = new ProductosDialog(frame, this::cargarMenu);
        dialog.setVisible(true);
    }

    private void cargarMenu() {
        // Evita que addItem dispare consultas anidadas mientras se recarga el combo.
        comboProducto.removeActionListener(productoListener);
        comboProducto.removeAllItems();
        String sql = "SELECT nombre, precio FROM menuPrecios ORDER BY nombre COLLATE NOCASE";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comboProducto.addItem(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar el menú: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "No se pudo cargar el menú.\n" + e.getMessage(),
                    "Base de datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            comboProducto.addActionListener(productoListener);
        }
        if (comboProducto.getItemCount() > 0) {
            comboProducto.setSelectedIndex(0);
            cargarPrecioProducto();
        } else {
            txtPrecioUnitario.setText("");
        }
    }

    private void cargarPrecioProducto() {
        Object selected = comboProducto.getSelectedItem();
        if (selected == null) {
            txtPrecioUnitario.setText("");
            return;
        }
        nombreSeleccionado = selected.toString();
        String sql = "SELECT precio FROM menuPrecios WHERE nombre = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nombreSeleccionado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int precio = (int) Math.round(rs.getDouble("precio"));
                    precioSeleccionado = String.valueOf(precio);
                    txtPrecioUnitario.setText(precioSeleccionado);
                } else {
                    txtPrecioUnitario.setText("");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar precio: " + e.getMessage());
        }
    }

    public void sumoInforme(conection conn, Date dateInforme, String precioGlobal) {
        try (Connection connection = conn.getConnection()) {
            sumoInforme(connection, dateInforme, precioGlobal);
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Error al guardar informe: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sumoInforme(Connection connection, Date dateInforme, String precioGlobal) throws SQLException {
        // fechaActivo existe en DBs viejas como NOT NULL: se completa con la misma fecha.
        String insertSql = "INSERT INTO informes (fecha, fechaActivo, total) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha = dateFormat.format(dateInforme);
            insertStmt.setString(1, fecha);
            insertStmt.setString(2, fecha);
            insertStmt.setDouble(3, Double.parseDouble(precioGlobal));
            insertStmt.executeUpdate();
        }
    }

    public Boolean existeInformeDiario(conection conn, Date dateInforme) {
        try (Connection connection = conn.getConnection()) {
            return existeInformeDiario(connection, dateInforme);
        } catch (SQLException e) {
            System.out.println("Error al verificar informe diario: " + e.getMessage());
            return false;
        }
    }

    private boolean existeInformeDiario(Connection connection, Date dateInforme) throws SQLException {
        String selectSql = "SELECT 1 FROM informes WHERE DATE(fecha) = ? LIMIT 1";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, new SimpleDateFormat("yyyy-MM-dd").format(dateInforme));
            try (ResultSet rs = selectStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void añadirInformeExistente(conection conn, Date dateInforme, String precioGlobal) {
        try (Connection connection = conn.getConnection()) {
            añadirInformeExistente(connection, dateInforme, precioGlobal);
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Error al actualizar informe existente: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void añadirInformeExistente(Connection connection, Date dateInforme, String precioGlobal) throws SQLException {
        String selectSql = "SELECT id, total FROM informes WHERE DATE(fecha) = ? ORDER BY id LIMIT 1";
        String updateSql = "UPDATE informes SET total = ? WHERE id = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, new SimpleDateFormat("yyyy-MM-dd").format(dateInforme));
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    int idInforme = rs.getInt("id");
                    double totalNuevo = rs.getDouble("total") + Double.parseDouble(precioGlobal);
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, totalNuevo);
                        updateStmt.setInt(2, idInforme);
                        updateStmt.executeUpdate();
                    }
                } else {
                    sumoInforme(connection, dateInforme, precioGlobal);
                }
            }
        }
    }

    private void mostrarVistaPrevia(String titulo, String texto) {
        JTextArea area = new JTextArea(texto, 18, 36);
        area.setFont(UiTheme.MONO);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), titulo, JOptionPane.PLAIN_MESSAGE);
    }

    private ImageIcon loadLogo() {
        File logo = ComercioConfig.get().resolveLogoFile();
        if (logo != null && logo.exists()) {
            return scaleIcon(logo.getAbsolutePath(), 48, 48);
        }
        return null;
    }

    private ImageIcon scaleIcon(String path, int w, int h) {
        ImageIcon raw = new ImageIcon(path);
        return new ImageIcon(raw.getImage().getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.SMALL_BOLD);
        l.setForeground(UiTheme.MUTED);
        return l;
    }

    private static void styleField(JTextField f) {
        UiTheme.styleField(f);
    }

    private static void styleCombo(JComboBox<?> combo) {
        UiTheme.styleCombo(combo);
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }

    private static JButton navButton(String text) {
        return UiTheme.navButton(text);
    }
}
