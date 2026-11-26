package proyectomago.panelclass;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import javax.imageio.ImageIO;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;

/**
 * Impresión térmica ESC/POS con impresora configurada o vista previa.
 * Opcionalmente antepone el logo como imagen raster (GS v 0).
 */
public final class TicketPrinter {

    private static final Charset CHARSET = Charset.forName("UTF-8");
    /** Ancho útil típico de térmica 80 mm (puntos). */
    private static final int PAPER_WIDTH_DOTS = 576;
    /** Pie fijo FEBROS (soldado en el ticket). */
    private static final String FEBROS_FOOTER_TEXT = "sistema de https://febrospuntodeventa.com/";
    private static final String FEBROS_MARK_RESOURCE = "/proyectomago/resources/febros_mark.png";
    private static final String FEBROS_MARK_FILE = "febros_mark.png";

    private TicketPrinter() {
    }

    public static String getLocalName() {
        return ComercioConfig.get().getNombreComercio();
    }

    /** @deprecated usar getLocalName() */
    public static final String LOCAL_NAME = "GARAJE GOURMET";

    public static class PrintResult {
        public final boolean printed;
        public final String message;
        public final String ticketText;

        public PrintResult(boolean printed, String message, String ticketText) {
            this.printed = printed;
            this.message = message;
            this.ticketText = ticketText;
        }
    }

    public static String buildSaleTicket(List<Pedido> pedidos, String direccion, String notaCliente) {
        return buildSaleTicket(pedidos, direccion, notaCliente, null, null, null);
    }

    public static String buildSaleTicket(List<Pedido> pedidos, String direccion, String notaCliente,
                                        PrincipalPanel.ClienteVenta cliente) {
        if (cliente == null) {
            return buildSaleTicket(pedidos, direccion, notaCliente, null, null, null);
        }
        return buildSaleTicket(pedidos, direccion, notaCliente,
                cliente.nombre, cliente.celular, cliente.documento);
    }

    public static String buildSaleTicket(List<Pedido> pedidos, String direccion, String notaCliente,
                                        String clienteNombre, String celular, String documento) {
        String nombre = getLocalName();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String fecha = String.format("%02d-%02d-%04d",
                now.getDayOfMonth(), now.getMonthValue(), now.getYear());
        String hora = String.format("%02d:%02d:%02d",
                now.getHour(), now.getMinute(), now.getSecond());

        /*
         * Layout pensado para térmica 80 mm (~42 caracteres):
         * nombre pegado al logo → meta → tabla con columnas equilibradas → total con peso → gracias.
         * (El pie largo / promo se hace en un paso siguiente.)
         */
        StringBuilder sb = new StringBuilder();
        sb.append(center(nombre)).append("\n");
        sb.append("\n");
        sb.append("Fecha: ").append(fecha).append("\n");
        sb.append("Hora:  ").append(hora).append("\n");
        if (clienteNombre != null && !clienteNombre.isBlank()) {
            sb.append("Cliente: ").append(fit(clienteNombre.trim(), W - 9)).append("\n");
        }
        if (celular != null && !celular.isBlank()) {
            sb.append("Cel: ").append(fit(celular.trim(), W - 5)).append("\n");
        }
        if (documento != null && !documento.isBlank()) {
            sb.append("Doc: ").append(fit(documento.trim(), W - 5)).append("\n");
        }
        if (direccion != null && !direccion.isBlank()) {
            sb.append("Dir: ").append(fit(direccion.trim(), W - 5)).append("\n");
        }
        if (notaCliente != null && !notaCliente.isBlank()) {
            sb.append("Nota: ").append(fit(notaCliente.trim(), W - 6)).append("\n");
        }
        sb.append("\n");
        sb.append(dashed()).append("\n");
        sb.append(colHeader()).append("\n");
        sb.append(dashed()).append("\n");
        double total = 0;
        if (pedidos != null) {
            for (Pedido p : pedidos) {
                sb.append(formatItemLines(p));
                total += p.getSubtotal();
            }
        }
        sb.append(dashed()).append("\n");
        sb.append(totalLine(total)).append("\n");
        sb.append("\n");
        sb.append(center("Muchas gracias por su visita")).append("\n");
        sb.append("\n");
        sb.append(FEBROS_FOOTER_TEXT).append("\n");
        sb.append("\n");
        return sb.toString();
    }

    public static String buildDeliveryTicket(String totalTexto, String direccion, String detallePedido) {
        String nombre = getLocalName();
        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------\n");
        sb.append(center(nombre)).append("\n");
        sb.append(center("TICKET REPARTIDOR")).append("\n");
        sb.append("--------------------------------\n");
        if (direccion != null && !direccion.isBlank()) {
            sb.append("DIRECCION:\n").append(direccion.trim()).append("\n\n");
        } else {
            sb.append("RETIRO EN LOCAL\n\n");
        }
        if (detallePedido != null && !detallePedido.isBlank()) {
            sb.append(detallePedido).append("\n");
        }
        sb.append("TOTAL: $").append(totalTexto).append("\n");
        sb.append("--------------------------------\n\n\n");
        return sb.toString();
    }

    public static PrintResult printCopies(String ticketBody, int copies) {
        try {
            PrintService printer = ComercioConfig.get().resolvePrinter();
            if (printer == null) {
                return new PrintResult(false, "No hay impresora disponible. Se muestra vista previa.", ticketBody);
            }

            byte[] cut = new byte[]{0x1D, 'V', 1};
            byte[] logoBytes = buildLogoEscPosOrEmpty();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 1; i <= copies; i++) {
                if (logoBytes.length > 0) {
                    // El raster del logo ya termina con salto; no agregar otro (evita el hueco enorme).
                    out.write(logoBytes);
                }
                if (copies > 1) {
                    out.write(("---------- TICKET " + i + " ----------\n").getBytes(CHARSET));
                }
                writeTicketBodyEscPos(out, ticketBody);
                out.write(buildFebrosFooterEscPos());
                // La cuchilla está más abajo que el cabezal: hay que alimentar
                // varias líneas o el pie FEBROS queda cortado / no se ve.
                out.write(new byte[]{0x1B, 'd', 6}); // ESC d n — avanzar n líneas
                out.write(cut);
            }

            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(out.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
            String extra = logoBytes.length > 0 ? " (con logo)" : "";
            return new PrintResult(true, "Impresión enviada a " + printer.getName() + extra, ticketBody);
        } catch (Exception e) {
            return new PrintResult(false, "Error al imprimir: " + e.getMessage(), ticketBody);
        }
    }

    /**
     * Pie fijo: isotipo FEBROS chico a la izquierda + texto del sistema a la derecha.
     * Va siempre (no depende de configuración).
     */
    private static byte[] buildFebrosFooterEscPos() {
        try {
            BufferedImage mark = loadFebrosMark();
            if (mark == null) {
                // Fallback solo texto
                ByteArrayOutputStream fallback = new ByteArrayOutputStream();
                fallback.write(new byte[]{0x1B, 'a', 0});
                fallback.write('\n');
                fallback.write(FEBROS_FOOTER_TEXT.getBytes(CHARSET));
                fallback.write('\n');
                return fallback.toByteArray();
            }

            // Solo el icono (lado izquierdo del wordmark), fondo negro → blanco, naranja → negro.
            BufferedImage icon = extractFebrosIcon(mark);
            int iconH = 48;
            int iconW = Math.max(8, (int) Math.round(icon.getWidth() * (iconH / (double) icon.getHeight())));
            iconW = (iconW + 7) / 8 * 8;

            BufferedImage iconMono = new BufferedImage(iconW, iconH, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D gi = iconMono.createGraphics();
            gi.setColor(Color.WHITE);
            gi.fillRect(0, 0, iconW, iconH);
            gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gi.drawImage(toPrintableMark(icon), 0, 0, iconW, iconH, null);
            gi.dispose();

            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics2D gp = probe.createGraphics();
            gp.setFont(font);
            FontMetrics fm = gp.getFontMetrics();
            int textW = fm.stringWidth(FEBROS_FOOTER_TEXT);
            int textH = fm.getAscent() + fm.getDescent();
            gp.dispose();

            int gap = 10;
            int stripH = Math.max(iconH, textH) + 6;
            int stripW = Math.min(PAPER_WIDTH_DOTS, iconW + gap + textW + 8);
            stripW = Math.max(iconW + gap + 8, (stripW + 7) / 8 * 8);

            BufferedImage strip = new BufferedImage(stripW, stripH, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = strip.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, stripW, stripH);
            g.drawImage(iconMono, 0, (stripH - iconH) / 2, null);
            g.setColor(Color.BLACK);
            g.setFont(font);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            int tx = iconW + gap;
            int ty = (stripH + fm.getAscent() - fm.getDescent()) / 2;
            // Si no entra en una línea, partir en dos
            if (tx + textW > stripW - 4) {
                String line1 = "sistema de";
                String line2 = "https://febrospuntodeventa.com/";
                g.drawString(line1, tx, stripH / 2 - 2);
                g.drawString(line2, tx, stripH / 2 + fm.getAscent());
            } else {
                g.drawString(FEBROS_FOOTER_TEXT, tx, ty);
            }
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write('\n');
            out.write(new byte[]{0x1B, 'a', 0});
            out.write(encodeGsV0(strip));
            out.write('\n');
            return out.toByteArray();
        } catch (Exception e) {
            System.out.println("No se pudo armar pie FEBROS: " + e.getMessage());
            try {
                ByteArrayOutputStream fallback = new ByteArrayOutputStream();
                fallback.write('\n');
                fallback.write(FEBROS_FOOTER_TEXT.getBytes(CHARSET));
                fallback.write('\n');
                return fallback.toByteArray();
            } catch (Exception ignored) {
                return new byte[0];
            }
        }
    }

    private static BufferedImage loadFebrosMark() {
        try (InputStream in = TicketPrinter.class.getResourceAsStream(FEBROS_MARK_RESOURCE)) {
            if (in != null) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            File local = new File(ComercioConfig.get().getConfigDir(), FEBROS_MARK_FILE);
            if (local.exists()) {
                return ImageIO.read(local);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Recorta la zona del icono (izquierda del wordmark). */
    private static BufferedImage extractFebrosIcon(BufferedImage mark) {
        int w = mark.getWidth();
        int h = mark.getHeight();
        // El icono ocupa aprox. el primer tercio / cuadrado izquierdo
        int side = Math.min(h, Math.max(1, w / 3));
        side = Math.min(side, w);
        return mark.getSubimage(0, 0, side, h);
    }

    /** Fondo oscuro → blanco; naranja/claro → negro (imprimible en térmica). */
    private static BufferedImage toPrintableMark(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int gr = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (r * 30 + gr * 59 + b * 11) / 100;
                // En el mark: negro=fondo, naranja/blanco=figura
                boolean ink = a > 20 && lum > 40;
                out.setRGB(x, y, ink ? 0x000000 : 0xFFFFFF);
            }
        }
        return out;
    }

    /**
     * Raster ESC/POS del logo si la opción está activa y el archivo existe.
     * Ante cualquier error devuelve vacío → el ticket sigue solo texto.
     */
    private static byte[] buildLogoEscPosOrEmpty() {
        ComercioConfig cfg = ComercioConfig.get();
        if (!cfg.isImprimirLogoTicket()) {
            return new byte[0];
        }
        File logoFile = cfg.resolveLogoFile();
        if (logoFile == null || !logoFile.exists()) {
            return new byte[0];
        }
        try {
            BufferedImage src = ImageIO.read(logoFile);
            if (src == null) {
                return new byte[0];
            }
            // pct = % del ancho del papel (45…110). 110 ≈ un poco más ancho/alto.
            int pct = cfg.getLogoTicketPorcentaje();
            int maxW = Math.max(96, (int) Math.round(PAPER_WIDTH_DOTS * (pct / 100.0)));
            maxW = Math.min(maxW, PAPER_WIDTH_DOTS);
            // Altura generosa para que el logo se vea grande (antes el tope era muy bajo).
            int maxH = Math.max(80, (int) Math.round(maxW * 0.85));
            if (pct >= 100) {
                maxH = Math.max(maxH, 360);
            }
            if (pct >= 110) {
                maxH = Math.max(maxH, 420);
            }
            BufferedImage mono = toMonoRaster(src, maxW, maxH);
            // Recorta márgenes blancos; keepPad deja un respiro chico debajo del dibujo.
            mono = trimWhiteMargins(mono, 4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[]{0x1B, 'a', 1}); // centrar
            out.write(encodeGsV0(mono));
            // Un solo salto: punto medio entre “muy separado” y “pegado”.
            out.write('\n');
            out.write(new byte[]{0x1B, 'a', 0}); // izquierda para el texto
            return out.toByteArray();
        } catch (Exception e) {
            System.out.println("No se pudo preparar logo para ticket: " + e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Elimina filas/columnas casi blancas alrededor del logo para que no “empuje”
     * el nombre del local hacia abajo.
     */
    private static BufferedImage trimWhiteMargins(BufferedImage mono, int keepPad) {
        int w = mono.getWidth();
        int h = mono.getHeight();
        int top = 0;
        int bottom = h - 1;
        int left = 0;
        int right = w - 1;

        while (top < h && rowIsWhite(mono, top)) {
            top++;
        }
        while (bottom > top && rowIsWhite(mono, bottom)) {
            bottom--;
        }
        while (left < w && colIsWhite(mono, left, top, bottom)) {
            left++;
        }
        while (right > left && colIsWhite(mono, right, top, bottom)) {
            right--;
        }
        if (top >= bottom || left >= right) {
            return mono;
        }

        top = Math.max(0, top - keepPad);
        left = Math.max(0, left - keepPad);
        bottom = Math.min(h - 1, bottom + keepPad);
        right = Math.min(w - 1, right + keepPad);

        int nw = right - left + 1;
        // Ancho múltiplo de 8 para ESC/POS
        nw = (nw + 7) / 8 * 8;
        if (left + nw > w) {
            left = Math.max(0, w - nw);
            nw = Math.min(nw, w - left);
            nw = nw / 8 * 8;
            if (nw <= 0) {
                return mono;
            }
        }
        int nh = bottom - top + 1;
        BufferedImage cropped = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = cropped.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(mono, 0, 0, nw, nh, left, top, left + nw, top + nh, null);
        g.dispose();
        return cropped;
    }

    private static boolean rowIsWhite(BufferedImage img, int y) {
        int w = img.getWidth();
        for (int x = 0; x < w; x++) {
            if ((img.getRGB(x, y) & 0xFFFFFF) == 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean colIsWhite(BufferedImage img, int x, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            if ((img.getRGB(x, y) & 0xFFFFFF) == 0) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage toMonoRaster(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        // Permite agrandar logos chicos hasta el tamaño elegido (antes no upscaleaba).
        double scale = Math.min(maxW / (double) w, maxH / (double) h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        // Ancho múltiplo de 8 para empaquetado limpio
        nw = (nw + 7) / 8 * 8;
        if (nw > maxW) {
            nw = maxW / 8 * 8;
        }
        int nh = Math.max(1, (int) Math.round(h * (nw / (double) w)));
        if (nh > maxH) {
            nh = maxH;
        }

        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        BufferedImage mono = new BufferedImage(nw, nh, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int gr = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (r * 30 + gr * 59 + b * 11) / 100;
                // Negro = imprime; blanco = no
                mono.setRGB(x, y, lum < 180 ? 0x000000 : 0xFFFFFF);
            }
        }
        return mono;
    }

    /** GS v 0 m xL xH yL yH d1..dk */
    private static byte[] encodeGsV0(BufferedImage mono) throws Exception {
        int width = mono.getWidth();
        int height = mono.getHeight();
        int widthBytes = (width + 7) / 8;
        ByteArrayOutputStream data = new ByteArrayOutputStream(widthBytes * height);
        for (int y = 0; y < height; y++) {
            for (int xByte = 0; xByte < widthBytes; xByte++) {
                int b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int x = xByte * 8 + bit;
                    if (x < width) {
                        int rgb = mono.getRGB(x, y) & 0xFFFFFF;
                        // En ESC/POS, bit 1 = punto negro
                        if (rgb == 0) {
                            b |= (0x80 >> bit);
                        }
                    }
                }
                data.write(b);
            }
        }
        byte[] raster = data.toByteArray();
        ByteArrayOutputStream cmd = new ByteArrayOutputStream(8 + raster.length);
        cmd.write(0x1D);
        cmd.write('v');
        cmd.write('0');
        cmd.write(0); // m = normal
        cmd.write(widthBytes & 0xFF);
        cmd.write((widthBytes >> 8) & 0xFF);
        cmd.write(height & 0xFF);
        cmd.write((height >> 8) & 0xFF);
        cmd.write(raster);
        return cmd.toByteArray();
    }

    /**
     * Ancho de texto para POS-80 (~80 mm). Con 32 el ticket se veía “chico y apretado”
     * al lado del logo; 42 equilibra nombre completo + columnas legibles.
     */
    private static final int W = 42;
    // 20 + 6 + 8 + 8 = 42  → Articulo amplio, números con aire entre sí
    private static final int COL_ART = 20;
    private static final int COL_CANT = 6;
    private static final int COL_PREC = 8;
    private static final int COL_IMP = 8;

    /**
     * Nombre centrado+negrita; TOTAL en negrita + doble altura (jerarquía real en térmica).
     */
    private static void writeTicketBodyEscPos(ByteArrayOutputStream out, String ticketBody) throws Exception {
        byte[] boldOn = new byte[]{0x1B, 'E', 1};
        byte[] boldOff = new byte[]{0x1B, 'E', 0};
        byte[] alignCenter = new byte[]{0x1B, 'a', 1};
        byte[] alignLeft = new byte[]{0x1B, 'a', 0};
        // GS ! n — bit 4 = doble altura (no ensancha, no rompe columnas)
        byte[] doubleHOn = new byte[]{0x1D, '!', 0x10};
        byte[] doubleHOff = new byte[]{0x1D, '!', 0x00};

        String[] lines = ticketBody.split("\n", -1);
        boolean firstTextLine = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (firstTextLine && !trimmed.isEmpty()) {
                out.write(alignCenter);
                out.write(boldOn);
                out.write(trimmed.getBytes(CHARSET));
                out.write('\n');
                out.write(boldOff);
                out.write(alignLeft);
                firstTextLine = false;
                continue;
            }
            firstTextLine = false;
            // El pie FEBROS se imprime después como icono+texto (evitar duplicar la línea).
            if (trimmed.equals(FEBROS_FOOTER_TEXT)) {
                continue;
            }
            if (trimmed.startsWith("TOTAL")) {
                out.write(boldOn);
                out.write(doubleHOn);
                out.write(line.getBytes(CHARSET));
                out.write('\n');
                out.write(doubleHOff);
                out.write(boldOff);
            } else {
                out.write(line.getBytes(CHARSET));
                out.write('\n');
            }
        }
    }

    private static String dashed() {
        return "-".repeat(W);
    }

    private static String colHeader() {
        return padRight("Articulo", COL_ART)
                + padLeft("Cant", COL_CANT)
                + padLeft("Prec", COL_PREC)
                + padLeft("Import", COL_IMP);
    }

    private static String totalLine(double total) {
        String label = "TOTAL $:";
        String amount = money(total);
        int gap = Math.max(1, W - label.length() - amount.length());
        return label + " ".repeat(gap) + amount;
    }

    private static String formatItemLines(Pedido p) {
        StringBuilder lines = new StringBuilder();
        String name = p.getNombreProd() == null ? "" : p.getNombreProd().trim();
        String cant = String.format(java.util.Locale.US, "%.2f", (double) p.getCantidad());
        String prec = money(p.getPrecioUnitario());
        String imp = money(p.getSubtotal());
        String nums = padLeft(cant, COL_CANT) + padLeft(prec, COL_PREC) + padLeft(imp, COL_IMP);

        if (name.length() <= COL_ART) {
            lines.append(padRight(name, COL_ART)).append(nums).append("\n");
        } else {
            // Nombre largo: línea propia; cifras alineadas debajo (legible, sin cortar feo)
            lines.append(fit(name, W)).append("\n");
            lines.append(padRight("", COL_ART)).append(nums).append("\n");
        }

        String nota = p.getNota();
        if (nota != null && !nota.isBlank() && !"Sin Aderezos".equalsIgnoreCase(nota.trim())) {
            lines.append("  (").append(fit(nota.trim(), W - 4)).append(")\n");
        }
        return lines.toString();
    }

    private static String money(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private static String center(String text) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= W) {
            return text.substring(0, W);
        }
        int pad = (W - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private static String padRight(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() > width) {
            return text.substring(0, width);
        }
        return text + " ".repeat(width - text.length());
    }

    private static String padLeft(String text, int width) {
        if (text == null) {
            text = "";
        }
        if (text.length() > width) {
            return text.substring(text.length() - width);
        }
        return " ".repeat(width - text.length()) + text;
    }

    private static String fit(String text, int width) {
        if (text == null) {
            return "";
        }
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width);
    }

    private static String wrapLeft(String text, int width) {
        if (text == null) {
            return "";
        }
        if (text.length() <= width) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + width, text.length());
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(text, i, end);
            i = end;
        }
        return sb.toString();
    }
}
