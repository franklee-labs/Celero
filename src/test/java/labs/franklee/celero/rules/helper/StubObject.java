package labs.franklee.celero.rules.helper;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class StubObject {

    private String name;

    private final Map<String, Object> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public StubObject setName(String name) {
        this.name = name;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }

    @JsonAnySetter
    public StubObject setProperties(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

}
