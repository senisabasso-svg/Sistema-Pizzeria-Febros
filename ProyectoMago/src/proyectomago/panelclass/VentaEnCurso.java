package proyectomago.panelclass;

import java.util.ArrayList;
import java.util.List;

/** Una pestaña de venta (activa o pendiente) en memoria. */
public final class VentaEnCurso {

    private static int seq = 1;

    private final int id;
    private String titulo;
    private final List<Pedido> items = new ArrayList<>();

    public VentaEnCurso() {
        this.id = seq++;
        this.titulo = "Venta " + id;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        if (titulo != null && !titulo.isBlank()) {
            this.titulo = titulo.trim();
        }
    }

    public List<Pedido> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public double getTotal() {
        double t = 0;
        for (Pedido p : items) {
            t += p.getSubtotal();
        }
        return t;
    }

    public int getCantidadItems() {
        int n = 0;
        for (Pedido p : items) {
            n += p.getCantidad();
        }
        return n;
    }

    /** Etiqueta corta para la pestaña. */
    public String tabLabel() {
        if (items.isEmpty()) {
            return titulo;
        }
        String first = items.get(0).getNombreProd();
        if (first == null) {
            first = "…";
        }
        if (first.length() > 14) {
            first = first.substring(0, 13) + "…";
        }
        return first + " · $" + String.format("%.0f", getTotal());
    }

    public String resumenCorto() {
        return titulo + " · " + items.size() + " línea(s) · $" + String.format("%.0f", getTotal());
    }

    public static void resetSeqForTests() {
        seq = 1;
    }
}
