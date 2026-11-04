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
 * Configuración local del comercio (marca, impresora y licencia/API).
 * Se guarda junto al JAR para instalación portable.
 */
public final class ComercioConfig {

    private static final String FILE_NAME = "comercio.properties";
    private static final String LOGO_FILE_NAME = "logo_comercio.png";

    private static final String KEY_NOMBRE = "nombreComercio";
    private static final String KEY_LOGO = "logoPath";
    private static final String KEY_IMPRESORA = "impresora";
    private static final String KEY_API_URL = "apiUrl";
    private static final String KEY_ID_CLIENTE = "idCliente";
    private static final String KEY_ACCESS_KEY = "accessKey";
    private static final String KEY_LAST_OK_AT = "lastStatusOkAt";
    private static final String KEY_LAST_STATUS = "lastStatus";

    private static final String DEFAULT_NOMBRE = "GARAJE GOURMET";
    /** Días que puede usarse offline tras una validación "activo". */
    public static final int DIAS_CACHE_LICENCIA = 2;

    private static ComercioConfig instance;

    private String nombreComercio = DEFAULT_NOMBRE;
    private String logoPath = "";
    private String impresora = "";
    private String apiUrl = "";
    private String idCliente = "";
    private String accessKey = "";
    private long lastStatusOkAt = 0L;
    private String lastStatus = "";

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

    public String getApiUrl() {
        return apiUrl == null ? "" : apiUrl.trim();
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
    }

    public String getIdCliente() {
        return idCliente == null ? "" : idCliente.trim();
    }

    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente == null ? "" : idCliente.trim();
    }

    public String getAccessKey() {
        return accessKey == null ? "" : accessKey.trim();
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey == null ? "" : accessKey.trim();
    }

    public long getLastStatusOkAt() {
        return lastStatusOkAt;
    }

    public void setLastStatusOkAt(long lastStatusOkAt) {
        this.lastStatusOkAt = lastStatusOkAt;
    }

    public String getLastStatus() {
        return lastStatus == null ? "" : lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus == null ? "" : lastStatus.trim();
    }

    public boolean tieneCredencialesLicencia() {
        return !getApiUrl().isBlank() && !getIdCliente().isBlank() && !getAccessKey().isBlank();
    }

    /**
     * true si la última validación "activo" sigue vigente (dentro de DIAS_CACHE_LICENCIA).
     */
    public boolean licenciaCacheVigente() {
        if (lastStatusOkAt <= 0) {
            return false;
        }
        if (!"activo".equalsIgnoreCase(lastStatus)) {
            return false;
        }
        long maxAgeMs = DIAS_CACHE_LICENCIA * 24L * 60L * 60L * 1000L;
        return (System.currentTimeMillis() - lastStatusOkAt) <= maxAgeMs;
    }

    public long millisRestantesCache() {
        if (!licenciaCacheVigente()) {
            return 0L;
        }
        long maxAgeMs = DIAS_CACHE_LICENCIA * 24L * 60L * 60L * 1000L;
        return Math.max(0L, (lastStatusOkAt + maxAgeMs) - System.currentTimeMillis());
    }

    /**
     * Arma el endpoint de status a partir de la URL base o completa.
     */
    public String resolveStatusEndpoint() {
        String base = getApiUrl().replaceAll("/+$", "");
        if (base.isBlank()) {
            return "";
        }
        String marker = "/api/local-systems/status";
        if (base.toLowerCase().endsWith(marker)) {
            return base;
        }
        return base + marker;
    }

    public void marcarLicenciaActiva() {
        this.lastStatus = "activo";
        this.lastStatusOkAt = System.currentTimeMillis();
    }

    public void marcarLicenciaInactiva() {
        this.lastStatus = "inactivo";
        // No tocamos lastStatusOkAt: la caché "activo" deja de valer por lastStatus.
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

    public File importarLogo(File origen) throws IOException {
        if (origen == null || !origen.exists()) {
            throw new IOException("No se encontró el archivo de logo.");
        }
        File destino = getDefaultLogoFile();
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
            apiUrl = props.getProperty(KEY_API_URL, "");
            idCliente = props.getProperty(KEY_ID_CLIENTE, "");
            accessKey = props.getProperty(KEY_ACCESS_KEY, "");
            lastStatus = props.getProperty(KEY_LAST_STATUS, "");
            try {
                lastStatusOkAt = Long.parseLong(props.getProperty(KEY_LAST_OK_AT, "0"));
            } catch (NumberFormatException e) {
                lastStatusOkAt = 0L;
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer comercio.properties: " + e.getMessage());
        }
    }

    public void save() throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_NOMBRE, getNombreComercio());
        props.setProperty(KEY_LOGO, logoPath == null ? "" : logoPath);
        props.setProperty(KEY_IMPRESORA, impresora == null ? "" : impresora);
        props.setProperty(KEY_API_URL, getApiUrl());
        props.setProperty(KEY_ID_CLIENTE, getIdCliente());
        props.setProperty(KEY_ACCESS_KEY, getAccessKey());
        props.setProperty(KEY_LAST_OK_AT, String.valueOf(lastStatusOkAt));
        props.setProperty(KEY_LAST_STATUS, getLastStatus());
        File file = getPropertiesFile();
        try (OutputStream out = new FileOutputStream(file)) {
            props.store(out, "Configuracion del comercio - Sistema Pizzeria");
        }
    }
}
