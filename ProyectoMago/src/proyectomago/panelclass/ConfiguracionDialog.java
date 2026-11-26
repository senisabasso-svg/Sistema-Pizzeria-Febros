package proyectomago.panelclass;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import javax.print.PrintService;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Configuración del comercio: marca, impresora y activación remota (URL / id / key).
 */
public class ConfiguracionDialog extends JDialog {

    private final ComercioConfig config = ComercioConfig.get();
    private final JTextField txtNombre = new JTextField(24);
    private final JComboBox<String> comboImpresora = new JComboBox<>();
    private final JCheckBox chkLogoTicket = new JCheckBox("Imprimir logo en el ticket");
    private final JComboBox<LogoSizeOption> comboLogoSize = new JComboBox<>();
    private final JLabel lblLogoPct = new JLabel("Tamaño del logo en ticket");
    private final JTextField txtApiUrl = new JTextField(28);

    private static final class LogoSizeOption {
        final int pct;
        final String label;

        LogoSizeOption(int pct, String label) {
            this.pct = pct;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    private final JTextField txtIdCliente = new JTextField(28);
    private final JPasswordField txtAccessKey = new JPasswordField(28);
    private final JLabel lblLicencia = new JLabel(" ");
    private final JLabel lblPreview = new JLabel();
    private final JLabel lblLogoPath = new JLabel("Sin logo");
    private File logoSeleccionado;
    private final Runnable onSaved;

    public ConfiguracionDialog(JFrame owner, Runnable onSaved) {
        super(owner, "Configuración comercio", true);
        this.onSaved = onSaved;

        setSize(620, 720);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JScrollPane scroll = new JScrollPane(buildForm());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTheme.BG);
        scroll.setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        cargarDatos();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 4, 6, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        int y = 0;

        c.gridy = y++;
        form.add(section("Comercio"), c);

        c.gridy = y++;
        form.add(label("Nombre del comercio (aparece en el ticket)"), c);
        c.gridy = y++;
        styleField(txtNombre);
        form.add(txtNombre, c);

        c.gridy = y++;
        form.add(label("Logo del comercio"), c);

        JPanel logoRow = new JPanel(new BorderLayout(10, 0));
        logoRow.setOpaque(false);
        lblPreview.setPreferredSize(new Dimension(72, 72));
        lblPreview.setHorizontalAlignment(JLabel.CENTER);
        lblPreview.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        lblPreview.setOpaque(true);
        lblPreview.setBackground(UiTheme.SURFACE_ALT);

        JPanel logoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoActions.setOpaque(false);
        JButton btnElegir = secondaryButton("Elegir imagen...");
        JButton btnQuitar = secondaryButton("Quitar logo");
        btnElegir.addActionListener(e -> elegirLogo());
        btnQuitar.addActionListener(e -> quitarLogo());
        logoActions.add(btnElegir);
        logoActions.add(btnQuitar);

        JPanel logoInfo = new JPanel(new BorderLayout(0, 6));
        logoInfo.setOpaque(false);
        lblLogoPath.setFont(UiTheme.SMALL);
        lblLogoPath.setForeground(UiTheme.MUTED);
        logoInfo.add(logoActions, BorderLayout.NORTH);
        logoInfo.add(lblLogoPath, BorderLayout.SOUTH);

        logoRow.add(lblPreview, BorderLayout.WEST);
        logoRow.add(logoInfo, BorderLayout.CENTER);
        c.gridy = y++;
        form.add(logoRow, c);

        c.gridy = y++;
        form.add(label("Impresora de tickets"), c);
        c.gridy = y++;
        styleCombo(comboImpresora);
        form.add(comboImpresora, c);

        JButton btnRefresh = secondaryButton("Actualizar lista de impresoras");
        btnRefresh.addActionListener(e -> cargarImpresoras(config.getImpresora()));
        c.gridy = y++;
        form.add(btnRefresh, c);

        chkLogoTicket.setOpaque(false);
        chkLogoTicket.setFont(UiTheme.BODY);
        chkLogoTicket.setForeground(UiTheme.INK);
        chkLogoTicket.setToolTipText("Si está marcado, intenta imprimir el logo arriba del ticket. "
                + "Si falla o no hay logo, imprime solo texto como siempre.");
        chkLogoTicket.addActionListener(e -> actualizarEstadoLogoPct());
        c.gridy = y++;
        form.add(chkLogoTicket, c);

        lblLogoPct.setFont(UiTheme.BODY_BOLD);
        lblLogoPct.setForeground(UiTheme.INK);
        c.gridy = y++;
        form.add(lblLogoPct, c);

        comboLogoSize.removeAllItems();
        comboLogoSize.addItem(new LogoSizeOption(45, "1 · Chico"));
        comboLogoSize.addItem(new LogoSizeOption(60, "2 · Normal"));
        comboLogoSize.addItem(new LogoSizeOption(75, "3 · Mediano"));
        comboLogoSize.addItem(new LogoSizeOption(90, "4 · Grande"));
        comboLogoSize.addItem(new LogoSizeOption(100, "5 · Muy grande"));
        comboLogoSize.addItem(new LogoSizeOption(110, "6 · Máximo (ancho completo)"));
        styleCombo(comboLogoSize);
        c.gridy = y++;
        form.add(comboLogoSize, c);

        JLabel tipLogo = new JLabel("<html>Elegí una de las 6 opciones. «Máximo» usa casi todo el ancho del ticket. "
                + "Solo aplica si «Imprimir logo» está marcado.</html>");
        tipLogo.setFont(UiTheme.SMALL);
        tipLogo.setForeground(UiTheme.MUTED);
        c.gridy = y++;
        form.add(tipLogo, c);

        c.gridy = y++;
        form.add(section("Activación / licencia remota"), c);

        c.gridy = y++;
        form.add(label("URL de la API (ej: https://tudominio.com)"), c);
        c.gridy = y++;
        styleField(txtApiUrl);
        form.add(txtApiUrl, c);

        c.gridy = y++;
        form.add(label("idCliente (UUID)"), c);
        c.gridy = y++;
        styleField(txtIdCliente);
        form.add(txtIdCliente, c);

        c.gridy = y++;
        form.add(label("AccessKey"), c);
        c.gridy = y++;
        stylePassword(txtAccessKey);
        form.add(txtAccessKey, c);

        JPanel licenseActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        licenseActions.setOpaque(false);
        JButton btnValidar = secondaryButton("Validar estado ahora");
        btnValidar.addActionListener(e -> validarAhora());
        licenseActions.add(btnValidar);
        c.gridy = y++;
        form.add(licenseActions, c);

        lblLicencia.setFont(UiTheme.SMALL);
        lblLicencia.setForeground(UiTheme.MUTED);
        c.gridy = y++;
        form.add(lblLicencia, c);

        JLabel tip = new JLabel("<html>Si la API responde <b>activo</b>, el sistema puede usarse "
                + ComercioConfig.DIAS_CACHE_LICENCIA
                + " días sin internet.<br>"
                + "Si lo pasás a <b>inactivo</b> en el servidor, al vencer la caché deja de abrir.</html>");
        tip.setFont(UiTheme.SMALL);
        tip.setForeground(UiTheme.MUTED);
        c.gridy = y++;
        form.add(tip, c);

        return form;
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnCancelar = secondaryButton("Cancelar");
        JButton btnGuardar = primaryButton("Guardar");
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        actions.add(btnCancelar);
        actions.add(btnGuardar);
        return actions;
    }

    private void cargarDatos() {
        txtNombre.setText(config.getNombreComercio());
        txtApiUrl.setText(config.getApiUrl());
        txtIdCliente.setText(config.getIdCliente());
        txtAccessKey.setText(config.getAccessKey());
        cargarImpresoras(config.getImpresora());
        chkLogoTicket.setSelected(config.isImprimirLogoTicket());
        seleccionarLogoSize(config.getLogoTicketPorcentaje());
        actualizarEstadoLogoPct();
        File logo = config.resolveLogoFile();
        logoSeleccionado = logo;
        mostrarPreview(logo);
        actualizarLabelLicencia();
    }

    private void seleccionarLogoSize(int pct) {
        for (int i = 0; i < comboLogoSize.getItemCount(); i++) {
            LogoSizeOption opt = comboLogoSize.getItemAt(i);
            if (opt != null && opt.pct == pct) {
                comboLogoSize.setSelectedIndex(i);
                return;
            }
        }
        // Valor viejo: el más cercano
        int bestIdx = 2;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < comboLogoSize.getItemCount(); i++) {
            LogoSizeOption opt = comboLogoSize.getItemAt(i);
            int d = Math.abs(opt.pct - pct);
            if (d < bestDist) {
                bestDist = d;
                bestIdx = i;
            }
        }
        comboLogoSize.setSelectedIndex(bestIdx);
    }

    private void actualizarEstadoLogoPct() {
        boolean on = chkLogoTicket.isSelected();
        comboLogoSize.setEnabled(on);
        lblLogoPct.setEnabled(on);
        lblLogoPct.setForeground(on ? UiTheme.INK : UiTheme.MUTED);
    }

    private void actualizarLabelLicencia() {
        if (config.licenciaCacheVigente()) {
            long horas = Math.max(1, config.millisRestantesCache() / (60L * 60L * 1000L));
            lblLicencia.setText("Estado local: activo (caché ~" + horas + " h restantes)");
            lblLicencia.setForeground(UiTheme.SUCCESS);
        } else if ("inactivo".equalsIgnoreCase(config.getLastStatus())) {
            lblLicencia.setText("Último estado conocido: inactivo");
            lblLicencia.setForeground(UiTheme.ACCENT);
        } else if (!config.tieneCredencialesLicencia()) {
            lblLicencia.setText("Sin credenciales de activación cargadas");
            lblLicencia.setForeground(UiTheme.MUTED);
        } else {
            lblLicencia.setText("Caché vencida o sin validar — hace falta internet");
            lblLicencia.setForeground(UiTheme.MUTED);
        }
    }

    private void validarAhora() {
        // Guardar temporalmente los campos de licencia en memoria para la prueba.
        config.setApiUrl(txtApiUrl.getText());
        config.setIdCliente(txtIdCliente.getText());
        config.setAccessKey(new String(txtAccessKey.getPassword()));

        LocalSystemLicense.Result result = LocalSystemLicense.validateNow();
        actualizarLabelLicencia();
        int type = result.allowed() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE;
        JOptionPane.showMessageDialog(this, result.message, "Validación de estado", type);
    }

    private void cargarImpresoras(String selected) {
        comboImpresora.removeAllItems();
        comboImpresora.addItem("(Automática)");
        PrintService[] services = config.listarImpresoras();
        String match = null;
        for (PrintService s : services) {
            comboImpresora.addItem(s.getName());
            if (selected != null && selected.equalsIgnoreCase(s.getName())) {
                match = s.getName();
            }
        }
        if (match != null) {
            comboImpresora.setSelectedItem(match);
        } else if (selected == null || selected.isBlank()) {
            comboImpresora.setSelectedIndex(0);
        }
    }

    private void elegirLogo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Elegir logo del comercio");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (PNG, JPG, GIF)", "png", "jpg", "jpeg", "gif"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            logoSeleccionado = chooser.getSelectedFile();
            mostrarPreview(logoSeleccionado);
        }
    }

    private void quitarLogo() {
        logoSeleccionado = null;
        lblPreview.setIcon(null);
        lblPreview.setText("Sin logo");
        lblLogoPath.setText("Sin logo (se usará el predeterminado si existe)");
    }

    private void mostrarPreview(File file) {
        if (file == null || !file.exists()) {
            lblPreview.setIcon(null);
            lblPreview.setText("Sin logo");
            lblLogoPath.setText("Sin logo");
            return;
        }
        ImageIcon raw = new ImageIcon(file.getAbsolutePath());
        Image scaled = raw.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        lblPreview.setText("");
        lblPreview.setIcon(new ImageIcon(scaled));
        lblLogoPath.setText(file.getName());
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresá el nombre del comercio.");
            return;
        }

        try {
            config.setNombreComercio(nombre);
            config.setApiUrl(txtApiUrl.getText());
            config.setIdCliente(txtIdCliente.getText());
            config.setAccessKey(new String(txtAccessKey.getPassword()));

            Object printerItem = comboImpresora.getSelectedItem();
            if (printerItem == null || "(Automática)".equals(printerItem.toString())) {
                config.setImpresora("");
            } else {
                config.setImpresora(printerItem.toString());
            }
            config.setImprimirLogoTicket(chkLogoTicket.isSelected());
            LogoSizeOption sizeOpt = (LogoSizeOption) comboLogoSize.getSelectedItem();
            config.setLogoTicketPorcentaje(sizeOpt != null ? sizeOpt.pct : 75);

            if (logoSeleccionado != null && logoSeleccionado.exists()) {
                File destinoEsperadoPng = config.getDefaultLogoFile();
                if (logoSeleccionado.getAbsolutePath().equalsIgnoreCase(destinoEsperadoPng.getAbsolutePath())
                        || logoSeleccionado.getName().startsWith("logo_comercio")) {
                    config.setLogoPath(logoSeleccionado.getAbsolutePath());
                } else {
                    File imported = config.importarLogo(logoSeleccionado);
                    config.setLogoPath(imported.getAbsolutePath());
                }
            } else {
                config.setLogoPath("");
            }

            config.save();

            // Si hay credenciales, intenta validar al guardar (si hay red).
            if (config.tieneCredencialesLicencia() && !config.licenciaCacheVigente()) {
                LocalSystemLicense.Result result = LocalSystemLicense.validateNow();
                actualizarLabelLicencia();
                if (!result.allowed()) {
                    JOptionPane.showMessageDialog(this,
                            "Configuración guardada, pero la licencia no quedó activa:\n" + result.message,
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Configuración guardada.\n" + result.message);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Configuración guardada.");
            }

            if (onSaved != null) {
                onSaved.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JLabel section(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.HEADING);
        l.setForeground(UiTheme.INK);
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 2, 0));
        return l;
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
        f.setBackground(UiTheme.SURFACE_ALT); f.setForeground(UiTheme.INK);
        f.setPreferredSize(new Dimension(10, 34));
    }

    private static void stylePassword(JPasswordField f) {
        f.setFont(UiTheme.BODY);
        f.setBorder(UiTheme.fieldBorder());
        f.setBackground(UiTheme.SURFACE_ALT); f.setForeground(UiTheme.INK);
        f.setPreferredSize(new Dimension(10, 34));
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
