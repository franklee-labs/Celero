package labs.franklee.celero.logic.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ValueTypeTest {

    @Test
    void invalidType() {
        assertThrows(IllegalArgumentException.class, () -> ValueType.fromString("Integer"));
    }
}
