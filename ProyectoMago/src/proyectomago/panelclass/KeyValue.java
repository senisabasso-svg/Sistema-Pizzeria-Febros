package proyectomago.panelclass;

public class KeyValue {
    private Integer key;
    private String value;

    // Constructor que acepta clave y valor
    public KeyValue(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    // Métodos getter
    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    // Sobrescribir el método toString() para que devuelva solo el valor en el JComboBox
    @Override
    public String toString() {
        return value; // Lo que se mostrará en el JComboBox
    }

    // Métodos setter (aunque no se necesitan si usas el constructor)
    public void setKey(Integer key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
