import connectDatabase.conection;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import proyectomago.panelclass.CajaService;
import proyectomago.panelclass.ComercioConfig;
import proyectomago.panelclass.Pedido;
import proyectomago.panelclass.TicketPrinter;

/**
 * Suite dura: licencia 7 días, caja, muchas ventas, totales ticket, cierre.
 * Usa DB temporal vía -Dproyectomago.db.path
 */
public class HardeningFull {

    private static int fails = 0;
    private static int passes = 0;

    public static void main(String[] args) throws Exception {
        int loops = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        System.out.println("=== HARDENING FULL · loops=" + loops + " · cacheLicencia="
                + ComercioConfig.DIAS_CACHE_LICENCIA + " días ===\n");

        check("licencia cache = 7 días", ComercioConfig.DIAS_CACHE_LICENCIA == 7);

        for (int loop = 1; loop <= loops; loop++) {
            File tmp = File.createTempFile("harden-" + loop + "-", ".db");
            tmp.deleteOnExit();
            System.setProperty("proyectomago.db.path", tmp.getAbsolutePath());
            conection.resetSchemaFlagForTests();

            // Boot schema
            try (Connection c = new conection().getConnection()) {
                // ensureSchema already ran
            }

            seedMenu();
            runLoop(loop);
        }

        System.out.println("\n=== RESULTADO: PASA=" + passes + " FALLA=" + fails + " ===");
        System.exit(fails == 0 ? 0 : 1);
    }

    private static void runLoop(int loop) throws Exception {
        String tag = "L" + loop + " ";

        // Sin caja abierta
        check(tag + "sin caja abierta", !CajaService.hayCajaAbierta());
        check(tag + "getAbierta null", CajaService.getAbierta() == null);

        // Abrir caja
        String ap = stamp();
        try (Connection c = new conection().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO cajas (fecha_apertura, monto_apertura, estado) VALUES (?,?, 'ABIERTA')")) {
            ps.setString(1, ap);
            ps.setDouble(2, 1000);
            ps.executeUpdate();
        }
        check(tag + "hay caja abierta", CajaService.hayCajaAbierta());
        CajaService.CajaAbierta caja = CajaService.getAbierta();
        check(tag + "apertura 1000", caja != null && Math.abs(caja.montoApertura - 1000) < 0.01);

        // Muchas ventas acumuladas (simula registrarVenta: informe + detalle en txn)
        double esperadoVentas = 0;
        int nVentas = 25 + (loop % 10);
        for (int i = 0; i < nVentas; i++) {
            List<Pedido> pedidos = new ArrayList<>();
            pedidos.add(new Pedido("Pizza", "350", "", 1 + (i % 3)));
            if (i % 2 == 0) {
                pedidos.add(new Pedido("Coca", "120", "", 1));
            }
            double total = 0;
            for (Pedido p : pedidos) {
                total += p.getSubtotal();
            }
            total = CajaService.round2(total);

            // ticket coherente
            String ticket = TicketPrinter.buildSaleTicket(pedidos, "Dir " + i, "", "Cli", "099", "1");
            double tTicket = parseTicketTotal(ticket);
            check(tag + "ticket==pedido #" + i, Math.abs(tTicket - total) < 0.009);

            persistVentaTxn(total, pedidos);
            esperadoVentas = CajaService.round2(esperadoVentas + total);
        }

        double ventasTurno = CajaService.sumVentasDesde(ap);
        check(tag + "ventas turno == acumulado (" + nVentas + " ventas)",
                Math.abs(ventasTurno - esperadoVentas) < 0.02);

        // Movimientos
        try (Connection c = new conection().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO movimientos_caja (caja_id, fecha, tipo, monto, concepto) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, caja.id);
            ps.setString(2, stamp());
            ps.setString(3, "ENTRADA");
            ps.setDouble(4, 200);
            ps.setString(5, "cambio");
            ps.executeUpdate();
            ps.setString(3, "SALIDA");
            ps.setDouble(4, 50);
            ps.setString(5, "gasto");
            ps.executeUpdate();
        }

        double esperado = CajaService.esperadoEnCaja(CajaService.getAbierta());
        double manual = CajaService.round2(1000 + esperadoVentas + 200 - 50);
        check(tag + "esperado caja = apertura+ventas+ent-sal", Math.abs(esperado - manual) < 0.02);
        check(tag + "montos cierran con contado correcto", CajaService.montosCierran(esperado, esperado));
        check(tag + "montos NO cierran si falta plata", !CajaService.montosCierran(esperado - 10, esperado));

        // Venta “antes” de apertura no debe contar
        try (Connection c = new conection().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO venta_detalle (fecha, producto, tipo, cantidad, precio_unit, costo_unit, subtotal, costo_total) "
                             + "VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, "2020-01-01 00:00:00");
            ps.setString(2, "Vieja");
            ps.setString(3, "Otro");
            ps.setInt(4, 1);
            ps.setDouble(5, 9999);
            ps.setDouble(6, 0);
            ps.setDouble(7, 9999);
            ps.setDouble(8, 0);
            ps.executeUpdate();
        }
        double ventas2 = CajaService.sumVentasDesde(ap);
        check(tag + "venta vieja no infla turno", Math.abs(ventas2 - esperadoVentas) < 0.02);

        // Cerrar caja
        try (Connection c = new conection().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE cajas SET fecha_cierre=?, monto_esperado=?, monto_contado=?, diferencia=0, estado='CERRADA' "
                             + "WHERE id=? AND estado='ABIERTA'")) {
            ps.setString(1, stamp());
            ps.setDouble(2, esperado);
            ps.setDouble(3, esperado);
            ps.setInt(4, caja.id);
            check(tag + "cierre OK", ps.executeUpdate() == 1);
        }
        check(tag + "tras cierre no hay abierta", !CajaService.hayCajaAbierta());

        // Informe diario: una sola fila, total = suma ventas del día (aprox todas en este test)
        try (Connection c = new conection().getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(total),0) FROM informes")) {
            rs.next();
            check(tag + "una fila informe/día (o pocas)", rs.getInt(1) >= 1);
            check(tag + "informe total >= ventas turno", rs.getDouble(2) + 0.02 >= esperadoVentas);
        }
    }

    private static void persistVentaTxn(double total, List<Pedido> pedidos) throws Exception {
        String ahora = stamp();
        String dia = ahora.substring(0, 10);
        try (Connection c = new conection().getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement up = c.prepareStatement(
                        "UPDATE informes SET total = total + ? WHERE DATE(fecha) = ?")) {
                    up.setDouble(1, total);
                    up.setString(2, dia);
                    if (up.executeUpdate() == 0) {
                        try (PreparedStatement ins = c.prepareStatement(
                                "INSERT INTO informes (fecha, fechaActivo, total) VALUES (?,?,?)")) {
                            ins.setString(1, ahora);
                            ins.setString(2, ahora);
                            ins.setDouble(3, total);
                            ins.executeUpdate();
                        }
                    }
                }
                double suma = 0;
                for (Pedido p : pedidos) {
                    double sub = CajaService.round2(p.getSubtotal());
                    suma = CajaService.round2(suma + sub);
                    try (PreparedStatement ins = c.prepareStatement(
                            "INSERT INTO venta_detalle (fecha, producto, tipo, cantidad, precio_unit, costo_unit, subtotal, costo_total) "
                                    + "VALUES (?,?,?,?,?,?,?,?)")) {
                        ins.setString(1, ahora);
                        ins.setString(2, p.getNombreProd());
                        ins.setString(3, "Comida");
                        ins.setInt(4, p.getCantidad());
                        ins.setDouble(5, p.getPrecioUnitario());
                        ins.setDouble(6, 0);
                        ins.setDouble(7, sub);
                        ins.setDouble(8, 0);
                        ins.executeUpdate();
                    }
                }
                if (Math.abs(suma - total) > 0.009) {
                    throw new IllegalStateException("suma!=total");
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static void seedMenu() throws Exception {
        try (Connection c = new conection().getConnection();
             Statement st = c.createStatement()) {
            st.execute("DELETE FROM menuPrecios");
            st.execute("INSERT INTO menuPrecios (nombre, precio, costo, tipo) VALUES ('Pizza', 350, 100, 'Comida')");
            st.execute("INSERT INTO menuPrecios (nombre, precio, costo, tipo) VALUES ('Coca', 120, 40, 'Bebida')");
        }
    }

    private static double parseTicketTotal(String ticket) {
        for (String line : ticket.split("\n")) {
            if (line.trim().startsWith("TOTAL")) {
                String num = line.replace("TOTAL $:", "").replace("TOTAL $", "")
                        .replace("TOTAL:", "").replace("$", "").trim();
                return Double.parseDouble(num.replace(",", "."));
            }
        }
        return -1;
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passes++;
            System.out.println("PASA  " + name);
        } else {
            fails++;
            System.out.println("FALLA " + name);
        }
    }
}
