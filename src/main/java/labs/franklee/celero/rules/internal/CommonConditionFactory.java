package labs.franklee.celero.rules.internal;

import labs.franklee.celero.logic.base.Priority;
import labs.franklee.celero.logic.base.ValueType;

import java.util.Map;

public abstract class CommonConditionFactory implements ConditionFactory {

    protected <T> T getValueAs(Map<String, Object> map, String key, Class<T> clazz) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }
        throw new IllegalArgumentException("Value is not of type: " + clazz.getName());
    }

    protected String field(Map<String, Object> p) {
        return getValueAs(p, "field", String.class);
    }

    protected String value(Map<String, Object> p) {
        return getValueAs(p, "value", String.class);
    }

    protected String expression(Map<String, Object> p) {
        return getValueAs(p, "expression", String.class);
    }

    protected ValueType valueType(Map<String, Object> p) {
        String vt = getValueAs(p, "valueType", String.class);
        return vt != null ? ValueType.fromString(vt) : null;
    }

    protected int priority(Map<String, Object> p) {
        Integer priority = getValueAs(p, "priority", Integer.class);
        return priority != null ? priority : Priority.DEFAULT;
    }

    protected boolean ignoreAbsence(Map<String, Object> p) {
        Boolean b = getValueAs(p, "ignoreAbsence", Boolean.class);
        return b != null ? b : false;
    }
}
