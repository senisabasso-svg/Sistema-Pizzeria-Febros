import connectDatabase.conection;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Helpers compartidos para la suite QA de Gestión almacenes. */
public final class QaSupport {

    private QaSupport() {
    }

    public static String dbPath() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return conection.resolveDatabasePath();
    }

    public static void wipeDb() throws Exception {
        String path = dbPath();
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement st = c.createStatement()) {
            String[] tables = {
                "venta_detalle", "informes", "movimientos_caja", "cajas",
                "compra_detalle", "compras", "mermas", "stock", "proveedores",
                "menuPrecios", "vendedores"
            };
            for (String t : tables) {
                try {
                    st.execute("DELETE FROM " + t);
                } catch (Exception ignored) {
                }
            }
            try {
                st.execute("DELETE FROM sqlite_sequence");
            } catch (Exception ignored) {
            }
        }
        try (Connection boot = new conection().getConnection()) {
            // ensureSchema
        }
    }

    public static void seedBaseProduct() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath())) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO menuPrecios (nombre, precio, costo, tipo, sku) VALUES (?,?,?,?,?)")) {
                ps.setString(1, "Agua 500");
                ps.setInt(2, 100);
                ps.setInt(3, 40);
                ps.setString(4, "Bebidas");
                ps.setString(5, "7790000000500");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock (nombre, cantidad, unidad, costo_unitario, minimo, categoria) "
                            + "VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, "Agua 500");
                ps.setDouble(2, 100);
                ps.setString(3, "u");
                ps.setDouble(4, 40);
                ps.setDouble(5, 5);
                ps.setString(6, "Bebida");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO stock (nombre, cantidad, unidad, costo_unitario, minimo, categoria) "
                            + "VALUES ('Coca Cola 1.5L', 20, 'u', 68, 2, 'Bebida')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO proveedores (nombre, telefono, rubro, nota) VALUES (?,?,?,?)")) {
                ps.setString(1, "Prov QA");
                ps.setString(2, "099");
                ps.setString(3, "Bebidas");
                ps.setString(4, "");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO vendedores (nombre) VALUES ('Vend QA')")) {
                ps.executeUpdate();
            }
        }
    }

    public static void openCaja(double monto, String vendedor) throws Exception {
        String ahora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis() - 3000));
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath());
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO cajas (fecha_apertura, monto_apertura, estado, vendedor) VALUES (?,?, 'ABIERTA', ?)")) {
            ps.setString(1, ahora);
            ps.setDouble(2, monto);
            ps.setString(3, vendedor);
            ps.executeUpdate();
        }
    }

    public static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    public static Method method(Class<?> type, String name, Class<?>... args) throws Exception {
        Method m = type.getDeclaredMethod(name, args);
        m.setAccessible(true);
        return m;
    }

    public static int count(String sql, Object... args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath());
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public static double sum(String sql, Object... args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath());
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public static double stockQty(String nombre) throws Exception {
        return sum("SELECT cantidad FROM stock WHERE lower(nombre)=lower(?) ORDER BY id LIMIT 1", nombre);
    }

    public static double stockCosto(String nombre) throws Exception {
        return sum("SELECT costo_unitario FROM stock WHERE lower(nombre)=lower(?) ORDER BY id LIMIT 1", nombre);
    }

    public static List<Component> findAll(Container root, Class<?> type) {
        List<Component> out = new ArrayList<>();
        collect(root, type, out);
        return out;
    }

    private static void collect(Container root, Class<?> type, List<Component> out) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c)) {
                out.add(c);
            }
            if (c instanceof Container) {
                collect((Container) c, type, out);
            }
        }
    }

    public static JButton findButton(Container root, String text) {
        for (Component c : findAll(root, JButton.class)) {
            JButton b = (JButton) c;
            if (text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    public static void edt(Runnable r) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeAndWait(r);
        }
    }

    @SuppressWarnings("unchecked")
    public static JSpinner spinnerCantidad(Object panel) throws Exception {
        return (JSpinner) field(panel, "spinCantidad");
    }

    @SuppressWarnings("unchecked")
    public static JComboBox<String> comboProducto(Object panel) throws Exception {
        return (JComboBox<String>) field(panel, "comboProducto");
    }

    public static JTextField txtPrecio(Object panel) throws Exception {
        return (JTextField) field(panel, "txtPrecioUnitario");
    }

    public static JTextField txtCostoCompras(Object dlg) throws Exception {
        return (JTextField) field(dlg, "txtCosto");
    }
}
