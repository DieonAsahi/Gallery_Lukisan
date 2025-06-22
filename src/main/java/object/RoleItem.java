package object;

public class RoleItem {
    private final String label;
    private final String value;

    public RoleItem(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label; // Ini yang ditampilkan di JComboBox
    }
}
