package labs.franklee.celero.rules.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import labs.franklee.celero.logic.base.ValueType;

import java.util.List;
import java.util.Map;

public abstract class CommonConditionFactory implements ConditionFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    protected String key(Map<String, Object> p) {
        return getValueAs(p, "key", String.class);
    }

    protected String value(Map<String, Object> p) {
        return getValueAs(p, "value", String.class);
    }

    protected ValueType valueType(Map<String, Object> p) {
        String vt = getValueAs(p, "valueType", String.class);
        return vt != null ? ValueType.fromString(vt) : null;
    }

    protected int priority(Map<String, Object> p) {
        Integer priority = getValueAs(p, "priority", Integer.class);
        return priority != null ? priority : 0;
    }

    protected List<Object> parseList(Map<String, Object> p) {
        String json = value(p);
        try {
            return MAPPER.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("value must be a JSON array, got: " + json, e);
        }
    }
}
