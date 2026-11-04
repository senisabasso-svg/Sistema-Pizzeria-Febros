package connectDatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión SQLite portable: la DB vive junto al JAR (o en el directorio de trabajo).
 * Cada getConnection() abre una conexión nueva; el caller debe cerrarla (try-with-resources).
 */
public class conection {

    private static final String DB_FILE_NAME = "prueba.db";
    private static boolean schemaReady = false;

    private static String getDatabaseURL() {
        String databasePath = resolveDatabasePath();
        System.out.println("Ruta de la base de datos: " + databasePath);
        return "jdbc:sqlite:" + databasePath;
    }

    /**
     * Prioridad: carpeta del JAR → carpeta padre → crear junto al JAR.
     */
    public static String resolveDatabasePath() {
        try {
            File jarDir = new File(conection.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();

            File inJarDir = new File(jarDir, DB_FILE_NAME);
            if (inJarDir.exists()) {
                return inJarDir.getAbsolutePath();
            }

            File parent = jarDir.getParentFile();
            if (parent != null) {
                File inParent = new File(parent, DB_FILE_NAME);
                if (inParent.exists()) {
                    return inParent.getAbsolutePath();
                }
            }

            return inJarDir.getAbsolutePath();
        } catch (Exception e) {
            File cwd = new File(System.getProperty("user.dir"), DB_FILE_NAME);
            return cwd.getAbsolutePath();
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver SQLite", e);
        }
        Connection connection = DriverManager.getConnection(getDatabaseURL());
        if (!schemaReady) {
            ensureSchema(connection);
            schemaReady = true;
        }
        System.out.println("Conexión a la base de datos establecida.");
        return connection;
    }

    private static void ensureSchema(Connection conn) {
        String menu = "CREATE TABLE IF NOT EXISTS menuPrecios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "precio REAL NOT NULL, "
                + "costo REAL DEFAULT 0"
                + ");";

        String informes = "CREATE TABLE IF NOT EXISTS informes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fecha TEXT NOT NULL, "
                + "fechaActivo TEXT, "
                + "total REAL NOT NULL"
                + ");";

        String cajas = "CREATE TABLE IF NOT EXISTS cajas ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fecha_apertura TEXT NOT NULL, "
                + "monto_apertura REAL NOT NULL, "
                + "fecha_cierre TEXT, "
                + "monto_esperado REAL, "
                + "monto_contado REAL, "
                + "diferencia REAL, "
                + "estado TEXT NOT NULL DEFAULT 'ABIERTA', "
                + "nota TEXT"
                + ");";

        String movimientos = "CREATE TABLE IF NOT EXISTS movimientos_caja ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "caja_id INTEGER NOT NULL, "
                + "fecha TEXT NOT NULL, "
                + "tipo TEXT NOT NULL, "
                + "monto REAL NOT NULL, "
                + "concepto TEXT NOT NULL"
                + ");";

        String stock = "CREATE TABLE IF NOT EXISTS stock ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "cantidad REAL NOT NULL DEFAULT 0, "
                + "unidad TEXT DEFAULT 'u', "
                + "costo_unitario REAL DEFAULT 0, "
                + "minimo REAL DEFAULT 0"
                + ");";

        String ventaDetalle = "CREATE TABLE IF NOT EXISTS venta_detalle ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fecha TEXT NOT NULL, "
                + "producto TEXT NOT NULL, "
                + "tipo TEXT NOT NULL DEFAULT 'Otro', "
                + "cantidad INTEGER NOT NULL DEFAULT 1, "
                + "precio_unit REAL NOT NULL, "
                + "costo_unit REAL NOT NULL DEFAULT 0, "
                + "subtotal REAL NOT NULL, "
                + "costo_total REAL NOT NULL DEFAULT 0"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(menu);
            stmt.execute(informes);
            stmt.execute(cajas);
            stmt.execute(movimientos);
            stmt.execute(stock);
            stmt.execute(ventaDetalle);
            ensureColumn(stmt, "menuPrecios", "costo", "REAL DEFAULT 0");
            ensureColumn(stmt, "menuPrecios", "tipo", "TEXT DEFAULT 'Otro'");
        } catch (SQLException e) {
            System.out.println("Error al verificar tablas: " + e.getMessage());
        }
    }

    /** Migración suave para DBs ya existentes (SQLite no tiene IF NOT EXISTS en ADD COLUMN). */
    private static void ensureColumn(Statement stmt, String table, String column, String definition) {
        try {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // La columna ya existe.
        }
    }

    public PreparedStatement prepareStatement(Connection conn, String sql) {
        try {
            return conn.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("Error al preparar la consulta: " + e.getMessage());
            return null;
        }
    }
}
