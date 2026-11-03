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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Configuración del comercio: nombre, logo e impresora de tickets.
 */
public class ConfiguracionDialog extends JDialog {

    private final ComercioConfig config = ComercioConfig.get();
    private final JTextField txtNombre = new JTextField(24);
    private final JComboBox<String> comboImpresora = new JComboBox<>();
    private final JLabel lblPreview = new JLabel();
    private final JLabel lblLogoPath = new JLabel("Sin logo");
    private File logoSeleccionado;
    private final Runnable onSaved;

    public ConfiguracionDialog(JFrame owner, Runnable onSaved) {
        super(owner, "Configuración comercio", true);
        this.onSaved = onSaved;

        setSize(560, 460);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UiTheme.BG);
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(buildForm(), BorderLayout.CENTER);
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

        c.gridy = 0;
        form.add(label("Nombre del comercio (aparece en el ticket)"), c);
        c.gridy = 1;
        styleField(txtNombre);
        form.add(txtNombre, c);

        c.gridy = 2;
        form.add(label("Logo del comercio"), c);

        JPanel logoRow = new JPanel(new BorderLayout(10, 0));
        logoRow.setOpaque(false);
        lblPreview.setPreferredSize(new Dimension(72, 72));
        lblPreview.setHorizontalAlignment(JLabel.CENTER);
        lblPreview.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        lblPreview.setOpaque(true);
        lblPreview.setBackground(Color.WHITE);

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
        c.gridy = 3;
        form.add(logoRow, c);

        c.gridy = 4;
        form.add(label("Impresora de tickets"), c);
        c.gridy = 5;
        styleCombo(comboImpresora);
        form.add(comboImpresora, c);

        JButton btnRefresh = secondaryButton("Actualizar lista de impresoras");
        btnRefresh.addActionListener(e -> cargarImpresoras(config.getImpresora()));
        c.gridy = 6;
        form.add(btnRefresh, c);

        JLabel tip = new JLabel("<html>Los cambios se guardan en esta PC (junto al programa).<br>"
                + "El nombre y el logo se usan en pantalla y en los tickets.</html>");
        tip.setFont(UiTheme.SMALL);
        tip.setForeground(UiTheme.MUTED);
        c.gridy = 7;
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
        cargarImpresoras(config.getImpresora());
        File logo = config.resolveLogoFile();
        logoSeleccionado = logo;
        mostrarPreview(logo);
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

            Object printerItem = comboImpresora.getSelectedItem();
            if (printerItem == null || "(Automática)".equals(printerItem.toString())) {
                config.setImpresora("");
            } else {
                config.setImpresora(printerItem.toString());
            }

            if (logoSeleccionado != null && logoSeleccionado.exists()) {
                // Si ya es el logo guardado en la carpeta del programa, solo confirmar path.
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
            JOptionPane.showMessageDialog(this, "Configuración guardada.");
            if (onSaved != null) {
                onSaved.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
        f.setBackground(Color.WHITE);
        f.setPreferredSize(new Dimension(10, 34));
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setFont(UiTheme.BODY);
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(10, 34));
    }

    private static JButton primaryButton(String text) {
        return UiTheme.primaryButton(text);
    }

    private static JButton secondaryButton(String text) {
        return UiTheme.secondaryButton(text);
    }
}
