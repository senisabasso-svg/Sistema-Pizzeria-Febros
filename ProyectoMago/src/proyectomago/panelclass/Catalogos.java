package proyectomago.panelclass;

/**
 * Listas compartidas de tipos / categorías / rubros del almacén.
 */
public final class Catalogos {

    public static final String[] TIPOS_PRODUCTO = {
        "Bebidas", "Almacén", "Lácteos", "Fiambres", "Snacks",
        "Limpieza", "Higiene", "Congelados", "Cigarrillos", "Otro"
    };

    public static final String[] CATEGORIAS_STOCK = {
        "Mercadería", "Bebida", "Fresco", "Limpieza", "Packaging", "Otro"
    };

    public static final String[] RUBROS_PROVEEDOR = {
        "Bebidas", "Almacén", "Lácteos", "Fiambres", "Snacks",
        "Limpieza", "Higiene", "Congelados", "Cigarrillos", "General"
    };

    private Catalogos() {
    }

    /** Mapea el tipo de producto a una categoría de stock razonable. */
    public static String categoriaDesdeTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "Mercadería";
        }
        switch (tipo.trim()) {
            case "Bebidas":
                return "Bebida";
            case "Lácteos":
            case "Fiambres":
            case "Congelados":
                return "Fresco";
            case "Limpieza":
            case "Higiene":
                return "Limpieza";
            case "Snacks":
            case "Almacén":
            case "Cigarrillos":
                return "Mercadería";
            default:
                return "Otro";
        }
    }
}
