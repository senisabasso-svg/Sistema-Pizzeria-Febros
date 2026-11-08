package proyectomago.panelclass;

public class KeyValue {
    private Integer key;
    private String value;

    public KeyValue(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

    public KeyValue(String key, String value) {
        this.key = Integer.parseInt(key);
        this.value = value;
    }

    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
