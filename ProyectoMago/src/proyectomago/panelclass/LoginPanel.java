package proyectomago.panelclass;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * Pantalla de bienvenida / iniciar sesiÃ³n con branding FEBROS.
 */
public class LoginPanel extends JPanel {

    private final Runnable onLogin;
    private BufferedImage background;

    public LoginPanel(Runnable onLogin) {
        this.onLogin = onLogin;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);
        loadBackground();

        JPanel overlay = new JPanel(new GridBagLayout());
        overlay.setOpaque(false);
        overlay.add(buildCard());
        add(overlay, BorderLayout.CENTER);
    }

    private JPanel buildCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 22, 40, 210));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.setColor(new Color(255, 138, 31, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(36, 42, 36, 42));
        card.setPreferredSize(new Dimension(420, 280));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);

        JLabel brand = new JLabel("FEBROS", SwingConstants.CENTER);
        brand.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        brand.setForeground(UiTheme.INK);

        JLabel brandSub = new JLabel("SOFTWARE DEVELOPMENT", SwingConstants.CENTER);
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(UiTheme.MUTED);

        JLabel product = new JLabel("<html><div style='text-align:center'>"
                + "<span style='color:#F7FAFF'>Gestión </span>"
                + "<span style='color:#FF8A1F'><i>panaderías</i></span>"
                + "</div></html>", SwingConstants.CENTER);
        product.setFont(UiTheme.LOGIN_TITLE);

        String comercio = ComercioConfig.get().getNombreComercio();
        JLabel local = new JLabel(comercio, SwingConstants.CENTER);
        local.setFont(UiTheme.BODY);
        local.setForeground(UiTheme.MUTED);

        JButton btn = UiTheme.primaryButton("Iniciar sesiÃ³n");
        btn.setFont(UiTheme.BODY_BOLD);
        btn.setPreferredSize(new Dimension(10, 46));
        btn.addActionListener(e -> {
            if (onLogin != null) {
                onLogin.run();
            }
        });

        c.gridy = 0;
        card.add(brand, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(brandSub, c);
        c.gridy = 2;
        c.insets = new Insets(8, 0, 4, 0);
        card.add(product, c);
        c.gridy = 3;
        c.insets = new Insets(4, 0, 22, 0);
        card.add(local, c);
        c.gridy = 4;
        c.insets = new Insets(8, 0, 0, 0);
        card.add(btn, c);

        return card;
    }

    private void loadBackground() {
        String[] candidates = {
            "login_bg.jpg",
            "assets/login_bg.jpg",
            "dist/login_bg.jpg"
        };
        for (String rel : candidates) {
            File f = new File(System.getProperty("user.dir"), rel);
            if (tryLoad(f)) {
                return;
            }
        }
        try {
            File jarDir = new File(LoginPanel.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();
            if (jarDir != null) {
                if (tryLoad(new File(jarDir, "login_bg.jpg"))) {
                    return;
                }
                if (tryLoad(new File(jarDir, "assets/login_bg.jpg"))) {
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        File cfg = new File(ComercioConfig.get().getConfigDir(), "login_bg.jpg");
        tryLoad(cfg);
    }

    private boolean tryLoad(File f) {
        if (f == null || !f.exists()) {
            return false;
        }
        try {
            background = ImageIO.read(f);
            return background != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (background != null) {
            int pw = getWidth();
            int ph = getHeight();
            int iw = background.getWidth();
            int ih = background.getHeight();
            double scale = Math.max(pw / (double) iw, ph / (double) ih);
            int tw = (int) Math.ceil(iw * scale);
            int th = (int) Math.ceil(ih * scale);
            int x = (pw - tw) / 2;
            int y = (ph - th) / 2;
            g2.drawImage(background.getScaledInstance(tw, th, Image.SCALE_SMOOTH), x, y, null);
            g2.setColor(new Color(8, 16, 28, 90));
            g2.fillRect(0, 0, pw, ph);
        } else {
            g2.setColor(UiTheme.BG);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 138, 31, 40));
            g2.fillOval(-120, -80, 360, 260);
            g2.fillOval(getWidth() - 220, getHeight() - 180, 320, 240);
        }
        g2.dispose();
    }
}
