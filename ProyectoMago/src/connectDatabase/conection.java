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
                + "precio REAL NOT NULL"
                + ");";

        String informes = "CREATE TABLE IF NOT EXISTS informes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fecha TEXT NOT NULL, "
                + "fechaActivo TEXT, "
                + "total REAL NOT NULL"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(menu);
            stmt.execute(informes);
        } catch (SQLException e) {
            System.out.println("Error al verificar tablas: " + e.getMessage());
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
