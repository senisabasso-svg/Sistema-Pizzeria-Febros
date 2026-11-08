package proyectomago.panelclass;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;

/**
 * Impresión térmica ESC/POS con impresora configurada o vista previa.
 */
public final class TicketPrinter {

    private static final Charset CHARSET = Charset.forName("UTF-8");

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
        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------\n");
        sb.append(center(nombre)).append("\n");
        sb.append(center("TICKET DE VENTA")).append("\n");
        sb.append("--------------------------------\n");
        double total = 0;
        for (Pedido p : pedidos) {
            sb.append(p.toTicketLine()).append("\n");
            total += p.getSubtotal();
        }
        sb.append("--------------------------------\n");
        sb.append("ITEMS: ").append(pedidos.size()).append("\n");
        sb.append("TOTAL: $").append(String.format("%.0f", total)).append("\n");
        if (clienteNombre != null && !clienteNombre.isBlank()) {
            sb.append("CLIENTE: ").append(clienteNombre.trim()).append("\n");
        }
        if (celular != null && !celular.isBlank()) {
            sb.append("CEL: ").append(celular.trim()).append("\n");
        }
        if (documento != null && !documento.isBlank()) {
            sb.append("DOC: ").append(documento.trim()).append("\n");
        }
        if (direccion != null && !direccion.isBlank()) {
            sb.append("DIR: ").append(direccion.trim()).append("\n");
        }
        if (notaCliente != null && !notaCliente.isBlank()) {
            sb.append("NOTA: ").append(notaCliente.trim()).append("\n");
        }
        sb.append("--------------------------------\n");
        sb.append(center("Gracias por su compra")).append("\n");
        sb.append("\n\n\n");
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
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int i = 1; i <= copies; i++) {
                String header = "---------- TICKET " + i + " ----------\n";
                out.write(header.getBytes(CHARSET));
                out.write(ticketBody.getBytes(CHARSET));
                out.write("\n\n".getBytes(CHARSET));
                out.write(cut);
            }

            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(out.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
            return new PrintResult(true, "Impresión enviada a " + printer.getName(), ticketBody);
        } catch (Exception e) {
            return new PrintResult(false, "Error al imprimir: " + e.getMessage(), ticketBody);
        }
    }

    private static String center(String text) {
        int width = 32;
        if (text == null) {
            text = "";
        }
        if (text.length() >= width) {
            return text;
        }
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }
}
