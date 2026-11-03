package connectDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class conection {

    // Variable para mantener la conexión activa
    private static Connection connection;

    // Método para obtener la URL de la base de datos con la ruta especificada
    private static String getDatabaseURL() {
        // Ruta absoluta de la base de datos (actualizada)
        String databasePath = "C:\\Users\\senis\\OneDrive\\Escritorio\\ProyectoMago\\ProyectoMago\\prueba.db";

//        String databasePath = "C:\\Users\\PC\\Desktop\\ProyectoMago\\ProyectoMago\\prueba.db";
        // Mostrar la ruta del directorio para depuración
        System.out.println("Ruta de la base de datos: " + databasePath);

        // Devuelve la URL completa para la conexión a la base de datos SQLite
        return "jdbc:sqlite:" + databasePath;
    }

    // Método para obtener la conexión a la base de datos
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Si la conexión no está activa o es nula, abrir una nueva
            String databaseUrl = getDatabaseURL();
            connection = DriverManager.getConnection(databaseUrl);
            System.out.println("Conexión a la base de datos establecida.");
        }
        return connection;
    }

    // Método para realizar la conexión y ejecutar el SELECT
    public void main(String[] args) {
        // Cargar el controlador SQLite
        try {
            Class.forName("org.sqlite.JDBC"); // Cargar el controlador
            System.out.println("Controlador SQLite cargado correctamente.");
        } catch (ClassNotFoundException e) {
            System.out.println("Error al cargar el controlador: " + e.getMessage());
            return;  // Detener el programa si no se puede cargar el controlador
        }

        try (Connection conn = getConnection()) {

            // Verificar que la conexión fue exitosa
            if (conn != null) {
                // Verificar si la tabla "menuPrecios" existe. Si no, crearla.
                createTableIfNotExists(conn);
                createTableIfNotExistsInformes(conn);

                // Crear la consulta SQL
                String sql = "SELECT * FROM menuPrecios WHERE nombre = ?";

                // Crear el PreparedStatement
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    // Establecer el parámetro del nombre en la consulta
                    pstmt.setString(1, "milanesa");

                    // Ejecutar la consulta
                    try (ResultSet rs = pstmt.executeQuery()) {
                        // Verificar si se obtuvo algún resultado
                        if (rs.next()) {
                            // Obtener los datos de la consulta
                            int id = rs.getInt("id"); // Suponiendo que la tabla tiene un campo id
                            String nombre = rs.getString("nombre");
                            double precio = rs.getDouble("precio"); // Suponiendo que la tabla tiene un campo precio

                            // Mostrar los resultados
                            System.out.println("ID: " + id);
                            System.out.println("Nombre: " + nombre);
                            System.out.println("Precio: " + precio);
                        } else {
                            System.out.println("No se encontró ningún registro con el nombre 'milanesa'.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }

    // Método para crear la tabla "menuPrecios" si no existe
    private static void createTableIfNotExists(Connection conn) {
        // SQL para crear la tabla si no existe
        String createTableSQL = "CREATE TABLE IF NOT EXISTS menuPrecios ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "nombre TEXT NOT NULL, "
                + "precio REAL NOT NULL"
                + ");";

        // Crear la tabla
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla 'menuPrecios' verificada o creada.");
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla: " + e.getMessage());
        }
    }

    // Método para crear la tabla "menuPrecios" si no existe
    private static void createTableIfNotExistsInformes(Connection conn) {
        // SQL para crear la tabla si no existe
        String createTableSQL = "CREATE TABLE IF NOT EXISTS informes ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "fecha TEXT NOT NULL, "
                + "fechaActivo TEXT NOT NULL, "
                + "total REAL NOT NULL"
                + ");";

        // Crear la tabla
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Tabla 'informes' verificada o creada.");
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla informes: " + e.getMessage());
        }
    }

    // Método para crear y devolver un PreparedStatement
    public PreparedStatement prepareStatement(Connection conn, String sql) {
        try {
            // Crear y devolver un PreparedStatement con la conexión proporcionada
            return conn.prepareStatement(sql);
        } catch (SQLException e) {
            System.out.println("Error al preparar la consulta: " + e.getMessage());
            return null;
        }
    }
}
