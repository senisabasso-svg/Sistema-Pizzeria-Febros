package proyectomago.panelclass;

public class Pedido {

    private String nombreProd;
    private String precio;
    private String nota;
    private int cantidad;

    public Pedido() {
        this.cantidad = 1;
        this.nota = "";
    }

    public Pedido(String nombreProd, String precio, String nota, int cantidad) {
        this.nombreProd = nombreProd;
        this.precio = precio;
        this.nota = nota == null ? "" : nota;
        this.cantidad = Math.max(1, cantidad);
    }

    public String getNombreProd() {
        return nombreProd;
    }

    public void setNombreProd(String nombreProd) {
        this.nombreProd = nombreProd;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota == null ? "" : nota;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = Math.max(1, cantidad);
    }

    public double getPrecioUnitario() {
        try {
            return Double.parseDouble(precio.replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    public double getSubtotal() {
        return getPrecioUnitario() * cantidad;
    }

    public String toTicketLine() {
        String line = cantidad + " x " + nombreProd + "  $" + String.format("%.0f", getSubtotal());
        if (nota != null && !nota.isBlank() && !"Sin Aderezos".equalsIgnoreCase(nota)) {
            line += "\n   (" + nota + ")";
        }
        return line;
    }

    @Override
    public String toString() {
        String base = cantidad + "× " + nombreProd + "  $" + String.format("%.0f", getSubtotal());
        if (nota != null && !nota.isBlank() && !"Sin Aderezos".equalsIgnoreCase(nota)) {
            base += " · " + nota;
        }
        return base;
    }
}
