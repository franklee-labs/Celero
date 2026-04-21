package labs.franklee.celero.context;

import labs.franklee.celero.engine.RuleContext;
import labs.franklee.celero.exceptions.InvalidParameterException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContextTest {

    @Test
    public void invalidKey_startWithDash_throwException() {
        assertThrows(InvalidParameterException.class, () -> Context.Builder.createBuilder(RuleContext.of(Map.of("_", "123"))).build());
    }

    @Test
    public void params_null_params() {
        assertDoesNotThrow(() -> Context.Builder.createBuilder(RuleContext.of(null)).build());
    }

    @Test
    public void params_empty_params() {
        assertDoesNotThrow(() -> Context.Builder.createBuilder(RuleContext.of(null)).build());
    }

    @Test
    public void invalidBuiltinParams_notStartWithDash_throwException() {
        assertThrows(InvalidParameterException.class, () -> Context.Builder.createBuilder(RuleContext.of(null)).build().buildEvalParams(Map.of("k", "v")));
    }

    @Test
    public void invalidBuiltinParams_nullKey_throwException() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, "v");
        assertThrows(InvalidParameterException.class, () -> Context.Builder.createBuilder(RuleContext.of(null)).build().buildEvalParams(map));
    }
}
