package labs.franklee.celero.logic.base;

public enum ValueType {
    String,
    Number,
    Boolean,
    List,
    Expression;

    public static ValueType fromString(String value) {
        for (ValueType valueType : values()) {
            if (valueType.name().equalsIgnoreCase(value)) {
                return valueType;
            }
        }
        throw new IllegalArgumentException("invalid value [" + value + "]");
    }


}
