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
import javax.swing.BorderFactory;
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
 * Pantalla principal POS para pizzería chica: armar pedido, cobrar/imprimir, delivery e informe del día.
 */
public class PrincipalPanel extends JPanel {

    private final ArrayList<Pedido> listaPedidos = new ArrayList<>();
    private final DefaultListModel<Pedido> listModel = new DefaultListModel<>();

    private final JComboBox<String> comboProducto = new JComboBox<>();
    private final JComboBox<String> comboNota = new JComboBox<>(new String[]{
        "Sin Aderezos", "Mayonesa", "Ketchup", "Mostaza", "Salsa golf", "Picante", "Extra queso"
    });
    private final JSpinner spinCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
    private final JTextField txtPrecioUnitario = new JTextField();
    private final JTextField txtDireccion = new JTextField();
    private final JTextField txtNotaCliente = new JTextField();
    private final JComboBox<KeyValue> comboCliente = new JComboBox<>();
    private final JLabel lblClienteInfo = new JLabel("Sin cliente asociado");
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
        setBorder(new EmptyBorder(12, 14, 14, 14));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        aplicarBranding();
        comboProducto.addActionListener(productoListener);
        comboCliente.addActionListener(e -> onClienteSeleccionado());
        cargarMenu();
        cargarClientes();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(UiTheme.SURFACE);
        header.setBorder(UiTheme.headerBorder());

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        brand.setOpaque(false);

        lblLogo.setPreferredSize(new Dimension(52, 52));
        lblLogo.setHorizontalAlignment(JLabel.CENTER);
        lblLogo.setOpaque(true);
        lblLogo.setBackground(UiTheme.ACCENT_SOFT);
        lblLogo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        lblTituloComercio.setFont(UiTheme.TITLE);
        lblTituloComercio.setForeground(UiTheme.INK);

        JPanel titles = new JPanel(new BorderLayout(0, 2));
        titles.setOpaque(false);
        JLabel subtitle = new JLabel("Punto de Venta · caja · pedidos · delivery");
        subtitle.setFont(UiTheme.SMALL);
        subtitle.setForeground(UiTheme.MUTED);
        titles.add(lblTituloComercio, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);

        brand.add(lblLogo);
        brand.add(titles);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton btnCaja = secondaryButton("Caja");
        JButton btnStock = secondaryButton("Stock");
        JButton btnClientes = secondaryButton("Clientes");
        JButton btnProductos = secondaryButton("Productos");
        JButton btnInforme = secondaryButton("Informes");
        JButton btnConfig = secondaryButton("Configuración");
        btnCaja.addActionListener(e -> abrirCaja());
        btnStock.addActionListener(e -> abrirStock());
        btnClientes.addActionListener(e -> abrirClientes());
        btnProductos.addActionListener(e -> abrirProductos());
        btnInforme.addActionListener(e -> abrirInformes());
        btnConfig.addActionListener(e -> abrirConfiguracion());
        actions.add(btnCaja);
        actions.add(btnStock);
        actions.add(btnClientes);
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
            lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 26));
            lblLogo.setForeground(UiTheme.ACCENT);
            lblLogo.setHorizontalAlignment(JLabel.CENTER);
        }

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame) {
            ((JFrame) w).setTitle(cfg.getNombreComercio() + " · Punto de Venta");
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
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(360, 0));

        left.add(buildAddProductCard(), BorderLayout.NORTH);
        left.add(buildClientCard(), BorderLayout.CENTER);
        return left;
    }

    private JPanel buildAddProductCard() {
        JPanel card = card("Agregar al pedido");
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 4, 7, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        card.add(fieldLabel("Producto"), c);
        c.gridy = 1;
        styleCombo(comboProducto);
        card.add(comboProducto, c);

        c.gridy = 2; c.gridwidth = 1; c.weightx = 0.55;
        card.add(fieldLabel("Nota / aderezo"), c);
        c.gridx = 1; c.weightx = 0.45;
        card.add(fieldLabel("Cant."), c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0.55;
        styleCombo(comboNota);
        card.add(comboNota, c);
        c.gridx = 1; c.weightx = 0.45;
        spinCantidad.setFont(UiTheme.BODY);
        spinCantidad.setPreferredSize(new Dimension(10, 36));
        card.add(spinCantidad, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2; c.weightx = 1;
        card.add(fieldLabel("Precio unitario (editable)"), c);
        c.gridy = 5;
        styleField(txtPrecioUnitario);
        card.add(txtPrecioUnitario, c);

        c.gridy = 6;
        JButton btnAdd = primaryButton("+ Sumar al pedido");
        btnAdd.addActionListener(e -> agregarProducto());
        card.add(btnAdd, c);

        return card;
    }

    private JPanel buildClientCard() {
        JPanel card = card("Cliente / delivery");
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 4, 7, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;

        c.gridy = 0;
        card.add(fieldLabel("Cliente (opcional)"), c);
        c.gridy = 1;
        styleCombo(comboCliente);
        card.add(comboCliente, c);

        c.gridy = 2;
        lblClienteInfo.setFont(UiTheme.SMALL);
        lblClienteInfo.setForeground(UiTheme.MUTED);
        card.add(lblClienteInfo, c);

        JPanel clienteActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        clienteActions.setOpaque(false);
        JButton btnBuscar = secondaryButton("Buscar por cel/doc");
        JButton btnGestion = secondaryButton("Gestionar clientes");
        btnBuscar.addActionListener(e -> buscarClienteRapido());
        btnGestion.addActionListener(e -> abrirClientes());
        clienteActions.add(btnBuscar);
        clienteActions.add(btnGestion);
        c.gridy = 3;
        card.add(clienteActions, c);

        c.gridy = 4;
        card.add(fieldLabel("Dirección (vacío = retiro)"), c);
        c.gridy = 5;
        styleField(txtDireccion);
        card.add(txtDireccion, c);

        c.gridy = 6;
        card.add(fieldLabel("Nota para cocina / cliente"), c);
        c.gridy = 7;
        styleField(txtNotaCliente);
        card.add(txtNotaCliente, c);

        c.gridy = 8;
        JLabel tip = new JLabel("<html>Tip: asociá un cliente solo si hace falta.<br>Celular y documento se guardan con la venta.</html>");
        tip.setFont(UiTheme.SMALL);
        tip.setForeground(UiTheme.MUTED);
        card.add(tip, c);

        return card;
    }

    private JPanel buildOrderColumn() {
        JPanel card = card("Pedido actual");
        card.setLayout(new BorderLayout(0, 12));

        listaUI.setFont(UiTheme.MONO);
        listaUI.setBackground(UiTheme.SURFACE_ALT);
        listaUI.setForeground(UiTheme.INK);
        listaUI.setSelectionBackground(UiTheme.ACCENT_SOFT);
        listaUI.setSelectionForeground(UiTheme.ACCENT);
        listaUI.setFixedCellHeight(32);
        listaUI.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane scroll = new JScrollPane(listaUI);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.getViewport().setBackground(UiTheme.SURFACE_ALT);
        card.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 12));
        south.setOpaque(false);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setBackground(UiTheme.SURFACE_ALT);
        totalRow.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.MatteBorder(0, 4, 0, 0, UiTheme.ACCENT),
                new EmptyBorder(12, 14, 12, 14)
        ));
        JLabel totalCaption = new JLabel("TOTAL A COBRAR");
        totalCaption.setFont(UiTheme.SMALL);
        totalCaption.setForeground(UiTheme.MUTED);
        lblTotal.setFont(UiTheme.TOTAL);
        lblTotal.setForeground(UiTheme.ACCENT);
        JPanel totalLeft = new JPanel(new BorderLayout());
        totalLeft.setOpaque(false);
        totalLeft.add(totalCaption, BorderLayout.NORTH);
        totalRow.add(totalLeft, BorderLayout.WEST);
        totalRow.add(lblTotal, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton btnQuitar = secondaryButton("Quitar ítem");
        JButton btnVaciar = secondaryButton("Vaciar");
        JButton btnDelivery = secondaryButton("Ticket repartidor");
        JButton btnProcesar = primaryButton("Cobrar e imprimir");
        btnQuitar.addActionListener(e -> quitarSeleccionado());
        btnVaciar.addActionListener(e -> vaciarPedido());
        btnDelivery.addActionListener(e -> imprimirRepartidor());
        btnProcesar.addActionListener(e -> procesarOrden());
        buttons.add(btnQuitar);
        buttons.add(btnVaciar);
        buttons.add(btnDelivery);
        buttons.add(btnProcesar);

        south.add(totalRow, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        card.add(south, BorderLayout.SOUTH);
        return card;
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

        int cantidad = (Integer) spinCantidad.getValue();
        String nota = String.valueOf(comboNota.getSelectedItem());
        Pedido pedido = new Pedido(selected.toString(), precioTxt, nota, cantidad);
        listaPedidos.add(pedido);
        listModel.addElement(pedido);
        actualizarTotal();
        spinCantidad.setValue(1);
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
        String direccion = txtDireccion.getText().trim();
        String nota = txtNotaCliente.getText().trim();
        ClienteVenta cliente = getClienteSeleccionado();
        String ticket = TicketPrinter.buildSaleTicket(listaPedidos, direccion, nota, cliente);

        // Guardar venta SIEMPRE, aunque falle la impresora.
        boolean guardado = registrarVenta(total);
        TicketPrinter.PrintResult result = TicketPrinter.printCopies(ticket, 2);

        if (!result.printed) {
            mostrarVistaPrevia("Vista previa del ticket", result.ticketText);
        }

        String msg = (guardado ? "Venta registrada. " : "No se pudo guardar el informe. ")
                + result.message;
        JOptionPane.showMessageDialog(this, msg, "Pedido procesado",
                guardado ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (guardado) {
            listaPedidos.clear();
            listModel.clear();
            actualizarTotal();
            txtDireccion.setText("");
            txtNotaCliente.setText("");
            if (comboCliente.getItemCount() > 0) {
                comboCliente.setSelectedIndex(0);
            }
            onClienteSeleccionado();
        }
    }

    private void imprimirRepartidor() {
        if (listaPedidos.isEmpty() && txtNotaCliente.getText().isBlank() && txtDireccion.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "No hay datos para el ticket de repartidor.");
            return;
        }

        String direccion = txtDireccion.getText().trim();
        if (direccion.isEmpty()) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "No hay dirección. ¿Imprimir igual (retiro / sin dirección)?",
                    "Ticket repartidor", JOptionPane.YES_NO_OPTION);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
        }

        StringBuilder detalle = new StringBuilder();
        for (Pedido p : listaPedidos) {
            detalle.append(p.toTicketLine()).append("\n");
        }
        if (!txtNotaCliente.getText().isBlank()) {
            detalle.append("NOTA: ").append(txtNotaCliente.getText().trim()).append("\n");
        }

        String totalTxt = String.format("%.0f", getTotalPedido());
        if (getTotalPedido() <= 0 && !txtNotaCliente.getText().isBlank()) {
            // Permite imprimir solo con monto manual en la nota si hace falta.
            totalTxt = txtNotaCliente.getText().trim();
        }

        String ticket = TicketPrinter.buildDeliveryTicket(totalTxt, direccion, detalle.toString());
        TicketPrinter.PrintResult result = TicketPrinter.printCopies(ticket, 1);
        if (!result.printed) {
            mostrarVistaPrevia("Ticket repartidor", result.ticketText);
        }
        JOptionPane.showMessageDialog(this, result.message);
    }

    private boolean registrarVenta(double total) {
        Date ahora = new Date();
        String totalStr = String.valueOf(total);
        conection conn = new conection();
        try {
            if (Boolean.TRUE.equals(existeInformeDiario(conn, ahora))) {
                añadirInformeExistente(conn, ahora, totalStr);
            } else {
                sumoInforme(conn, ahora, totalStr);
            }
            registrarDetalleVenta(ahora);
            return true;
        } catch (Exception e) {
            System.out.println("Error al registrar venta: " + e.getMessage());
            return false;
        }
    }

    /** Guarda ítems con tipo/costo y cliente opcional. */
    private void registrarDetalleVenta(Date fecha) {
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fecha);
        ClienteVenta cliente = getClienteSeleccionado();
        String sqlMeta = "SELECT COALESCE(tipo, 'Otro') AS tipo, COALESCE(costo, 0) AS costo "
                + "FROM menuPrecios WHERE nombre = ? LIMIT 1";
        String sqlIns = "INSERT INTO venta_detalle "
                + "(fecha, producto, tipo, cantidad, precio_unit, costo_unit, subtotal, costo_total, "
                + "cliente_id, cliente_celular, cliente_documento) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = new conection().getConnection()) {
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
                    if (cliente != null) {
                        ins.setInt(9, cliente.id);
                        ins.setString(10, cliente.celular);
                        ins.setString(11, cliente.documento);
                    } else {
                        ins.setObject(9, null);
                        ins.setString(10, null);
                        ins.setString(11, null);
                    }
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al guardar detalle de venta: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void cargarClientes() {
        Integer selectedId = null;
        KeyValue current = (KeyValue) comboCliente.getSelectedItem();
        if (current != null && current.getKey() != null && current.getKey() > 0) {
            selectedId = current.getKey();
        }

        comboCliente.removeAllItems();
        comboCliente.addItem(new KeyValue(0, "Sin cliente"));
        String sql = "SELECT id, nombre, celular, documento FROM clientes "
                + "ORDER BY nombre COLLATE NOCASE, id DESC";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String celular = rs.getString("celular");
                String documento = rs.getString("documento");
                String label;
                if (nombre != null && !nombre.isBlank()) {
                    label = nombre + " · " + celular + " · CI " + documento;
                } else {
                    label = celular + " · CI " + documento;
                }
                comboCliente.addItem(new KeyValue(id, label));
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar clientes: " + e.getMessage());
        }

        if (selectedId != null) {
            for (int i = 0; i < comboCliente.getItemCount(); i++) {
                KeyValue kv = comboCliente.getItemAt(i);
                if (selectedId.equals(kv.getKey())) {
                    comboCliente.setSelectedIndex(i);
                    break;
                }
            }
        } else if (comboCliente.getItemCount() > 0) {
            comboCliente.setSelectedIndex(0);
        }
        onClienteSeleccionado();
    }

    private void onClienteSeleccionado() {
        ClienteVenta c = getClienteSeleccionado();
        if (c == null) {
            lblClienteInfo.setText("Sin cliente asociado a esta venta");
            return;
        }
        lblClienteInfo.setText("Cel: " + c.celular + "  ·  Doc: " + c.documento
                + (c.nombre.isBlank() ? "" : "  ·  " + c.nombre));
        if (txtDireccion.getText().isBlank() && c.direccion != null && !c.direccion.isBlank()) {
            txtDireccion.setText(c.direccion);
        }
    }

    private ClienteVenta getClienteSeleccionado() {
        KeyValue kv = (KeyValue) comboCliente.getSelectedItem();
        if (kv == null || kv.getKey() == null || kv.getKey() <= 0) {
            return null;
        }
        String sql = "SELECT id, nombre, celular, documento, direccion FROM clientes WHERE id = ?";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, kv.getKey());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new ClienteVenta(
                        rs.getInt("id"),
                        nullToEmpty(rs.getString("nombre")),
                        nullToEmpty(rs.getString("celular")),
                        nullToEmpty(rs.getString("documento")),
                        nullToEmpty(rs.getString("direccion"))
                );
            }
        } catch (SQLException e) {
            return null;
        }
    }

    private void buscarClienteRapido() {
        String q = JOptionPane.showInputDialog(this,
                "Buscar por celular o documento:", "Buscar cliente", JOptionPane.QUESTION_MESSAGE);
        if (q == null) {
            return;
        }
        q = q.trim();
        if (q.isEmpty()) {
            return;
        }
        String sql = "SELECT id FROM clientes WHERE celular LIKE ? OR documento LIKE ? "
                + "ORDER BY id DESC LIMIT 1";
        try (Connection connection = new conection().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    int crear = JOptionPane.showConfirmDialog(this,
                            "No se encontró. ¿Abrir gestión de clientes para darlo de alta?",
                            "Sin resultados", JOptionPane.YES_NO_OPTION);
                    if (crear == JOptionPane.YES_OPTION) {
                        abrirClientes();
                    }
                    return;
                }
                int id = rs.getInt("id");
                for (int i = 0; i < comboCliente.getItemCount(); i++) {
                    if (id == comboCliente.getItemAt(i).getKey()) {
                        comboCliente.setSelectedIndex(i);
                        onClienteSeleccionado();
                        return;
                    }
                }
                cargarClientes();
                for (int i = 0; i < comboCliente.getItemCount(); i++) {
                    if (id == comboCliente.getItemAt(i).getKey()) {
                        comboCliente.setSelectedIndex(i);
                        onClienteSeleccionado();
                        return;
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage());
        }
    }

    private void abrirClientes() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
        ClientesDialog dialog = new ClientesDialog(frame, this::cargarClientes);
        dialog.setVisible(true);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Datos mínimos del cliente asociado a la venta. */
    static final class ClienteVenta {
        final int id;
        final String nombre;
        final String celular;
        final String documento;
        final String direccion;

        ClienteVenta(int id, String nombre, String celular, String documento, String direccion) {
            this.id = id;
            this.nombre = nombre;
            this.celular = celular;
            this.documento = documento;
            this.direccion = direccion;
        }
    }

    private void abrirInformes() {
        Window w = SwingUtilities.getWindowAncestor(this);
        JFrame frame = (w instanceof JFrame) ? (JFrame) w : null;
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
        // fechaActivo existe en DBs viejas como NOT NULL: se completa con la misma fecha.
        String insertSql = "INSERT INTO informes (fecha, fechaActivo, total) VALUES (?, ?, ?)";
        try (Connection connection = conn.getConnection();
             PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String fecha = dateFormat.format(dateInforme);
            insertStmt.setString(1, fecha);
            insertStmt.setString(2, fecha);
            insertStmt.setDouble(3, Double.parseDouble(precioGlobal));
            insertStmt.executeUpdate();
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Error al guardar informe: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Boolean existeInformeDiario(conection conn, Date dateInforme) {
        String selectSql = "SELECT 1 FROM informes WHERE DATE(fecha) = ? LIMIT 1";
        try (Connection connection = conn.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            selectStmt.setString(1, dateFormat.format(dateInforme));
            try (ResultSet rs = selectStmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Error al verificar informe diario: " + e.getMessage());
            return false;
        }
    }

    public void añadirInformeExistente(conection conn, Date dateInforme, String precioGlobal) {
        String selectSql = "SELECT id, total FROM informes WHERE DATE(fecha) = ? LIMIT 1";
        String updateSql = "UPDATE informes SET total = ? WHERE id = ?";
        try (Connection connection = conn.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            selectStmt.setString(1, dateFormat.format(dateInforme));
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
                    sumoInforme(conn, dateInforme, precioGlobal);
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.out.println("Error al actualizar informe existente: " + e.getMessage());
            throw new RuntimeException(e);
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

    private static JPanel card(String title) {
        JPanel p = new JPanel();
        p.setBackground(UiTheme.SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.MatteBorder(0, 3, 0, 0, UiTheme.ACCENT),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UiTheme.BORDER, 1),
                        BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(12, 14, 14, 14),
                                title,
                                0, 0,
                                UiTheme.HEADING,
                                UiTheme.INK
                        )
                )
        ));
        return p;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(UiTheme.SMALL);
        l.setForeground(UiTheme.MUTED);
        return l;
    }

    private static void styleField(JTextField f) {
        f.setFont(UiTheme.BODY);
        f.setBackground(UiTheme.SURFACE_ALT);
        f.setForeground(UiTheme.INK);
        f.setCaretColor(UiTheme.ACCENT);
        f.setBorder(UiTheme.fieldBorder());
        f.setPreferredSize(new Dimension(10, 38));
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
}
