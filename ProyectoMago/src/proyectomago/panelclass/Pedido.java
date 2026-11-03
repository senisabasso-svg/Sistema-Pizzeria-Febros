/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyectomago.panelclass;

/**
 *
 * @author senis
 */
public class Pedido {
    
    String NombreProd ; 
    String precio ;

    Pedido() {
//nose porque pero no puedo sacarlo
    }

    public String getNombreProd() {
        return NombreProd;
    }

    public void setNombreProd(String NombreProd) {
        this.NombreProd = NombreProd;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public Pedido(String precio) {
        this.precio = precio;
    }
    
    
    
}
