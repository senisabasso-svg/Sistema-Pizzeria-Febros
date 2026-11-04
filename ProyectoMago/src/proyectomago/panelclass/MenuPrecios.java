/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectomago.panelclass;

/**
 *
 * @author senis
 */
public class MenuPrecios {
    
    public Integer id;
    public String nombre;
    public Integer precio;
    public Integer costo;
    public String tipo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public Integer getCosto() {
        return costo == null ? 0 : costo;
    }

    public void setCosto(Integer costo) {
        this.costo = costo;
    }

    public String getTipo() {
        return tipo == null || tipo.isBlank() ? "Otro" : tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getGanancia() {
        int p = precio == null ? 0 : precio;
        int c = costo == null ? 0 : costo;
        return p - c;
    }
}
