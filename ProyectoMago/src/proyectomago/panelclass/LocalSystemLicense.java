package proyectomago.panelclass;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valida el estado del sistema local contra la API remota.
 * Si responde "activo", se cachea {DIAS_CACHE_LICENCIA} días sin volver a pedir internet.
 */
public final class LocalSystemLicense {

    private static final Pattern STATUS_PATTERN = Pattern.compile(
            "\"status\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private LocalSystemLicense() {
    }

    public enum Outcome {
        ALLOWED_CACHE,
        ALLOWED_REMOTE,
        BLOCKED_INACTIVE,
        BLOCKED_NO_CONFIG,
        BLOCKED_NETWORK,
        BLOCKED_BAD_RESPONSE
    }

    public static final class Result {
        public final Outcome outcome;
        public final String message;
        public final String rawStatus;

        public Result(Outcome outcome, String message, String rawStatus) {
            this.outcome = outcome;
            this.message = message;
            this.rawStatus = rawStatus;
        }

        public boolean allowed() {
            return outcome == Outcome.ALLOWED_CACHE || outcome == Outcome.ALLOWED_REMOTE;
        }
    }

    /**
     * Chequeo de arranque: usa caché si está vigente; si no, consulta la API.
     */
    public static Result ensureAllowed() {
        ComercioConfig cfg = ComercioConfig.get();

        if (cfg.licenciaCacheVigente()) {
            long horas = cfg.millisRestantesCache() / (60L * 60L * 1000L);
            return new Result(Outcome.ALLOWED_CACHE,
                    "Licencia activa en caché (quedan ~" + Math.max(1, horas) + " h sin revalidar).",
                    "activo");
        }

        if (!cfg.tieneCredencialesLicencia()) {
            return new Result(Outcome.BLOCKED_NO_CONFIG,
                    "Falta configurar URL, idCliente y AccessKey en Configuración comercio.",
                    "");
        }

        return validateRemoteAndPersist(true);
    }

    /**
     * Fuerza consulta a la API (botón "Probar / Validar ahora").
     */
    public static Result validateNow() {
        ComercioConfig cfg = ComercioConfig.get();
        if (!cfg.tieneCredencialesLicencia()) {
            return new Result(Outcome.BLOCKED_NO_CONFIG,
                    "Completá URL, idCliente y AccessKey antes de validar.",
                    "");
        }
        return validateRemoteAndPersist(true);
    }

    private static Result validateRemoteAndPersist(boolean persist) {
        ComercioConfig cfg = ComercioConfig.get();
        try {
            String status = fetchStatus(cfg);
            if (status == null || status.isBlank()) {
                return new Result(Outcome.BLOCKED_BAD_RESPONSE,
                        "La API no devolvió un status reconocible.",
                        "");
            }
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            if ("activo".equals(normalized)) {
                cfg.marcarLicenciaActiva();
                if (persist) {
                    cfg.save();
                }
                return new Result(Outcome.ALLOWED_REMOTE,
                        "Sistema activo. Podés usarlo hasta "
                                + ComercioConfig.DIAS_CACHE_LICENCIA
                                + " días sin internet.",
                        "activo");
            }
            if ("inactivo".equals(normalized)) {
                cfg.marcarLicenciaInactiva();
                if (persist) {
                    cfg.save();
                }
                return new Result(Outcome.BLOCKED_INACTIVE,
                        "El sistema está inactivo. Contactá a soporte / administración.",
                        "inactivo");
            }
            return new Result(Outcome.BLOCKED_BAD_RESPONSE,
                    "Status desconocido recibido: " + status,
                    status);
        } catch (Exception e) {
            // Si había caché activa no llegaríamos acá; sin red y sin caché = bloqueo.
            return new Result(Outcome.BLOCKED_NETWORK,
                    "No se pudo validar (hace falta internet): " + e.getMessage(),
                    "");
        }
    }

    /**
     * POST JSON; si falla, intenta GET con query params.
     */
    public static String fetchStatus(ComercioConfig cfg) throws Exception {
        String endpoint = cfg.resolveStatusEndpoint();
        if (endpoint.isBlank()) {
            throw new IllegalStateException("URL de API vacía");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();

        String body = "{\"id\":\"" + jsonEscape(cfg.getIdCliente())
                + "\",\"key\":\"" + jsonEscape(cfg.getAccessKey()) + "\"}";

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = client.send(post, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseStatus(response.body());
            }
            System.out.println("POST status HTTP " + response.statusCode() + ": " + response.body());
        } catch (Exception postEx) {
            System.out.println("POST falló, intento GET: " + postEx.getMessage());
        }

        String q = endpoint
                + (endpoint.contains("?") ? "&" : "?")
                + "id=" + URLEncoder.encode(cfg.getIdCliente(), StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(cfg.getAccessKey(), StandardCharsets.UTF_8);

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(q))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(get, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (getResp.statusCode() < 200 || getResp.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + getResp.statusCode() + " — " + truncate(getResp.body(), 180));
        }
        return parseStatus(getResp.body());
    }

    private static String parseStatus(String json) {
        if (json == null) {
            return null;
        }
        Matcher m = STATUS_PATTERN.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        // fallback simple
        String lower = json.toLowerCase(Locale.ROOT);
        if (lower.contains("\"activo\"")) {
            return "activo";
        }
        if (lower.contains("\"inactivo\"")) {
            return "inactivo";
        }
        return null;
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }
}
