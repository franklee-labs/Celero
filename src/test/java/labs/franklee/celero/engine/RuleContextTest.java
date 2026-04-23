package labs.franklee.celero.engine;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RuleContextTest {

    @Test
    void attribute_set_get() {
        RuleContext context = RuleContext.of(Collections.emptyMap());
        context.setAttribute("k1", "v1");
        assertEquals("v1", context.getAttribute("k1"));
        assertEquals(1, context.getAttributes().size());
    }
}
