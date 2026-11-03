package proyectomago.panelclass;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * Configuración local del comercio (nombre, logo, impresora).
 * Se guarda junto al JAR para instalación portable sin internet.
 */
public final class ComercioConfig {

    private static final String FILE_NAME = "comercio.properties";
    private static final String LOGO_FILE_NAME = "logo_comercio.png";
    private static final String KEY_NOMBRE = "nombreComercio";
    private static final String KEY_LOGO = "logoPath";
    private static final String KEY_IMPRESORA = "impresora";
    private static final String DEFAULT_NOMBRE = "GARAJE GOURMET";

    private static ComercioConfig instance;

    private String nombreComercio = DEFAULT_NOMBRE;
    private String logoPath = "";
    private String impresora = "";

    private ComercioConfig() {
        load();
    }

    public static synchronized ComercioConfig get() {
        if (instance == null) {
            instance = new ComercioConfig();
        }
        return instance;
    }

    public String getNombreComercio() {
        return nombreComercio == null || nombreComercio.isBlank() ? DEFAULT_NOMBRE : nombreComercio.trim();
    }

    public void setNombreComercio(String nombreComercio) {
        this.nombreComercio = nombreComercio == null ? DEFAULT_NOMBRE : nombreComercio.trim();
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath == null ? "" : logoPath.trim();
    }

    public String getImpresora() {
        return impresora;
    }

    public void setImpresora(String impresora) {
        this.impresora = impresora == null ? "" : impresora.trim();
    }

    public File getConfigDir() {
        try {
            File jarDir = new File(ComercioConfig.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getParentFile();
            if (jarDir != null && jarDir.isDirectory()) {
                return jarDir;
            }
        } catch (Exception ignored) {
        }
        return new File(System.getProperty("user.dir"));
    }

    public File getPropertiesFile() {
        return new File(getConfigDir(), FILE_NAME);
    }

    public File getDefaultLogoFile() {
        return new File(getConfigDir(), LOGO_FILE_NAME);
    }

    /**
     * Copia la imagen elegida a logo_comercio junto al JAR y guarda la ruta.
     */
    public File importarLogo(File origen) throws IOException {
        if (origen == null || !origen.exists()) {
            throw new IOException("No se encontró el archivo de logo.");
        }
        File destino = getDefaultLogoFile();
        // Conservar extensión original si no es png.
        String name = origen.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            destino = new File(getConfigDir(), "logo_comercio.jpg");
        } else if (name.endsWith(".gif")) {
            destino = new File(getConfigDir(), "logo_comercio.gif");
        }
        Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
        this.logoPath = destino.getAbsolutePath();
        return destino;
    }

    public File resolveLogoFile() {
        if (logoPath != null && !logoPath.isBlank()) {
            File configured = new File(logoPath);
            if (configured.exists()) {
                return configured;
            }
        }
        File localPng = getDefaultLogoFile();
        if (localPng.exists()) {
            return localPng;
        }
        File jpg = new File(getConfigDir(), "logo_comercio.jpg");
        if (jpg.exists()) {
            return jpg;
        }
        File descarga = new File(getConfigDir(), "descarga.png");
        if (descarga.exists()) {
            return descarga;
        }
        File parent = getConfigDir().getParentFile();
        if (parent != null) {
            File parentLogo = new File(parent, "descarga.png");
            if (parentLogo.exists()) {
                return parentLogo;
            }
        }
        return null;
    }

    public PrintService[] listarImpresoras() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        return services == null ? new PrintService[0] : services;
    }

    public PrintService resolvePrinter() {
        PrintService[] services = listarImpresoras();
        if (services.length == 0) {
            return null;
        }
        if (impresora != null && !impresora.isBlank()) {
            for (PrintService s : services) {
                if (impresora.equalsIgnoreCase(s.getName())) {
                    return s;
                }
            }
        }
        for (PrintService s : services) {
            String name = s.getName();
            if (name != null && (name.contains("POS-80C") || name.toUpperCase().contains("POS"))) {
                return s;
            }
        }
        return services[0];
    }

    public void load() {
        File file = getPropertiesFile();
        if (!file.exists()) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
            nombreComercio = props.getProperty(KEY_NOMBRE, DEFAULT_NOMBRE);
            logoPath = props.getProperty(KEY_LOGO, "");
            impresora = props.getProperty(KEY_IMPRESORA, "");
        } catch (IOException e) {
            System.out.println("No se pudo leer comercio.properties: " + e.getMessage());
        }
    }

    public void save() throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_NOMBRE, getNombreComercio());
        props.setProperty(KEY_LOGO, logoPath == null ? "" : logoPath);
        props.setProperty(KEY_IMPRESORA, impresora == null ? "" : impresora);
        File file = getPropertiesFile();
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Configuracion del comercio - Sistema Pizzeria");
        }
    }
}
